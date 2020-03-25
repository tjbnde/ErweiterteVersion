package Server.Worker;

import Model.Login;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class LoginWorker extends Worker {
    private Login newLogin;

    public LoginWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Login newLogin) {
        super(dataManager, clientOut, clientIn);
        this.dataManager = dataManager;
        this.newLogin = newLogin;
    }

    public void run() {
        if (dataManager.validateUser(newLogin.getUsername(), newLogin.getPassword())) {
            newLogin.setSuccessful(true);
            newLogin.setErrorMessage("");
        } else {
            newLogin.setSuccessful(false);
            newLogin.setErrorMessage("** wrong username or password");
        }
        try {
            clientOut.writeObject(newLogin);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }
    }
}