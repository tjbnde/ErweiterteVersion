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
        Object nextElement = null;

        while (true) {

            try {
                connectionToOtherServer = server.accept();
            } catch (IOException e) {
                System.err.println("** connection to server failed");
                continue;
            }

            try {
                OutputStream outputStream = connectionToOtherServer.getOutputStream();
                serverOut = new ObjectOutputStream(outputStream);
                InputStream inputStream = connectionToOtherServer.getInputStream();
                serverIn = new ObjectInputStream(inputStream);
            } catch (IOException e) {
                System.err.println("** lost connection to server");
                continue;
            }

            try {
                nextElement = serverIn.readObject();
            } catch (IOException e) {
                System.err.println("** lost connection to server");
                continue;
            } catch (ClassNotFoundException e) {
                System.err.println(e);
            }


            if (nextElement instanceof Message) {
                Message myMessage = (Message) nextElement;

                dataManager.writeLogEntry(new Date() + " - testing if message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                if (dataManager.messageCanBeCommited(myMessage)) {
                    myMessage.setStatus("READY");
                    dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) + " can be committed locally");
                } else {
                    myMessage.setStatus("ABORT");
                    dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) + " can not be committed locally");
                }

                try {
                    serverOut.writeObject(myMessage);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                }

                try {
                    myMessage = (Message) serverIn.readObject();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                } catch (ClassNotFoundException e) {
                    System.err.println(e);
                }

                if (myMessage.getStatus().equals("COMMIT")) {
                    dataManager.commitMessage(myMessage);
                    dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) + " committed locally");
                } else {
                    dataManager.writeLogEntry(new Date() + " - message " + (myMessage.getHeader().getMessageId()) + " not commited");
                }

                myMessage.setStatus("OK");

                try {
                    serverOut.writeObject(myMessage);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    dataManager.abortMessage(myMessage);
                }
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

                try {
                    serverOut.writeObject(myLogin);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                }

                try {
                    myLogin = (Login) serverIn.readObject();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                } catch (ClassNotFoundException e) {
                    System.err.println(e);
                }


                if (myLogin.getStatus().equals("COMMIT")) {
                    dataManager.commitLogin(myLogin);
                    dataManager.writeLogEntry(new Date() + " - login of user " + myLogin.getUsername() + " committed locally");
                } else {
                    dataManager.writeLogEntry(new Date() + " - login of user " + myLogin.getUsername() + " not committed");
                }

                myLogin.setStatus("OK");


                try {
                    serverOut.writeObject(myLogin);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    dataManager.abortLogin(myLogin);
                }
            } else if (nextElement instanceof Register) {
                Register myRegister = (Register) nextElement;

                dataManager.writeLogEntry(new Date() + " - testing if register of user " + myRegister.getUsername() + " is successful");
                if (dataManager.registerCanBeCommited(myRegister)) {
                    myRegister.setStatus("READY");
                    dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " can be committed locally");
                } else {
                    myRegister.setStatus("ABORT");
                    dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " can not be committed locally");
                }

                try {
                    serverOut.writeObject(myRegister);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                }

                try {
                    myRegister = (Register) serverIn.readObject();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                } catch (ClassNotFoundException e) {
                    System.err.println(e);
                }

                if (myRegister.getStatus().equals("COMMIT")) {
                    dataManager.commitRegister(myRegister);
                    dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " committed locally");
                } else {
                    dataManager.writeLogEntry(new Date() + " - register of user " + myRegister.getUsername() + " not committed");
                }

                myRegister.setStatus("OK");

                try {
                    serverOut.writeObject(myRegister);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    dataManager.abortRegister(myRegister);
                }
            } else if (nextElement instanceof Chat) {
                Chat myChat = (Chat) nextElement;

                dataManager.writeLogEntry(new Date() + " - testing if chat " + myChat.getChatId() + " is successful");
                if (dataManager.chatCanBeCommited(myChat)) {
                    myChat.setStatus("READY");
                    dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " can be committed locally");
                } else {
                    myChat.setStatus("ABORT");
                    dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " can not be committed locally");
                }

                try {
                    serverOut.writeObject(myChat);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                }

                try {
                    myChat = (Chat) serverIn.readObject();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    continue;
                } catch (ClassNotFoundException e) {
                    System.err.println(e);
                }

                if (myChat.getStatus().equals("COMMIT")) {
                    dataManager.commitChat(myChat);
                    dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " committed locally");
                } else {
                    dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " aborted locally");
                }

                myChat.setStatus("OK");

                try {
                    serverOut.writeObject(myChat);
                    serverOut.flush();
                } catch (IOException e) {
                    System.err.println("** lost connection to server");
                    dataManager.abortChat(myChat);
                }
            }
        }
    }

}
