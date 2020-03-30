package Model;

import java.io.Serializable;


public class Login implements Serializable {
    private String username;
    private String password;

    private boolean successful;
    private String errorMessage;

    private String status;



    public Login(String username, String password) {
        this.username = username;
        this.password = password
        ;
        this.successful = false;
        this.errorMessage = "";
        status = "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
