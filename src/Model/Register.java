package Model;

import java.io.Serializable;

public class Register implements Serializable {
    private String username;
    private String password;
    private String errorMessage;
    private String status;
    private boolean successful;

    public Register(String username, String password) {
        this.password = password;
        this.username = username;
        errorMessage = "";
        successful = false;
        status = "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
