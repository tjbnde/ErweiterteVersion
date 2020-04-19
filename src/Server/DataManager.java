package Server;

import Client.Client;
import Model.Chat;
import Model.Login;
import Model.Message;
import Model.Register;

import java.io.*;
import java.util.*;

public class DataManager {
    private File userFile;
    private File chatListFile;
    private File chatFile;
    private File logFile;

    private Properties properties;

    private HashMap<String, Client> registeredUsers;
    private HashMap<String, Chat> existingChatList;
    private HashMap<String, ObjectOutputStream> loggedUsers;

    public DataManager(String userFileName, String chatListFileName, String chatFileName, String logFileName) {
        userFile = new File(userFileName);
        chatListFile = new File(chatListFileName);
        chatFile = new File(chatFileName);
        logFile = new File(logFileName);

        properties = new Properties();

        try {
            FileInputStream propertiesInputStream = new FileInputStream("config.properties");
            properties.load(propertiesInputStream);
        } catch (IOException e) {
            System.err.println(e);
        }

        registeredUsers = new HashMap<>();
        existingChatList = new HashMap<>();
        loggedUsers = new HashMap<>();
        readRegisteredUsers();
        readExistingChatList();
        readChats();
    }

    public Properties getProperties() {
        return properties;
    }

    public void addUser(Register newRegister) {
        try {
            FileWriter writer = new FileWriter(userFile, true);
            writer.write(newRegister.getUsername() + ";" + newRegister.getPassword() + ";" + "0\n");
            Client newClient = new Client(newRegister.getUsername(), newRegister.getPassword(), 0);
            registeredUsers.put(newClient.getUsername(), newClient);
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public ArrayList<Message> returnChatMessages(Chat myChat) {
        String userA = myChat.getUserA();
        String userB = myChat.getUserB();
        if (existingChatList.containsKey(userA + userB)) {
            return existingChatList.get(userA + userB).getMessages();
        } else {
            return existingChatList.get(userB + userA).getMessages();
        }

    }

    private void readRegisteredUsers() {
        try {
            Scanner userFileReader = new Scanner(userFile);
            while (userFileReader.hasNextLine()) {
                String data = userFileReader.nextLine();
                String[] userData = data.split(";");
                String username = userData[0];
                String password = userData[1];
                int lamportCounter = Integer.parseInt(userData[2]);
                Client myClient = new Client(username, password, lamportCounter);
                registeredUsers.put(myClient.getUsername(), myClient);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }

    private void readExistingChatList() {
        try {
            Scanner chatListFileReader = new Scanner(chatListFile);
            while (chatListFileReader.hasNextLine()) {
                String data = chatListFileReader.nextLine();
                String[] chatListData = data.split(";");
                String chatID = chatListData[0];
                String userA = chatListData[1];
                String userB = chatListData[2];
                Chat myChat = new Chat(chatID, userA, userB);
                existingChatList.put(myChat.getChatId(), myChat);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }


    private void readChats() {
        try {
            Scanner chatFileReader = new Scanner(chatFile);
            while (chatFileReader.hasNextLine()) {
                String data = chatFileReader.nextLine();
                String[] chatData = data.split(";");
                String chatID = chatData[0];
                ArrayList<Message> messages;
                if (existingChatList.containsKey(chatID)) {
                    messages = existingChatList.get(chatID).getMessages();
                } else {
                    messages = new ArrayList<>();
                }
                for (int i = 1; i < chatData.length; i++) {
                    String[] messageData = chatData[i].split("#%#");
                    String messageID = messageData[0];
                    String sendFrom = messageData[1];
                    String sendTo = messageData[2];
                    String lamportCounter = messageData[3];
                    String sendSuccessfull = messageData[4];
                    String timeSend = messageData[5];
                    String text = messageData[6];

                    Message myMessage = new Message(messageID, sendFrom, sendTo, lamportCounter, sendSuccessfull, timeSend, text);
                    messages.add(myMessage);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }

    private boolean validateUser(String username, String password) {
        if (registeredUsers.containsKey(username)) {
            Client user = registeredUsers.get(username);
            if (user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public boolean userIsRegistered(String username) {
        return registeredUsers.containsKey(username);
    }


    public boolean chatExists(Chat myChat) {
        String userA = myChat.getUserA();
        String userB = myChat.getUserB();
        return existingChatList.containsKey(userA + userB) || existingChatList.containsKey(userB + userA);
    }

    public void writeMessage(Message myMessage) {
        try {
            Scanner chatFileReader = new Scanner(chatFile);
            ArrayList<String> lines = new ArrayList<>();

            String userA = myMessage.getHeader().getSendFrom();
            String userB = myMessage.getHeader().getSendTo();
            while (chatFileReader.hasNextLine()) {
                String data = chatFileReader.nextLine();
                String[] chatData = data.split(";");
                String chatID = chatData[0];
                if (chatID.equals(userA + userB) || chatID.equals(userB + userA)) {
                    data += ";" + myMessage.toString();
                }
                data += "\n";
                lines.add(data);
            }
            FileWriter writer = new FileWriter(chatFile, false);
            Iterator<String> i = lines.iterator();
            while (i.hasNext()) {
                writer.write(i.next());
            }
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }
        readChats();
    }

    public boolean messageCanBeCommited(Message myMessage) {
        // TODO
        return true;
    }

    public boolean loginCanBeCommited(Login myLogin) {
        return validateUser(myLogin.getUsername(), myLogin.getPassword());
    }

    public boolean registerCanBeCommited(Register myRegister) {
        return !registeredUsers.containsKey(myRegister.getUsername());
    }

    public boolean chatCanBeCommited(Chat myChat) {
        if (myChat.getUserA().equals(myChat.getUserB())) {
            myChat.setErrorMessage("** you can't send messages to yourself");
            return false;
        }
        if (!userIsRegistered(myChat.getUserB())) {
            myChat.setErrorMessage("** user \"" + myChat.getUserB() + "\" is not registered");
            return false;
        }
        return true;
    }


    /**
     * Schnittstelle die vom TwoPhaseCommitWorker aufgerufen wird
     *
     * @param myLogin Referent für Login der commited wird
     */
    public void commitLogin(Login myLogin) {
        loggedUsers.put(myLogin.getUsername(), null);
    }

    public void commitRegister(Register myRegister) {
        loggedUsers.put(myRegister.getUsername(), null);
        Client myClient = new Client(myRegister.getUsername(), myRegister.getPassword(), 0);
        registeredUsers.put(myRegister.getUsername(), myClient);
        addUser(myRegister);
    }

    public void commitMessage(Message myMessage) {
        writeMessage(myMessage);
        updateLamportCounter(myMessage.getHeader().getSendFrom(), myMessage.getHeader().getLocalLamportCounter());
    }

    public void commitChat(Chat myChat) {
        if (chatExists(myChat)) {
            ArrayList<Message> messages = returnChatMessages(myChat);
            Collections.sort(messages);
            myChat.setMessages(messages);
            myChat.setNewCreated(false);
        } else {
            addChat(myChat);
            myChat.setNewCreated(true);
        }
    }


    public void loginUser(Login myLogin, ObjectOutputStream clientOut) {
        loggedUsers.put(myLogin.getUsername(), clientOut);
    }

    public void abortLogin(Login myLogin) {
        loggedUsers.remove(myLogin.getUsername());
    }

    public void abortRegister(Register myRegister) {
        ArrayList<String> bufferedUsers = new ArrayList<>();

        loggedUsers.remove(myRegister.getUsername());
        registeredUsers.remove(myRegister.getUsername());

        // read all registered users and filter the new one
        try {
            Scanner userFileReader = new Scanner(userFile);
            while (userFileReader.hasNextLine()) {
                String data = userFileReader.nextLine();
                String[] userData = data.split(";");
                String username = userData[0];
                if (!username.equals(myRegister.getUsername())) {
                    bufferedUsers.add(data + "\n");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }

        // write filtered users back into user file
        try {
            FileWriter writer = new FileWriter(userFile, false);
            for (String user : bufferedUsers) {
                writer.write(user);
            }
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }


    }

    public void abortMessage(Message myMessage) {
        ArrayList<String> bufferedChats = new ArrayList<>();
        String userA = myMessage.getHeader().getSendFrom();
        String userB = myMessage.getHeader().getSendTo();

        // read all chats and filter the one where the message appears
        try {
            Scanner chatFileReader = new Scanner(chatFile);
            while (chatFileReader.hasNextLine()) {
                String data = chatFileReader.nextLine();
                String[] chatData = data.split(";");
                String chatID = chatData[0];
                if (chatID.equals(userA + userB) || chatID.equals(userB + userA)) {
                    StringBuilder newChat = new StringBuilder();
                    // read all messages from filtered chat and filter the new one
                    for (int i = 1; i < chatData.length; i++) {
                        String[] messageData = chatData[i].split("#%#");
                        String messageID = messageData[0];
                        if (!messageID.equals(myMessage.getHeader().getMessageId().toString())) {
                            newChat.append(chatData[i]);
                        }
                    }
                    newChat.append("\n");
                    bufferedChats.add(newChat.toString());
                } else {
                    bufferedChats.add(data + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }

        // write all chats back into chat file
        try {
            FileWriter chatFileWriter = new FileWriter(chatFile, false);
            for (String chat : bufferedChats) {
                chatFileWriter.write(chat);
            }
            chatFileWriter.close();
        } catch (IOException e){
            System.err.println(e);
        }
        readChats();
    }

    public void abortChat(Chat myChat) {
        if (myChat.isNewCreated()) {
            ArrayList<String> bufferedChats = new ArrayList<>();
            ArrayList<String> bufferedChatList = new ArrayList<>();

            existingChatList.remove(myChat.getChatId());

            // read all existing chats and filter the new one
            try {
                Scanner chatFileReader = new Scanner(chatFile);
                while (chatFileReader.hasNextLine()) {
                    String data = chatFileReader.nextLine();
                    String[] chatData = data.split(";");
                    String chatID = chatData[0];
                    if (!chatID.equals(myChat.getChatId())) {
                        bufferedChats.add(data + "\n");
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println(e);
            }

            // read all existing chat list entries and filter the new one
            try {
                Scanner chatListFileReader = new Scanner(chatListFile);
                while (chatListFileReader.hasNextLine()) {
                    String data = chatListFileReader.nextLine();
                    String[] chatData = data.split(";");
                    String chatID = chatData[0];
                    if (!chatID.equals(myChat.getChatId())) {
                        bufferedChatList.add(data + "\n");
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println(e);
            }

            // write filtered chats back into the chat file
            try {
                FileWriter chatWriter = new FileWriter(chatFile, false);
                for (String chat : bufferedChats) {
                    chatWriter.write(chat);
                }
                chatWriter.close();
            } catch (IOException e) {
                System.err.println(e);
            }

            // write filtered chat list entries back into the chatList file
            try {
                FileWriter chatListWriter = new FileWriter(chatListFile, false);
                for (String chatListEntry : bufferedChatList) {
                    chatListWriter.write(chatListEntry);
                }
                chatListWriter.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public void writeLogEntry(String logEntry) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(logFile, true);
            writer.write(logEntry + "\n");
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
    }

    public ObjectOutputStream getChatPartnerSocket(Message myMessage) {
        String chatPartnerUsername = myMessage.getHeader().getSendTo();
        return loggedUsers.get(chatPartnerUsername);
    }

    public void addChat(Chat myChat) {
        try {
            FileWriter writer = new FileWriter(chatListFile, true);
            FileWriter chatWriter = new FileWriter(chatFile, true);
            chatWriter.write(myChat.getChatId());
            writer.write(myChat.getChatId() + ";" + myChat.getUserA() + ";" + myChat.getUserB() + "\n");
            existingChatList.put(myChat.getChatId(), new Chat(myChat.getChatId(), myChat.getUserA(), myChat.getUserB()));
            writer.close();
            chatWriter.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public HashMap<String, ObjectOutputStream> getLoggedUsers() {
        return loggedUsers;
    }

    public void loginUser(String username, ObjectOutputStream clientOut) {
        loggedUsers.put(username, clientOut);
    }

    public void updateLamportCounter(String username, int newCounter) {
        ArrayList<String> bufferedUsers = new ArrayList<>();

        // read all users and update lamport counter of filtered one
        try {
            Scanner userFileReader = new Scanner(userFile);
            while (userFileReader.hasNextLine()) {
                String data = userFileReader.nextLine();
                String[] userData = data.split(";");
                String usernameData = userData[0];
                if(username.equals(usernameData)) {
                    String password = userData[1];
                    bufferedUsers.add(username + ";" + password + ";" + newCounter + "\n");
                } else {
                    bufferedUsers.add(data + "\n");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }

        // write updated users back into user file
        try {
            FileWriter writer = new FileWriter(userFile, false);
            for (String user : bufferedUsers) {
                writer.write(user);
            }
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }


    }

    public HashMap<String, Client> getRegisteredUsers() {
        return registeredUsers;
    }
}