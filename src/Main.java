import javax.swing.*;

public class Main {

    private SSPServer sspServer;

    public static void main(String[] args) {
        new Main();
    }

    public Main(){
        sspServer= new SSPServer(1234);
    }
}
