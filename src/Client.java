import java.io.*;
import java.net.Socket;

public class Client {
    public static final int MAIN_SERVER_PORT = 5555;

    private String hostname = "localhost";

    private Socket connection;

    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    private BufferedReader userInput;

    private String username;
    private String password;
    private Chat chat;

    private int responsiveServer;

    public Client() {
        username = "";
        password = "";
        chat = null;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        responsiveServer = 0;
    }

    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        chat = null;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        responsiveServer = 0;
    }

    private void startConnection() {
        try {
            // connection to responsive server
            connection = new Socket("localhost", responsiveServer);
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

    public void start() {
        // send request to main server for responsive server (LOADBALANCING)
        responsiveServer = getResponsiveServer();
        if (responsiveServer == 0) {
            return; // FAILURE -- MAIN SERVER DID NOT RESPOND PROPERLY
        }
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
    }

    private void login() {
        String username = "";
        String password = "";
        Login myLogin = new Login(username, password);
        try {
            startConnection();
            username = usernameInput();
            myLogin.setUsername(username);

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
                this.username = username;
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


    private int getResponsiveServer() {
        try {
            Socket connection = new Socket("localhost", MAIN_SERVER_PORT);
            BufferedReader networkIn = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            PrintWriter networkOut = new PrintWriter(connection.getOutputStream());
            networkOut.println("SERVERREQUEST");
            networkOut.flush();
            String serverAnswer = networkIn.readLine();
            return Integer.parseInt(serverAnswer);
        } catch (IOException e) {
            System.err.println(e);
        }

        return 0;
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
}
