package Client;

public class ClientView {

    public void print(String message) {
        System.out.println(message);
    }

    public void start() {
        this.print("AAAAAA Client starting ...");
    }

    public void initialMenu() {
        this.print("");
        this.print("---------------------------------");
        this.print("  1. Register");
        this.print("  2. Login");
        this.print("  3. Logout");
        this.print("  0. Exit");
        this.print("---------------------------------");
    }

    public void exit() {
        this.print("Exiting ...");
    }

    public void waiting() {
        this.print("Waiting for server response ...");
    }

    public void responseStatus(String status) {
        this.print("Server response: " + status);
    }

    public void serverAddressPrompt() {
        this.print(" > Enter server address: ");
    }

    public void serverPortPrompt() {
        this.print(" > Enter server port: ");
    }

    public void errorInput() {
        this.print("Invalid input");
    }

    public void errorIO() {
        this.print("IO error");
    }

    public void errorInterrupted() {
        this.print("Interrupted");
    }

    public void usernamePrompt() {
        this.print(" > Enter username: ");
    }

    public void passwordPrompt() {
        this.print(" > Enter password: ");
    }   

    public void alreadyLoggedIn() {
        this.print("Already logged in.");
    }

}
