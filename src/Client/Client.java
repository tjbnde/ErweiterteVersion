package Client;

import Client.Worker.MessageReaderWorker;
import Model.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Properties;

public class Client {
    // adresses of server
    private String[] serverHostname;

    // port of server
    private int communicationPort;

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
        init();
    }

    public Client(String username, String password) {
        this.username = username;
        this.password = password;
        init();
    }

    private void init(){
        chat = null;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        responsiveServerPort = 0;
        responsiveServerHostname = "";
        globalLamportCounter = 0;
        Properties properties = new Properties();
        try {
            FileInputStream propertiesInputStream = new FileInputStream("config.properties") ;
            properties.load(propertiesInputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
        serverHostname = new String[]{properties.getProperty("hostname1"), properties.getProperty("hostname2")};
        communicationPort = Integer.parseInt(properties.getProperty("communicationPort"));
    }

    /**
     * Startet das Client-Programm
     */
    public void start() {
        enterSystem();
        joinChat();
        chatLoop();
    }

    /**
     * Führt abhängig vom User verschiedene Methoden aus
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
     * User gibt username vom Chat Partner ein und sendet dies an den Server
     * Server antwortet ob dem Chat erfolgreich betreten wurde
     * Im Fehlerfall wird die Methode erneut aufgerufen
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
            closeConnection();
        }
    }

    /**
     * Endlos Schleife in der Nachrichten geschrieben werden können, welche zum Server geschickt werden
     * Startet einen MessageReaderWorker der permanent Nachrichten liest
     * @see MessageReaderWorker
     */
    private void chatLoop() {
        startConnection();
        MessageReaderWorker readerWorker = new MessageReaderWorker(serverIn);
        Thread t = new Thread(readerWorker);
        t.start();
        while (isLoggedIn()) {
            try {
                String messageText = null;
                while(messageText == null || messageText.isEmpty()) {
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
     * @return eingegebner Befehl
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
     * User gibt Benutzername und Passwort ein und sendet dies an den Server
     * Server antwortet ob der Login erfolgreich war
     * Im Fehlerfall wird die Methode erneut aufgerufen
     * @see #usernameInput()
     */
    private void login() {
        startConnection();

        String username = usernameInput();
        String password = "";

        if (username.equals("exit")) {
            System.out.println("** exit successfully");
            System.exit(0);
        }

        System.out.println("> enter your password");

        try {
            password = userInput.readLine();
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
     * User legt einen Benutzernamen und ein Passwort fest und sendet dies an den Server
     * Server antwortet ob die Registrierung erfolgreich war
     * Im Fehlerfall wird die Methode erneut aufgerufen
     * @see #usernameInput()
     * @see #passwordInput()
     */
    private void register() {
        startConnection();
        username = usernameInput();
        password = passwordInput();
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
                // call function again if it fails
                register();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }
    }


    /**
     * Gibt eine Liste aller möglichen Befehle auf der Konsole aus
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
     * Startet eine Verbindung mit einem zufälligen Server und startet die zugehörigen Streams
     */
    private void startConnection() {
        // contact a random server
        responsiveServerHostname = returnRandomServerHostname();

        try {
            // connection to responsive server
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
     * Schließt die bestehnde Verbindung, wenn sie vorhanden ist
     */
    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    /**
     * User kann entscheiden ob er mit dem Logout fortfahren will
     */
    private void logoutDialog() {
        System.out.println("** going to logout - proceed? (y/n)");
        try {
            String userAnswer = userInput.readLine();
            while(!userAnswer.equals("y") | !userAnswer.equals("n")) {
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
     * sämtliche Attribute werden zurückgesetzt
     */
    private void logout() {
        username = "";
        password = "";
        chat = null;
        globalLamportCounter = 0;
        responsiveServerHostname = "";
        responsiveServerPort = 0;
        closeConnection();
    }


    /**
     *
     * @return eingebener Benutername
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
     * @param userCommand zu prüfender Befehl
     * @return Wahrheitswert, ob der Befehl existiert
     */
    private boolean userCommandIsValid(String userCommand) {
        userCommand = userCommand.toLowerCase();
        return userCommand.equals("exit") || userCommand.equals("login") || userCommand.equals("register") || userCommand.equals("info");
    }

    private boolean isLoggedIn() {
        return !username.isEmpty() && !password.isEmpty();
    }


    /**
     * User gibt solange ein Passwort wiederholt ein, bis es übereinstimmt
     * @return erstelltes Passwort
     */
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

    /**
     * Entscheidet per Zufall welcher Server angesprochen wird
     * @return IP-Adressse des verarbeitenden Servers
     */
    private String returnRandomServerHostname() {
        double random = Math.random();
        if (random < 0.5) {
            return serverHostname[0];
        } else {
            return serverHostname[1];
        }
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
}
