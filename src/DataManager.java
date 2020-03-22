import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class DataManager {
    private File userFile;
    private File chatListFile;
    private File chatFile;

    private HashMap<String, Client> registeredUsers;



    public DataManager(String userFileName, String chatListFileName, String chatFileName) {
        userFile = new File(userFileName);
        chatListFile = new File(chatListFileName);
        chatFile = new File(chatFileName);

        registeredUsers = new HashMap<>();
        readRegisteredUsers();

    }


    public void addUser(Register newRegister) {
        newRegister.setPassword(Helper.hashPassword(newRegister.getPassword()));
        try {
            FileWriter writer = new FileWriter(userFile, true);
            writer.write(newRegister.getUsername() + ";" + newRegister.getPassword() + "\n");
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void readRegisteredUsers() {
        try {
            Scanner userFileReader = new Scanner(userFile);
            while(userFileReader.hasNextLine()) {
                String data = userFileReader.nextLine();
                String[] userData = data.split(";");
                String username = userData[0];
                String password = userData[1];
                Client myClient = new Client(username, password);
                registeredUsers.put(myClient.getUsername(), myClient);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }

    public boolean checkUser(String username, String password) {
        if(registeredUsers.containsKey(username)) {
            Client user = registeredUsers.get(username);
            return user.getPassword().equals(password);
        }
        return false;
    }

    public boolean checkUsername(String username) {
        return registeredUsers.containsKey(username);
    }
}
