package Server.Worker;

import Model.Login;
import Model.Message;
import Server.DataManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TwoPhaseCommitWorker implements Runnable {
    private Socket connectionToOtherServer;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

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
    }

    @Override
    public void run() {
        while(true) {
            try {
                connectionToOtherServer = server.accept();
                OutputStream outputStream = connectionToOtherServer.getOutputStream();
                serverOut = new ObjectOutputStream(outputStream);
                InputStream inputStream = connectionToOtherServer.getInputStream();
                serverIn = new ObjectInputStream(inputStream);


                Object nextElement = serverIn.readObject();
                if(nextElement instanceof Message) {
                    Message myMessage = (Message) nextElement;
                    switch(myMessage.getStatus()) {
                        case "PREPARE":
                            dataManager.writeLogEntry(System.currentTimeMillis() + " - testing if message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                            if(dataManager.messageCanBeCommited(myMessage)) {
                                myMessage.setStatus("READY");
                                dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                            } else {
                                myMessage.setStatus("ABORT");
                                dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " can not be committed locally");
                            }
                            break;
                        case "COMMIT":
                            dataManager.writeMessage(myMessage);
                            dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " committed successfully");
                            myMessage.setStatus("OK");
                            break;
                        case "ABORT":
                            dataManager.abortMessage(myMessage);
                            dataManager.writeLogEntry(System.currentTimeMillis() + " - message " + (myMessage.getHeader().getMessageId()) + " aborted successfully");
                            myMessage.setStatus("OK");
                            break;
                    }
                    serverOut.writeObject(myMessage);
                    serverOut.flush();
                } else if(nextElement instanceof Login) {
                    Login myLogin = (Login) nextElement;
                    switch (myLogin.getStatus()) {
                        case "PREPARE":
                            dataManager.writeLogEntry(System.currentTimeMillis() + " - testing if login of user " + myLogin.getUsername() + "is successful");
                            if(dataManager.loginCanBeCommited(myLogin)){
                                myLogin.setStatus("READY");
                                dataManager.writeLogEntry(System.currentTimeMillis() + " - login of user " + myLogin.getUsername() + " can be committed locally");
                            } else {
                                myLogin.setStatus("ABORT");
                                dataManager.writeLogEntry(System.currentTimeMillis() + " - login of user " + myLogin.getUsername() + " can not be committed locally");
                            }
                            break;
                        case "COMMIT":

                            break;
                        case "ABORT":
                            break;

                    }
                }

            } catch(IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }
}
