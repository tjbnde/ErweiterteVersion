package Server;

import Client.Client;
import Model.Chat;
import Model.Message;
import Model.Register;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class DataManager {
    private File userFile;
    private File chatListFile;
    private File chatFile;

    private HashMap<String, Client> registeredUsers;
    private HashMap<String, Chat> existingChatList;
    private HashMap<String, Socket> loggedUsers;



    public DataManager(String userFileName, String chatListFileName, String chatFileName) {
        userFile = new File(userFileName);
        chatListFile = new File(chatListFileName);
        chatFile = new File(chatFileName);

        registeredUsers = new HashMap<>();
        existingChatList = new HashMap<>();
        loggedUsers = new HashMap<>();
        readRegisteredUsers();
        readExistingChatList();
        readChats();

    }


    public void addUser(Register newRegister) {
        newRegister.setPassword(newRegister.getPassword());
        try {
            FileWriter writer = new FileWriter(userFile, true);
            writer.write(newRegister.getUsername() + ";" + newRegister.getPassword() + "\n");
            Client newClient = new Client(newRegister.getUsername(), newRegister.getPassword()))
            registeredUsers.put(newClient.getUsername(), newClient);
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public ArrayList<Message> returnChatMessages(Chat myChat) {
        String userA = myChat.getUserA();
        String userB = myChat.getUserB();
        if(existingChatList.containsKey(userA + userB)) {
            return existingChatList.get(userA + userB).getMessages();
        } else {
            return existingChatList.get(userB + userA).getMessages();
        }

    }

    private void readRegisteredUsers() {
        try {
            Scanner userFileReader = new Scanner(userFile);
            while(userFileReader.hasNextLine()) {
                String data = userFileReader.nextLine();
                String[] userData = data.split(";");
                String username = userData[0];
                String password = userData[1];
                Client myClient = new Client(username, password);
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
                ArrayList<Message> messages = existingChatList.get(chatID).getMessages();
                for(int i = 1; i < chatData.length; i++) {
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

    public boolean validateUser(String username, String password) {
        if(registeredUsers.containsKey(username)) {
            Client user = registeredUsers.get(username);
            if(user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public boolean userIsRegistered(String username) {
        return registeredUsers.containsKey(username);
    }

    public boolean userNameAvailable(String username) {
        return !registeredUsers.containsKey(username);
    }

    public boolean chatExists(Chat myChat) {
        String userA = myChat.getUserA();
        String userB = myChat.getUserB();
        return existingChatList.containsKey(userA + userB) || existingChatList.containsKey(userB + userA);
    }

    public void addChat(Chat myChat) {
        try {
            FileWriter writer = new FileWriter(chatListFile, true);
            writer.write(myChat.getChatId() + ";" + myChat.getUserA() + ";" + myChat.getUserB() + "\n");
            existingChatList.put(myChat.getChatId(), new Chat(myChat.getChatId(), myChat.getUserA(), myChat.getUserB()));
            writer.close();
        } catch(IOException e) {
            System.err.println(e);
        }
    }



}
