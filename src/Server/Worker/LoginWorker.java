package Server.Worker;

import Model.Login;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectOutputStream;


public class LoginWorker extends Worker {
    private ObjectOutputStream networkOut;
    private Login newLogin;

    public LoginWorker(DataManager dataManager, ObjectOutputStream networkOut, Login newLogin) {
        super(dataManager);
        this.dataManager = dataManager;
        this.networkOut = networkOut;
        this.newLogin = newLogin;
    }

    public void run() {
        if (dataManager.validateUser(newLogin.getUsername(), newLogin.getPassword())) {
            newLogin.setSuccessful(true);
        } else {
            newLogin.setSuccessful(false);
            newLogin.setErrorMessage("** wrong username or password");
        }
        try {
            networkOut.writeObject(newLogin);
            networkOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                if(networkOut != null) {
                    networkOut.close();
                }
            } catch (IOException e) {
                System.err.println(e);
            }

        }
    }
}

