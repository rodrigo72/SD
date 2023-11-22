package Client;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;
import Packets.Server.*;

public class ClientController {
    private Client client;
    private ClientView view;
    private Scanner scanner;
    private int option;
    private boolean loggedIn;

    public ClientController(Scanner scanner, ClientView view) {
        this.client = null;
        this.view = view;
        this.scanner = scanner;
        this.option = -1;
    }

    public void start() {

        String address;
        int port;

        this.view.start();
        while (true) {
            try {
                this.view.serverAddressPrompt();
                address = this.scanner.nextLine();
                this.view.serverPortPrompt();
                port = Integer.parseInt(this.scanner.nextLine());
                break;
            } catch (InputMismatchException | NumberFormatException e) {
                this.view.errorInput();
            }
        }

        try {
            this.client = new Client(address, port);
        } catch (IOException e) {
            this.view.errorIO();
        }

        while (this.option != 0) {
            this.view.initialMenu();
            try {
                this.option = Integer.parseInt(this.scanner.nextLine());

                switch (this.option) {
                    case 1 -> this.register();
                    case 2 -> this.login();
                    case 3 -> this.logout();
                    case 0 -> this.exit();
                    default -> this.view.errorInput();
                }

            } catch (InputMismatchException | NumberFormatException e) {
                this.view.errorInput();
            } catch (IOException e) {
                this.view.errorIO();
            } catch (Exception e) {
                this.view.errorInterrupted();
                break;
            }
        }
    }

    public void register() {

        if (this.loggedIn) {
            this.view.alreadyLoggedIn();
            return;
        }

        this.getCredentials();

        try {
            long id = this.client.sendRegistration();
            this.view.waiting();
            ServerStatusPacket packet = (ServerStatusPacket) this.client.receive(id);
            ServerStatusPacket.Status status = packet.getStatus();
            this.view.responseStatus(status.toString());
            switch (status) {
                case SUCCESS:
                    this.loggedIn = true;
                    break;
                default:
                    break;
            }

        } catch (InterruptedException e) {
            this.view.errorInterrupted();
        } catch (IOException  e) {
            this.view.errorIO();
        }
    }

    public void login() {

        if (this.loggedIn) {
            this.view.alreadyLoggedIn();
            return;
        }

        this.getCredentials();
        // this.client.login();
    }

    public void logout() {
        // this.client.logout();
    }

    public void exit() throws IOException {
        this.client.exit();

    }

    private void getCredentials() {
        String name;
        String password;
        while (true) {
            try {
                this.view.usernamePrompt();
                name = this.scanner.nextLine();
                this.view.passwordPrompt();
                password = this.scanner.nextLine();
                break;
            } catch (InputMismatchException e) {
                this.view.errorInput();
            }
        }
        this.client.createRegistration(name, password);
        this.view.print("");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ClientView view = new ClientView();
        ClientController controller = new ClientController(scanner, view);
        controller.start();
    }

}
