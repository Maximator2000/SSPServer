import Datenstrukturen.List;
import com.sun.xml.internal.bind.v2.TODO;
import netz.Client;
import netz.Server;

public class SSPServer extends Server {

    private int amountOfPlayers;
    public static final int AMOUNT=5;
    private List<Player> playerList;


    public SSPServer(int pPort) {
        super(pPort);
        playerList=new List<>();
        amountOfPlayers=0;
    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        playerList.append(new Player(pClientIP,pClientPort,true));
        amountOfPlayers++;


    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        String[] messageParts = pMessage.split("$");
        if(messageParts[0].equals("Name")){
            boolean allNames=true;
            playerList.toFirst();
            while(playerList.hasAccess()){
                if(playerList.getContent().getName()==null) {
                    if (playerList.getContent().playerEquals(pClientIP, pClientPort)) {
                        playerList.getContent().setName(messageParts[1]);
                    }else{
                        allNames=false;
                    }
                }
                playerList.next();
            }
            if(amountOfPlayers==AMOUNT && allNames){
                //Aufgabe des MAtchControllers
            }
        }

    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {

    }

    public void askForName(String pClientIP,int pClientPort){
        //send(pClientIP,pClientPort,"Name");
    }

}
