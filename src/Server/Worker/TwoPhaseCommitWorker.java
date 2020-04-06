package Server.Worker;

import Model.Chat;
import Model.Login;
import Model.Message;
import Model.Register;
import Server.DataManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class TwoPhaseCommitWorker implements Runnable {
    private Socket connectionToOtherServer;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;


    private ObjectOutputStream clientOut;

    private ServerSocket server;

    private DataManager dataManager;

    public TwoPhaseCommitWorker(DataManager dataManager, int port) {
        this.dataManager = dataManager;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println(e);
        }
        connectionToOtherServer = null;
        serverIn = null;
        serverOut = null;
        clientOut = null;
    }

    @Override
    public void run() {
        while (true) {
            try {
                connectionToOtherServer = server.accept();
                OutputStream outputStream = connectionToOtherServer.getOutputStream();
                serverOut = new ObjectOutputStream(outputStream);
                InputStream inputStream = connectionToOtherServer.getInputStream();
                serverIn = new ObjectInputStream(inputStream);


                Object nextElement = serverIn.readObject();
                if (nextElement instanceof Message) {
                    Message myMessage = (Message) nextElement;

                    // means that the other server doesn't know the output stream to client
                    if(myMessage.getStatus().equals("OK")) {
                        clientOut = dataManager.getChatPartnerSocket(myMessage);
                        clientOut.writeObject(myMessage);
                        clientOut.flush();
                        continue;
                    }

                    dataManager.writeLogEntry(new Date() + " - testing if message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                    if (dataManager.messageCanBeCommited(myMessage)) {
                        myMessage.setStatus("READY");
                        dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                    } else {
                        myMessage.setStatus("ABORT");
                        dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) +  " can not be committed locally");
                    }
                    serverOut.writeObject(myMessage);
                    serverOut.flush();


                    myMessage = (Message) serverIn.readObject();
                    if (myMessage.getStatus().equals("COMMIT")) {
                        dataManager.commitMessage(myMessage);
                        dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) + " committed locally");
                    } else {
                        dataManager.abortMessage(myMessage);
                        dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) + " aborted locally");
                    }
                    myMessage.setStatus("OK");
                    serverOut.writeObject(myMessage);
                    serverOut.flush();
                } else if (nextElement instanceof Login) {
                    Login myLogin = (Login) nextElement;

                    dataManager.writeLogEntry(new Date() + " - testing if login of user " + myLogin.getUsername() + " is successful");
                    if (dataManager.loginCanBeCommited(myLogin)) {
                        myLogin.setStatus("READY");
                        dataManager.writeLogEntry(new Date() + " - login of user " + myLogin.getUsername() + " can be committed locally");
                    } else {
                        myLogin.setStatus("ABORT");
                        dataManager.writeLogEntry(new Date() + " - login of user " + myLogin.getUsername() + " can not be committed locally");
                    }
                    serverOut.writeObject(myLogin);
                    serverOut.flush();


                    myLogin = (Login) serverIn.readObject();
                    if (myLogin.getStatus().equals("COMMIT")) {
                        dataManager.commitLogin(myLogin);
                        dataManager.writeLogEntry(new Date() + " - login of user " + myLogin.getUsername() + " committed locally");
                    } else {
                        dataManager.abortLogin(myLogin);
                        dataManager.writeLogEntry(new Date() + " - login of user " + myLogin.getUsername() + " aborted locally");
                    }
                    myLogin.setStatus("OK");
                    serverOut.writeObject(myLogin);
                    serverOut.flush();
                } else if (nextElement instanceof Register) {
                    Register myRegister = (Register) nextElement;

                    // TODO id erstellen für register / login
                    dataManager.writeLogEntry(new Date() + " - testing if register of user " + myRegister.getUsername() + " is successful");
                    if (dataManager.registerCanBeCommited(myRegister)) {
                        myRegister.setStatus("READY");
                        dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " can be committed locally");
                    } else {
                        myRegister.setStatus("ABORT");
                        dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " can not be committed locally");
                    }
                    serverOut.writeObject(myRegister);
                    serverOut.flush();


                    myRegister = (Register) serverIn.readObject();
                    if (myRegister.getStatus().equals("COMMIT")) {
                        dataManager.commitRegister(myRegister);
                        dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " committed locally");
                    } else {
                        dataManager.abortRegister(myRegister);
                        dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " aborted locally");
                    }
                    myRegister.setStatus("OK");
                    serverOut.writeObject(myRegister);
                    serverOut.flush();
                } else if(nextElement instanceof Chat) {
                    Chat myChat = (Chat) nextElement;

                    dataManager.writeLogEntry(new Date() + " - testing if chat " + myChat.getChatId() + " is successful");
                    if (dataManager.chatCanBeCommited(myChat)) {
                        myChat.setStatus("READY");
                        dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " can be committed locally");
                    } else {
                        myChat.setStatus("ABORT");
                        dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " can not be committed locally");
                    }
                    serverOut.writeObject(myChat);
                    serverOut.flush();


                    myChat = (Chat) serverIn.readObject();
                    if (myChat.getStatus().equals("COMMIT")) {
                        dataManager.commitChat(myChat);
                        dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " committed locally");
                    } else {
                        dataManager.abortChat(myChat);
                        dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " aborted locally");
                    }
                    myChat.setStatus("OK");
                    serverOut.writeObject(myChat);
                    serverOut.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }
}
