package Client;

public class Registration {
    private final String name;
    private final String password;

    public Registration(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }
}
