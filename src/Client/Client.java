package Client;

import Model.Chat;
import Model.Login;
import Model.Message;
import Model.Register;

import java.io.*;
import java.net.Socket;
import java.util.Iterator;

public class Client {
    // adresses of server
    private String[] serverHostname;
    private int[] serverPort;

    // connection to server and streams
    private Socket connection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    // stream for user input
    private BufferedReader userInput;

    // unique username
    private String username;
    private String password;

    // chat object to get chat partner
    private Chat chat;

    // server port and hostname which processes request
    private int responsiveServerPort;
    private String responsiveServerHostname;

    // counter for lamport
    private int globalLamportCounter;


    public Client() {
        username = "";
        password = "";
        chat = null;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        responsiveServerPort = 0;
        responsiveServerHostname = "";
        serverPort = new int[]{6666, 8888};
        serverHostname = new String[]{"localhost", "localhost"};
        globalLamportCounter = 0;
    }

    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        chat = null;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        responsiveServerPort = 0;
        responsiveServerHostname = "";
        serverPort = new int[]{6666, 8888};
        serverHostname = new String[]{"localhost", "localhost"};
        globalLamportCounter = 0;
    }

    private void enterSystem() {
        String userCommand = getUserCommand();
        switch (userCommand) {
            case "login":
                login();
                System.out.println("** login successful");
                break;
            case "register":
                register();
                System.out.println("** register successful");
                break;
            case "exit":
                System.out.println("** exit successful");
                System.exit(0);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void start() {
        enterSystem();
        joinChat();
        chatLoop();
    }


    private void chatLoop() {
        startConnection();
        while (isLoggedIn()) {
            try {
                String messageText = userInput.readLine();
                if (messageText.equals("logout")) {
                    logoutDialog();
                }
                Message myMessage = new Message(username, chat.getUserB(), globalLamportCounter, messageText);
                serverOut.writeObject(myMessage);
                serverOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }

        }
    }

    private void logoutDialog() {
        System.out.println("** going to logout - proceed? (y/n)");
        try {
            String userAnswer = userInput.readLine();
            if (userAnswer.equals("y")) {
                logout();
            } else if (!userAnswer.equals("n")) {
                logoutDialog();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void logout() {
        username = "";
        password = "";
        chat = null;
        globalLamportCounter = 0;
        responsiveServerHostname = "";
        responsiveServerPort = 0;
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void startConnection() {
        int random = returnRandom();
        responsiveServerPort = serverPort[random];
        responsiveServerHostname = serverHostname[random];
        try {
            // connection to responsive server
            connection = new Socket(responsiveServerHostname, responsiveServerPort);
            OutputStream outputStream = connection.getOutputStream();
            serverOut = new ObjectOutputStream(outputStream);
            InputStream inputStream = connection.getInputStream();
            serverIn = new ObjectInputStream(inputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private String usernameInput() {
        String username = "";
        System.out.println("> enter your username");
        try {
            username = userInput.readLine();
        } catch (IOException e) {
            System.err.println(e);
        }
        return username;
    }

    private boolean userCommandIsValid(String userCommand) {
        userCommand = userCommand.toLowerCase();
        return userCommand.equals("exit") || userCommand.equals("login") || userCommand.equals("register");
    }

    private String getUserCommand() {
        System.out.println("enter command [\"login\"] [\"register\"] [\"exit\"]");
        String userCommand = "DEFAULT";
        while (userCommand.equals("DEFAULT")) {
            try {
                userCommand = userInput.readLine();
                if (userCommandIsValid(userCommand)) {
                    return userCommand;
                } else {
                    userCommand = "DEFAULT";
                }
            } catch (IOException e) {
                System.err.println(e);
            }
            System.out.println("enter valid command [\"login\"] [\"register\"] [\"exit\"]");
        }
        return "";
    }

    private void joinChat() {
        startConnection();
        System.out.println("** enter username of chat partner");
        try {
            String chatPartner = userInput.readLine();
            chat = new Chat(username, chatPartner);
            serverOut.writeObject(chat);
            chat = (Chat) serverIn.readObject();
            if (!chat.getErrorMessage().isEmpty()) {
                System.err.println(chat.getErrorMessage());
                joinChat();
            } else {
                System.out.println("** chat successfully joined");
                Iterator<Message> i = chat.getMessages().iterator();
                while (i.hasNext()) {
                    Message myMessage = i.next();
                    System.out.println(myMessage.getHeader().getSendFrom() + ": " + myMessage.getText());

                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }


    private boolean isLoggedIn() {
        return !username.isEmpty() && !password.isEmpty();
    }

    private void login() {
        startConnection();
        String username = "";
        String password = "";
        Login myLogin = new Login(username, password);
        username = usernameInput();
        myLogin.setUsername(username);
        try {
            System.out.println("> enter your password");
            password = userInput.readLine();
            myLogin.setPassword(password);
            serverOut.writeObject(myLogin);
            serverOut.flush();

            myLogin = (Login) serverIn.readObject();

            if (myLogin.isSuccessful()) {
                this.username = username;
                this.password = password;
                return;
            } else {
                System.err.println(myLogin.getErrorMessage());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
        login();
    }


    private Register createNewUsername() {
        String username = "";
        Register myRegister = new Register(username);

        startConnection();
        username = usernameInput();
        myRegister.setUsername(username);

        // send register request to server and see if username is available
        try {
            serverOut.writeObject(myRegister);
            serverOut.flush();
            myRegister = (Register) serverIn.readObject();
            if (myRegister.isUsernameAvailable()) {
                return myRegister;
            } else {
                System.err.println(myRegister.getErrorMessage());
                register();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        return myRegister;
    }

    private void register() {
        String password = "";
        Register myRegister = createNewUsername();
        try {
            startConnection();
            password = passwordInput();
            myRegister.setPassword(password);
            serverOut.writeObject(myRegister);
            serverOut.flush();

            myRegister = (Register) serverIn.readObject();

            if (myRegister.isSuccessful()) {
                this.username = myRegister.getUsername();
                this.password = password;
            } else {
                System.err.println(myRegister.getErrorMessage());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }

    private String passwordInput() {
        String password = "";
        String passwordRepeat = "";
        boolean passwordMatch = false;
        while (!passwordMatch) {
            System.out.println("> enter your password");
            try {
                password = userInput.readLine();
            } catch (IOException e) {
                System.err.println(e);
            }
            System.out.println("> repeat your password");
            try {
                passwordRepeat = userInput.readLine();
            } catch (IOException e) {
                System.err.println(e);
            }
            if (password.equals(passwordRepeat)) {
                passwordMatch = true;
            } else {
                System.err.println("** passwords do not match");
            }
        }
        return password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    private int returnRandom() {
        double random =  Math.random();
        if (random < 0.5) {
            return 0;
        } else {
            return 1;
        }
    }
}
