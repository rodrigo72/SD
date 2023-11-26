package Worker;

public class WorkerView {

    public void print(String str) {
        System.out.println(str);
    }

    public void memoryLimit()   { this.print("Enter memory limit: "); }
    public void error()         { this.print("Error"); }
    public void address()       { this.print("Enter server address: "); }
    public void port()          { this.print("Enter server port: "); }
    public void disconnection() { this.print("Sent disconnection packet"); }


}
