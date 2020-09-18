import Datenstrukturen.List;
import com.sun.xml.internal.bind.v2.TODO;
import netz.Client;
import netz.Server;

public class SSPServer extends Server {

    private int amountOfPlayers;
    private int roundNumber;
    public static final int AMOUNT=5;
    private List<Player> playerList;
    private Player[] enemies;


    public SSPServer(int pPort) {
        super(pPort);
        playerList=new List<>();
        amountOfPlayers=0;
    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        playerList.append(new Player(pClientIP,pClientPort,true));
        amountOfPlayers++;
        askForName(pClientIP,pClientPort);

    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        String[] messageParts = pMessage.split("$");
        if(messageParts[0].equals("Name")){
            boolean allNames=true;// alle Namen werden als vorhanden angenommen
            playerList.toFirst();
            while(playerList.hasAccess()){
                if(playerList.getContent().getName()==null) {
                    if (playerList.getContent().playerEquals(pClientIP, pClientPort)) {
                        playerList.getContent().setName(messageParts[1]);
                    }else{
                        allNames=false; // falls ein Name nicht vorhanden ist und nicht eingesetzt wurde
                    }
                }
                playerList.next();
            }
            if(amountOfPlayers==AMOUNT && allNames){
                //Aufgabe des MatchControllers
            }
        }

    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {

    }

    public void askForName(String pClientIP,int pClientPort){
        send(pClientIP,pClientPort,"sende$Name");
    }

    public void startRound(){
        roundNumber=-1;
        enemies=new Player[5];
        playerList.toFirst();
        int i=0;
        while(playerList.hasAccess()){
            enemies[i]=playerList.getContent();
            playerList.next();
            i++;
        }

    }

    public void playRounds(){
        roundNumber++;
        int matchNum=0;
        Match[] matches= new Match[2];
        for(int i=0;i<= enemies.length;i++){
            int playerNum=i;
            int enemyNum=2-i+roundNumber;
            if(enemyNum<1){
                enemyNum+=5;
            }
            //problem :so entstehn 4 matches!!!!!!!!!!!!!
            if(playerNum!=enemyNum ){
                Player p1=enemies[playerNum];
                Player p2=enemies[enemyNum];
                Match match=new Match(p1,p2);
                if(matchNum==1 && match.equals(matches[0])){
                    matches[1]=match;
                    matchNum++;
                }
            }
        }
    }

}
