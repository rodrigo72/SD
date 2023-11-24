package Client;

import java.util.List;

public class ClientView {

    public void print(String str) {
        System.out.println(str);
    }

    public void initialMenu() {
        this.print("");
        this.print("---------------------------------");
        this.print("  1. Register");
        this.print("  2. Login");
        this.print("  0. Exit");
        this.print("---------------------------------");
    }

    public void loggedInMenu() {
        this.print("");
        this.print("---------------------------------");
        this.print("  1. List jobs");
        this.print("  2. List job results");
        this.print("  0. Logout");
        this.print("---------------------------------");
    }

    public void start()                         { this.print("\nClient starting ...\n"); }
    public void exit()                          { this.print("Exiting ..."); }
    public void waiting()                       { this.print("Waiting for server response ..."); }
    public void responseStatus(String status)   { this.print("Server response: " + status); }
    public void serverAddressPrompt()           { this.print(" > Enter server address: "); }
    public void serverPortPrompt()              { this.print(" > Enter server port: "); }
    public void errorInput()                    { this.print("Invalid input"); }
    public void errorIO()                       { this.print("IO error"); }
    public void error()                         { this.print("Error"); }
    public void errorInterrupted()              { this.print("Interrupted"); }
    public void usernamePrompt()                { this.print(" > Enter username: "); }
    public void passwordPrompt()                { this.print(" > Enter password: "); }   
    public void alreadyLoggedIn()               { this.print("Already logged in."); }
    public void failedLogin()                   { this.print("Failed to login."); }
    public void failedRegistration()            { this.print("Failed to register."); }
    public void failedLogout()                  { this.print("Failed to logout."); }
    public void notLoggedIn()                   { this.print("Not logged in."); }
    public void jobDirPrompt()                  { this.print(" > Enter job directory path: "); } 
    public void jobResultDirPrompt()            { this.print(" > Enter job result directory path: "); }
    public void errorReadingDirectory()         { this.print("Error reading directory."); }
    public void sendJobPrompt()                 { this.print(" > Select job send: [0 - exit]"); }
    public void requiredMemoryPrompt()          { this.print(" > Enter required memory: "); }
    public void waitingForResult(long id)       { this.print("Still waiting for result (" + id + ")"); }

    public void printJobs(List<String> jobs) {
        StringBuilder str = new StringBuilder();
        int count = 1;
        for (String jobName : jobs) {
            str.append(count).append(". ").append(jobName).append("\n");
            count++;
        }
        this.print(str.toString());
    }
}