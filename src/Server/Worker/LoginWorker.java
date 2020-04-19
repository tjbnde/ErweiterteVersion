package Server.Worker;

import Model.Login;
import Server.DataManager;

import java.io.*;
import java.util.Date;


public class LoginWorker extends Worker {
    private Login myLogin;

    /**
     * Konstruktor der Klasse
     *
     * @param dataManager Datenmanager
     * @param clientOut   Output Stream zum Client
     * @param clientIn    Input Stream vom Client
     * @param hostname    Hostname vom anderen Server
     * @param myLogin     Zu verarbeitender Login
     * @see Worker#Worker(DataManager, ObjectOutputStream, ObjectInputStream, String)
     */
    public LoginWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname, Login myLogin) {
        super(dataManager, clientOut, clientIn, hostname);
        this.myLogin = myLogin;
    }

    /**
     * Start of the thread. Processes client command to login
     *
     * @see #twoPhaseCommitLogin()
     * @see Server.DataManager#loginUser(Login, ObjectOutputStream)
     * @see Server.DataManager#writeLogEntry(String)
     * @see DataManager#abortLogin(Login)
     * @see Worker#closeClientConnection()
     */
    public void run() {
        if (twoPhaseCommitLogin()) {
            myLogin.setSuccessful(true);
            dataManager.commitLogin(myLogin);
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " successful");
        } else {
            myLogin.setSuccessful(false);
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " not successful");
        }

        try {
            clientOut.writeObject(myLogin);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println("** lost connection to client");
            if (myLogin.isSuccessful()) {
                dataManager.abortLogin(myLogin);
            }
        } finally {
            closeClientConnection();
        }
    }


    /**
     * Checks if a login is successful with the  "Two Phase Commit" protocol
     *
     * @return Success of login
     * @see Worker#openServerConnection()
     * @see Server.DataManager#writeLogEntry(String)
     * @see #sendLoginToOtherServer() ()
     * @see Server.DataManager#loginCanBeCommited(Login)
     * @see #readLoginFromOtherServer() ()
     * @see Worker#closeServerConnection()
     */
    private boolean twoPhaseCommitLogin() {
        if(!openServerConnection()){
            myLogin.setErrorMessage("** connection to server failed");
            return false;
        }

        dataManager.writeLogEntry(new Date() + " - preparing commit of login for user " + myLogin.getUsername());
        myLogin.setStatus("PREPARE");

        if(!sendLoginToOtherServer()) {
            return false;
        }


        if (!dataManager.loginCanBeCommited(myLogin)) {
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can not be commited - wrong username or password");
            myLogin.setStatus("ABORT");
            sendLoginToOtherServer();
            closeServerConnection();
            return false;
        }

        if(!readLoginFromOtherServer()) {
            return false;
        }

        if (myLogin.getStatus().equals("ABORT")) {
            myLogin.setStatus("ABORT");
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can not be commited");
            sendLoginToOtherServer();
            closeServerConnection();
            return false;
        }

        if (myLogin.getStatus().equals("READY")) {
            myLogin.setStatus("COMMIT");
            dataManager.writeLogEntry(new Date() + " - login for user " + myLogin.getUsername() + " can be commited");
        }

        if(!sendLoginToOtherServer()) {
            return false;
        }

        if(!readLoginFromOtherServer()) {
            return false;
        }

        closeServerConnection();

        return myLogin.getStatus().equals("OK");
    }


    /**
     * Reads a login from other server during two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean readLoginFromOtherServer() {
        try {
            myLogin = (Login) serverIn.readObject();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            myLogin.setErrorMessage("** connection to server failed");
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    /**
     * Sends a login to the other server during two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean sendLoginToOtherServer() {
        try {
            serverOut.writeObject(myLogin);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println("** lost connection to server");
            myLogin.setErrorMessage("** connection to server failed");
            return false;
        }

        return true;
    }
}