package Server.Worker;

import Model.Register;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class RegisterWorker extends Worker {
    private Register myRegister;

    public RegisterWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Register myRegister) {
        super(dataManager, clientOut, clientIn);
        this.myRegister = myRegister;
    }

    public void run() {
        if(dataManager.usernameIsAvailable(myRegister.getUsername())) {
            dataManager.addUser(myRegister);
            myRegister.setSuccessful(true);
            myRegister.setErrorMessage("");
        } else {
            myRegister.setErrorMessage("** username is already taken");
        }
        try {
            clientOut.writeObject(myRegister);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }

    }
}