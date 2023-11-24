package Client;

public class Registration {
    private final String name;
    private final String password;
    private boolean loggedIn;

    public Registration(String name, String password, boolean loggedIn) {
        this.name = name;
        this.password = password;
        this.loggedIn = loggedIn;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setLoggedInt(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean getLoggedIn() {
        return this.loggedIn;
    }
}
