import java.io.Serializable;

public class Register implements Serializable {
    private String username;
    private String password;

    private boolean usernameAvailable;

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    private String errorMessage;
    private boolean successful;

    public Register(String username) {
        this.password = "";
        this.username = username;
        errorMessage = "";
        successful = false;
        usernameAvailable = false;
    }

    public boolean isUsernameAvailable() {
        return usernameAvailable;
    }

    public void setUsernameAvailable(boolean usernameAvailable) {
        this.usernameAvailable = usernameAvailable;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
