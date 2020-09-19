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
    private Match[] matches;



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
        if(messageParts[0].equals("name")){
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
                startRound();
            }
        } else if(messageParts[0].equals("spiele")){
            if(matches!=null && matches.length>0){
                for(int i=0;i<=matches.length;i++){
                    if(matches[i].contains(pClientIP,pClientPort)){
                        matches[i].setChoice(pClientIP,pClientPort,messageParts[1]);
                        if(matches[i].getPlayer1().playerEquals(pClientIP,pClientPort)){
                            send(pClientIP,pClientPort,"gegner$auswahl$"+matches[i].getChoice2());
                        }else{
                            send(pClientIP,pClientPort,"gegner$auswahl$"+matches[i].getChoice1());
                        }
                    }
                }
                //TODO: Ein Match muss reagieren
                // sobald es beide ergebnisse kennt und vergleichen wer gewinnt.
                // Daraufhin gibt es die Ergebnisse weiter
                // und wertet Punkte aus und bestimmt den weiteren Spielverlauf
            }
        }

    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {

    }

    public void askForName(String pClientIP,int pClientPort){
        send(pClientIP,pClientPort,"sende$name");
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
        matches= new Match[2];
        //spielentstehung
        Player restPlayer=null;
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
                if(matchNum==0 ||
                        (matchNum==1 && match.matchEquals(matches[0].getPlayer1(),matches[0].getPlayer2()))){
                    matches[matchNum]=match;
                    matchNum++;
                }
            }else{
                restPlayer=enemies[playerNum];
            }
        }
        //spielvorgang
        for(int i=0;i<=matches.length;i++){
            send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                    "sende$möglichkeiten");
            send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                    "sende$möglichkeiten");
            send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                    "gegner$name$"+matches[i].getPlayer2().getName());
            send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                    "gegner$name$"+matches[i].getPlayer1().getName());
        }
        if(restPlayer!=null) {
            send(restPlayer.getpClientIP(), restPlayer.getpClientPort(), "status$aussetzen");
        }
    }



}
