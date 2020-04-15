package Client;

import Client.Worker.MessageReaderWorker;
import Model.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Properties;

public class Client {
    /**
     * String Array with IP addresses of server
     */
    private String[] serverHostname;

    /**
     * Port of the server
     */
    private int communicationPort;

    /**
     * Connection to server
     */
    private Socket connection;

    /**
     * Input stream from server
     */
    private ObjectInputStream serverIn;

    /**
     * Output stream to server
     */
    private ObjectOutputStream serverOut;

    /**
     * Buffered Reader for user input
     */
    private BufferedReader userInput;

    /**
     * Username of Client
     */
    private String username;

    /**
     * Password of Client
     */
    private String password;

    /**
     * Chat object to get chat partner
     */
    private Chat chat;

    /**
     * IP adress of responsive server
     */
    private String responsiveServerHostname;

    /**
     * Lamport counter for logical order
     */
    private int globalLamportCounter;

    /**
     * Creates a Client that is not logged in
     */
    public Client() {
        username = "";
        password = "";
        init();
    }

    /**
     * Creates a Client that is logged in
     *
     * @param username - Username of client
     * @param password - Password of client
     * @see #init()
     */
    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        init();
    }

    /**
     * Initialises client attributes
     */
    private void init() {
        chat = null;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        responsiveServerHostname = "";
        globalLamportCounter = 0;

        Properties properties = new Properties();
        try {
            FileInputStream propertiesInputStream = new FileInputStream("config.properties");
            properties.load(propertiesInputStream);
        } catch (IOException e) {
            System.err.println(e);
        }

        serverHostname = new String[]{properties.getProperty("hostname1"), properties.getProperty("hostname2")};
        communicationPort = Integer.parseInt(properties.getProperty("communicationPort"));
    }

    /**
     * Start of client programm
     *
     * @see #enterSystem()
     * @see #joinChat()
     * @see #chatLoop()
     */
    public void start() {
        enterSystem();
        joinChat();
        chatLoop();
    }

    /**
     * Calls other methods depending on user input
     *
     * @see #getUserCommand()
     * @see #login()
     * @see #register()
     * @see #printInfo()
     */
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
            case "info":
                printInfo();
                enterSystem();
                break;
            case "exit":
                System.out.println("** exit successful");
                System.exit(0);
        }
    }

    /**
     * User enters name of the chat partner, which gets send to the server. The server replies whether the chat could be joined successful or not. If not, the method calls itself.
     */
    private void joinChat() {
        startConnection();

        System.out.println("** enter username of chat partner");

        try {
            String chatPartner = userInput.readLine();

            chat = new Chat(username, chatPartner);
            serverOut.writeObject(chat);

            chat = (Chat) serverIn.readObject();

            if (chat.isSuccessful()) {
                System.out.println("** chat successfully joined");
                Iterator<Message> i = chat.getMessages().iterator();
                while (i.hasNext()) {
                    Message myMessage = i.next();
                    System.out.println(myMessage.getHeader().getSendFrom() + ": ");
                    System.out.println(myMessage.getText());
                }
            } else {
                System.err.println(chat.getErrorMessage());
                joinChat();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            if(!chat.isSuccessful()) {
                closeConnection();
            }
        }
    }

    /**
     * Endless loop, where the user can write messages, which get send to the server
     * Starts a MessageReaderWorker that reads incoming messages
     *
     * @see MessageReaderWorker
     */
    private void chatLoop() {
        MessageReaderWorker readerWorker = new MessageReaderWorker(serverIn);
        Thread t = new Thread(readerWorker);
        t.start();
        while (isLoggedIn()) {
            try {
                String messageText = null;
                while (messageText == null || messageText.isEmpty()) {
                    messageText = userInput.readLine();
                }

                System.out.println(username + ": ");
                System.out.println(messageText);
                System.out.println();
                if (messageText.equals("logout")) {
                    logoutDialog();
                }
                Message myMessage = new Message(username, chat.getUserB(), globalLamportCounter, messageText);
                globalLamportCounter++;
                serverOut.writeObject(myMessage);
                serverOut.flush();

            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * User can enter a command as long till its valid
     *
     * @return entered Command
     */
    private String getUserCommand() {
        System.out.println("enter command [”info” for a list of all commands]");
        String userCommand = "";

        try {
            userCommand = userInput.readLine();
            while (!userCommandIsValid(userCommand)) {
                System.err.println("enter valid command [”info” for a list of all commands]");
                userCommand = userInput.readLine();
            }
        } catch (IOException e) {
            System.err.println(e);
        }

        return userCommand;
    }

    /**
     * User enters username and password, which gets send to the server. The server replies whether the login was successful or not. If not, the method calls itself.
     *
     * @see #usernameInput()
     */
    private void login() {
        startConnection();

        String username = usernameInput();

        if (username.equals("exit")) {
            System.out.println("** exit successfully");
            System.exit(0);
        }

        System.out.println("> enter your password");

        try {
            String password = userInput.readLine();
            Login myLogin = new Login(username, password);

            serverOut.writeObject(myLogin);
            serverOut.flush();

            myLogin = (Login) serverIn.readObject();

            if (myLogin.isSuccessful()) {
                this.username = username;
                this.password = password;
            } else {
                System.err.println(myLogin.getErrorMessage());
                login();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }
    }

    /**
     * User creates a new username and password, which gets send to the server. The server responds whether the registration was successful or not. If not, the method calls itself.
     *
     * @see #usernameInput()
     * @see #createPassword()
     */
    private void register() {
        startConnection();

        username = usernameInput();
        password = createPassword();

        Register myRegister = new Register(username, password);

        try {
            serverOut.writeObject(myRegister);
            serverOut.flush();

            myRegister = (Register) serverIn.readObject();

            if (myRegister.isSuccessful()) {
                this.username = myRegister.getUsername();
                this.password = myRegister.getPassword();
            } else {
                System.err.println(myRegister.getErrorMessage());
                register();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }
    }


    /**
     * Prints a list of all commands to the console
     */
    private void printInfo() {
        System.out.println("** printing a list of all commands");
        System.out.println("** enter [command]-help to get infos for a specific command");
        System.out.println("login");
        System.out.println("register");
        System.out.println("autoregister");
        System.out.println("exit");
        System.out.println();
    }

    /**
     * Starts a connection to a random server and starts the input and output stream
     */
    private void startConnection() {
        responsiveServerHostname = returnRandomServerHostname();

        try {
            connection = new Socket(InetAddress.getByName(responsiveServerHostname), communicationPort);
            OutputStream outputStream = connection.getOutputStream();
            serverOut = new ObjectOutputStream(outputStream);
            InputStream inputStream = connection.getInputStream();
            serverIn = new ObjectInputStream(inputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    /**
     * Closes the connection to the server
     */
    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }


    /**
     * User can decide whether he wants to proceed logout or not.
     *
     * @see #logout()
     */
    private void logoutDialog() {
        System.out.println("** going to logout - proceed? (y/n)");

        try {
            String userAnswer = userInput.readLine();
            while (!userAnswer.equals("y") | !userAnswer.equals("n")) {
                userAnswer = userInput.readLine();
            }

            if (userAnswer.equals("y")) {
                logout();
            } else {
                System.out.println("** logout quited");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Resets all attributes and closes the connection
     *
     * @see #closeConnection()
     */
    private void logout() {
        username = "";
        password = "";
        chat = null;
        globalLamportCounter = 0;
        responsiveServerHostname = "";

        closeConnection();
    }


    /**
     * User can enter a username
     *
     * @return entered username
     */
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

    /**
     * Checks if the user command is valid
     *
     * @param userCommand - Command which gets checked
     * @return true if the user command exists
     */
    private boolean userCommandIsValid(String userCommand) {
        userCommand = userCommand.toLowerCase();

        return userCommand.equals("exit") || userCommand.equals("login") || userCommand.equals("register") || userCommand.equals("info");
    }

    /**
     * checks if the user is logged in
     * @return true if the user is logged in
     */
    private boolean isLoggedIn() {
        return !username.isEmpty() && !password.isEmpty();
    }

    /**
     * User enters two passwords as long as they match
     *
     * @return created password
     */
    private String createPassword() {
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

    /**
     * Decides randomly processing server
     *
     * @return IP address of processing server
     */
    private String returnRandomServerHostname() {
        double random = Math.random();

       // if (random < 0.5) {
        //    return serverHostname[0];
       // } else {
            return serverHostname[1];
      //  }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
