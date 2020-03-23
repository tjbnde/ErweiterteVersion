package Server.Worker;

import Model.Register;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class RegisterWorker extends Worker {
    private ObjectOutputStream clientOut;
    private Register newRegister;

    public RegisterWorker(DataManager dataManager, ObjectOutputStream networkOut, Register newRegister) {
        super(dataManager);
        this.clientOut = networkOut;
        this.newRegister = newRegister;
    }

    public void run() {
        if(!newRegister.isUsernameAvailable()) {
            if (dataManager.userNameAvailable(newRegister.getUsername())) {
                newRegister.setUsernameAvailable(true);
            } else {
                newRegister.setErrorMessage("** username is already taken");
            }
            try {
                clientOut.writeObject(newRegister);
                clientOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
        } else {
            dataManager.addUser(newRegister);
            newRegister.setSuccessful(true);
            newRegister.setErrorMessage("");
            try {
                clientOut.writeObject(newRegister);
                clientOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
