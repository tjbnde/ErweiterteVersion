import java.io.IOException;
import java.io.ObjectOutputStream;

public class RegisterWorker extends Worker {
    private ObjectOutputStream networkOut;
    private Register newRegister;

    public RegisterWorker(DataManager dataManager, ObjectOutputStream networkOut) {
        super(dataManager);
        this.networkOut = networkOut;
    }

    public void run() {
        while (true) {
            if(newRegister.getPassword().equals("")) {
                if (dataManager.checkUsername(newRegister.getUsername())) {
                    newRegister.setErrorMessage("** ERROR: username is already taken");
                    try {
                        networkOut.writeObject(newRegister);
                        networkOut.flush();
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                } else {
                    newRegister.setErrorMessage("");
                    newRegister.setSuccessful(true);
                    try {
                        networkOut.writeObject(newRegister);
                        networkOut.flush();
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            }





        }
    }
}
