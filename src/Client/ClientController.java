package Client;

import java.io.IOException;
import java.util.Comparator;
import java.util.InputMismatchException;
import java.util.Scanner;
import Packets.Server.*;
import java.util.List;
import Packets.Packet;
import java.util.Collections;

public class ClientController {
    private ClientAPI client;
    private ClientView view;
    private Scanner scanner;
    private int option;
    private boolean loggedIn;
    private JobManager jobs;

    public ClientController(Scanner scanner, ClientView view) {
        this.client = null;
        this.jobs = null;
        this.view = view;
        this.scanner = scanner;
        this.option = -1;
    }

    public void start() {

        this.view.start();

        while (true) {
            try {
                this.view.jobDirPrompt();
                String path = this.scanner.nextLine();

                this.view.jobResultDirPrompt();
                String resultPath = this.scanner.nextLine();
                
                if (path.equals("d") || path.equals("default"))
                    path = "/home/core/SD/JobExamples";
                
                if (resultPath.equals("d") || resultPath.equals("default"))
                    resultPath = "/home/core/SD/JobResults";

                this.jobs = new JobManager(path, resultPath);
                this.jobs.readDirectory();
                break;
            } catch (IOException e) {
                this.view.errorReadingDirectory();
            }
        }

        String address;
        int port;

        while (true) {
            try {
                this.view.serverAddressPrompt();
                address = this.scanner.nextLine();

                if (address.equals("d") || address.equals("default"))
                    address = "10.4.4.1";

                this.view.serverPortPrompt();
                String portStr = this.scanner.nextLine();

                if (portStr.equals("d") || portStr.equals("default"))
                    port = 8888;
                else
                    port = Integer.parseInt(portStr);

                try {
                    this.client = new Client(address, port, this.jobs);
                    break;
                } catch (IOException e) {
                    this.view.errorIO();
                }
            } catch (InputMismatchException | NumberFormatException e) {
                this.view.errorInput();
            }
        }

        while (this.option != 0) {
            this.view.initialMenu();
            try {
                this.option = Integer.parseInt(this.scanner.nextLine());

                switch (this.option) {
                    case 1 -> this.register();
                    case 2 -> this.login();
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
                case SUCCESS -> { 
                    this.loggedIn = true; 
                    this.loggedInMenu();
                }
                default -> { this.view.failedRegistration(); }
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

        try {
            long id = this.client.sendLogin();
            this.view.waiting();
            ServerStatusPacket packet = (ServerStatusPacket) this.client.receive(id);
            ServerStatusPacket.Status status = packet.getStatus();
            this.view.responseStatus(status.toString());
            switch (status) {
                case SUCCESS -> { 
                    this.loggedIn = true; 
                    this.loggedInMenu();
                }
                default -> { this.view.failedLogin(); }
            }

        } catch (InterruptedException e) {
            this.view.errorInterrupted();
        } catch (IOException  e) {
            this.view.errorIO();
        }
    }

    public void logout() {

        if (!this.loggedIn) {
            this.view.notLoggedIn();
            return;
        }

        try {
            long id = this.client.sendLogout();
            this.view.waiting();
            ServerStatusPacket packet = (ServerStatusPacket) this.client.receive(id);
            ServerStatusPacket.Status status = packet.getStatus();
            this.view.responseStatus(status.toString());
            switch (status) {
                case SUCCESS -> { this.loggedIn = false; }
                default -> { this.view.failedLogout(); }
            }
        } catch (InterruptedException e) {
            this.view.errorInterrupted();
        } catch (IOException  e) {
            this.view.errorIO();
        }
        
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

    public void loggedInMenu() {
        while (this.option != 0) {
            this.view.loggedInMenu();
            try {
                this.option = Integer.parseInt(this.scanner.nextLine());
                this.view.print("");

                switch (this.option) {
                    case 1 -> this.listJobs();
                    case 2 -> this.listJobResults();
                    case 3 -> this.listJobRequestsSent();
                    case 4 -> this.getInfo();
                    case 0 -> this.logout();
                    default -> this.view.errorInput();
                }

            } catch (InputMismatchException | NumberFormatException e) {
                this.view.errorInput();
            } catch (Exception e) {
                this.view.errorInterrupted();
                break;
            }
        }
        this.option = -1;
    }

    public void getInfo() {
        if (!this.loggedIn) {
            this.view.notLoggedIn();
            return;
        }

        try {
            long id = this.client.sendGetInfo();
            this.view.waiting();
            ServerInfoPacket packet = (ServerInfoPacket) this.client.receive(id);
            this.view.print(packet.toString());
        } catch (IOException e) {
            this.view.errorIO();
        } catch (InterruptedException e) {
            this.view.errorInterrupted();
        }
    }

    public void listJobs() {
        List<String> jobs = this.jobs.getJobNames();
        this.view.printJobs(jobs);
        while (this.option != 0) {
            try {
                this.view.sendJobPrompt();
                this.option = Integer.parseInt(this.scanner.nextLine());

                if (this.option == 0)
                    break;

                byte[] job = this.jobs.getJob(jobs.get(this.option - 1));

                if (job == null) {
                    this.view.errorInput();
                    continue;
                }

                this.view.requiredMemoryPrompt();
                int memory = Integer.parseInt(this.scanner.nextLine());

                this.client.sendJob(memory, job);
            } catch (InputMismatchException | NumberFormatException e) {
                this.view.errorInput();
            } catch (Exception e) {
                this.view.errorInterrupted();
                break;
            }
        }
        this.option = -1;
    }

    public void listJobResults() {
        try {

            List<Packet> packets = this.client.getJobResults();

            if (packets.isEmpty()) {
                this.view.print("No results");
            } else {
                Collections.sort(packets, Comparator.comparingLong(Packet::getId));
                List<String> jobs = packets.stream().map(Packet::toString).toList();
                this.view.printJobs(jobs);
            }
        } catch (InputMismatchException | NumberFormatException e) {
            this.view.errorInput();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            this.view.errorInterrupted();
        }
        this.option = -1;
    }
    
    public void listJobRequestsSent() {
        try {

            List<Packet> packets = this.client.getJobRequests();

            if (packets.isEmpty()) {
                this.view.print("No job requests sent on this connection.");
            } else {
                Collections.sort(packets, Comparator.comparingLong(Packet::getId));
                List<String> jobs = packets.stream().map(Packet::toString).toList();
                this.view.printJobs(jobs);
            }
        } catch (InputMismatchException | NumberFormatException e) {
            this.view.errorInput();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            this.view.errorInterrupted();
        }
        this.option = -1;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ClientView view = new ClientView();
        ClientController controller = new ClientController(scanner, view);
        controller.start();
    }

}