package Client;

import Client.Worker.MessageReaderWorker;
import Model.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
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
        globalLamportCounter = 0;
        init();
    }

    /**
     * Creates a Client that is logged in
     *
     * @param username - Username of client
     * @param password - Password of client
     * @see #init()
     */
    public Client(String username, String password, int globalLamportCounter) {
        this.username = username;
        this.password = password;
        this.globalLamportCounter = globalLamportCounter;
        init();
    }

    /**
     * Initialises client attributes
     */
    private void init() {
        chat = null;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        responsiveServerHostname = "";

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

        String chatPartner = "";
        Chat myChat;

        System.out.println("** enter username of chat partner");

        try {
            chatPartner = userInput.readLine();
        } catch (IOException e) {
            System.err.println(e);
        }

        myChat = new Chat(username, chatPartner);

        try {
            serverOut.writeObject(myChat);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            System.err.println("** trying to reconnect");
            joinChat(myChat);
            return;
        }

        try {
            myChat = (Chat) serverIn.readObject();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            System.err.println("** trying to reconnect");
            joinChat(myChat);
            return;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        }

        if (!myChat.isSuccessful()) {
            closeConnection();
            System.err.println(myChat.getErrorMessage());
            joinChat();
        } else {
            System.out.println("** chat successfully joined");
            chat = myChat;
            for (Message myMessage : chat.getMessages()) {
                System.out.println(myMessage.getHeader().getSendFrom() + ": ");
                System.out.println(myMessage.getText());
            }
        }
    }

    /**
     * If the connection to the server fails this method gets called and tries to send it to the server until it has been received.
     *
     * @param myChat - Chat that gets send
     */
    private void joinChat(Chat myChat) {
        startConnection();
        System.out.println("** connection to server established successful");

        try {
            serverOut.writeObject(myChat);
            serverOut.flush();
        } catch (IOException e) {
            joinChat(myChat);
            return;
        }

        try {
            myChat = (Chat) serverIn.readObject();
        } catch (IOException e) {
            joinChat(myChat);
            return;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            if (!myChat.isSuccessful()) {
                closeConnection();
            }
        }

        if (myChat.isSuccessful()) {
            System.out.println("** chat successfully joined");
            chat = myChat;
            for (Message myMessage : chat.getMessages()) {
                System.out.println(myMessage.getHeader().getSendFrom() + ": ");
                System.out.println(myMessage.getText());
            }
        } else {
            System.err.println(myChat.getErrorMessage());
            joinChat();
        }
    }


    /**
     * Endless loop, where the user can write messages, which get send to the server
     * Starts a MessageReaderWorker that reads incoming messages
     *
     * @see MessageReaderWorker
     */
    private void chatLoop() {
        MessageReaderWorker readerWorker = new MessageReaderWorker(serverIn, this);
        Thread t = new Thread(readerWorker);
        t.start();

        while (isLoggedIn()) {
            String messageText = null;
            while (messageText == null || messageText.isEmpty()) {
                try {
                    messageText = userInput.readLine();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }

            if (messageText.equals("logout")) {
                logoutDialog();
            }


            Message myMessage = new Message(username, chat.getUserB(), globalLamportCounter, messageText);
            globalLamportCounter++;

            try {
                serverOut.writeObject(myMessage);
                serverOut.flush();
                System.out.println(username + ": ");
                System.out.println(messageText);
                System.out.println();
            } catch (IOException e) {
                System.err.println("** lost connection to server");
                System.err.println("** trying to reconnect");
                t.interrupt();
                sendMessage(myMessage);
                return;
            }
        }
    }

    /**
     * Sends message to server if connection has been lost
     *
     * @param myMessage - Message that gets send
     */
    private void sendMessage(Message myMessage) {
        startConnection();
        try {
            serverOut.writeObject(myMessage);
            serverOut.flush();
            System.out.println(username + ": ");
            System.out.println(myMessage.getText());
            System.out.println();
            chatLoop();
        } catch (IOException e) {
            sendMessage(myMessage);
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

        String username;
        String password = "";

        username = usernameInput();

        if (username.equals("exit")) {
            System.out.println("** exit successful");
            System.exit(0);
        }

        System.out.println("> enter your password");

        try {
            password = userInput.readLine();
        } catch (IOException e) {
            System.err.println(e);
        }

        Login myLogin = new Login(username, password);

        try {
            serverOut.writeObject(myLogin);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            System.err.println("** trying to reconnect");
            login(myLogin);
            return;
        }

        try {
            myLogin = (Login) serverIn.readObject();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            System.err.println("** trying to reconnect");
            login(myLogin);
            return;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }

        if (myLogin.isSuccessful()) {
            this.username = username;
            this.password = password;
            this.globalLamportCounter = myLogin.getLocalLamportCounter();
        } else {
            System.err.println(myLogin.getErrorMessage());
            login();
        }
    }

    /**
     * If the connection to the server fails this method gets called and tries to send it to the server until it has been received.
     *
     * @param myLogin - Login that gets send
     */
    private void login(Login myLogin) {
        startConnection();
        System.out.println("** connection to server established successful");

        try {
            serverOut.writeObject(myLogin);
            serverOut.flush();
        } catch (IOException e) {
            login(myLogin);
            return;
        }

        try {
            myLogin = (Login) serverIn.readObject();
        } catch (IOException e) {
            login(myLogin);
            return;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }

        if (myLogin.isSuccessful()) {
            this.username = myLogin.getUsername();
            this.password = myLogin.getPassword();
        } else {
            System.err.println(myLogin.getErrorMessage());
            login();
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
        String username;
        String password;

        username = usernameInput();
        password = createPassword();

        Register myRegister = new Register(username, password);

        try {
            serverOut.writeObject(myRegister);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            System.err.println("** trying to reconnect");
            register(myRegister);
            return;
        }

        try {
            myRegister = (Register) serverIn.readObject();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            System.err.println("** trying to reconnect");
            register(myRegister);
            return;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }

        if (myRegister.isSuccessful()) {
            this.username = username;
            this.password = password;
        } else {
            System.err.println(myRegister.getErrorMessage());
            register();
        }
    }

    /**
     * If the connection to the server fails this method gets called and tries to send it to the server until it has been received.
     *
     * @param myRegister - Register that gets send
     */
    private void register(Register myRegister) {
        startConnection();
        System.out.println("** connection to server established successful");

        try {
            serverOut.writeObject(myRegister);
            serverOut.flush();
        } catch (IOException e) {
            register(myRegister);
            return;
        }

        try {
            myRegister = (Register) serverIn.readObject();
        } catch (IOException e) {
            register(myRegister);
            return;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }

        if (myRegister.isSuccessful()) {
            this.username = myRegister.getUsername();
            this.password = myRegister.getPassword();
        } else {
            System.err.println(myRegister.getErrorMessage());
            register();
        }
    }


    /**
     * Prints a list of all commands to the console
     */
    private void printInfo() {
        System.out.println("** printing a list of all commands");
        System.out.println("login");
        System.out.println("register");
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

        } catch (IOException e) {
            System.err.println("** connection to server failed");
            System.err.println("** trying to reconnect");
        }

        if (connection != null) {
            try {
                OutputStream outputStream = connection.getOutputStream();
                serverOut = new ObjectOutputStream(outputStream);
                InputStream inputStream = connection.getInputStream();
                serverIn = new ObjectInputStream(inputStream);
            } catch (IOException e) {
                System.err.println("** lost connection to server");
                System.err.println("** trying to reconnect");
                restartConnection();
            }
        } else {
            restartConnection();
        }
    }

    private void restartConnection() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
        startConnection();
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
     *
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

        /*if (random < 0.5) {
            return serverHostname[0];
        } else {
       */
        return serverHostname[1];
        // }
    }

    public void setGlobalLamportCounter(int globalLamportCounter) {
        if (globalLamportCounter > this.globalLamportCounter) {
            this.globalLamportCounter = globalLamportCounter + 1;
        } else {
            this.globalLamportCounter++;
        }
    }

    public Chat getChat() {
        return chat;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getGlobalLamportCounter() {
        return globalLamportCounter;
    }
}
