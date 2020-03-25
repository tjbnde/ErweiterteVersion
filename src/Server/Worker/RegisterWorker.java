package Server.Worker;

import Model.Register;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class RegisterWorker extends Worker {
    private Register newRegister;

    public RegisterWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Register newRegister) {
        super(dataManager, clientOut, clientIn);
        this.newRegister = newRegister;
    }

    public void run() {
        if(dataManager.usernameIsAvailable(newRegister.getUsername())) {
            dataManager.addUser(newRegister);
            newRegister.setSuccessful(true);
            newRegister.setErrorMessage("");
        } else {
            newRegister.setErrorMessage("** username is already taken");

        }
        try {
            clientOut.writeObject(newRegister);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }

    }
}