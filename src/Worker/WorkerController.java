package Worker;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class WorkerController {
    private Worker worker;
    private Scanner scanner;
    private WorkerView view;
    
    public WorkerController(Scanner scanner, WorkerView view) {
        this.scanner = scanner;
        this.view = view;
        this.worker = null;
    }

    public void start() {

        long memoryLimit = -1;
        while (true) {
            this.view.memoryLimit();
            try {
                memoryLimit = Long.parseLong(this.scanner.nextLine());
                break;
            } catch (InputMismatchException | NumberFormatException e) {
                this.view.error();
            }
        }


        String address;
        int port;

        while (true) {
            try {
                this.view.address();
                address = this.scanner.nextLine();

                if (address.equals("d") || address.equals("default"))
                    address = "10.4.4.1";

                this.view.port();
                String portStr = this.scanner.nextLine();

                if (portStr.equals("d") || portStr.equals("default"))
                    port = 8888;
                else
                    port = Integer.parseInt(portStr);

                try {
                    this.worker = new Worker(address, port, memoryLimit);
                    break;
                } catch (IOException e) {
                    this.view.error();
                }
            } catch (InputMismatchException | NumberFormatException e) {
                this.view.error();
            }
        }

        this.worker.receive();

        int option = -1;
        while (option != 0) {
            try {
                option = Integer.parseInt(this.scanner.nextLine());
            } catch (InputMismatchException | NumberFormatException e) {
                this.view.error();
            }
        }

        try {
            this.view.disconnection();
            this.worker.sendDisconnectionPacket();
            this.worker.stop();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        WorkerView view = new WorkerView();
        Scanner scanner = new Scanner(System.in);
        WorkerController controller = new WorkerController(scanner, view);
        controller.start();
    }
}
