import Datenstrukturen.List;
import com.sun.xml.internal.bind.v2.TODO;
import netz.Client;
import netz.Server;

public class SSPServer extends Server {

    private int amountOfPlayers;
    private int roundNumber;
    public static final int AMOUNT=5;
    private List<Player> playerList;
    private List<Player> alternativePlayers;
    private Player[] enemies;
    private Match[] matches;
    private int playedGames;
    private boolean roundIsOver;
    private int amountOfParticipators;


    public SSPServer(int pPort) {
        super(pPort);
        playerList=new List<>();
        alternativePlayers= new List<>();
        amountOfPlayers=0;
        roundIsOver=true;
        amountOfParticipators=5;
        System.out.println("Server wird erstellt");
    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        playerList.append(new Player(pClientIP,pClientPort,true));
        amountOfPlayers++;
        askForName(pClientIP,pClientPort);
        System.out.println("Mit Server verbundnen "+pClientIP+" "+pClientPort);

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
                        if(matches[i].isFilled() && !matches[i].isWinnerKnown()) {
                            if (matches[i].firstPlayerWins() ) {
                                matches[i].getPlayer1().addPoints(3);
                                matches[i].getPlayer2().addPoints(-1);
                                matches[i].setWinnerKnown(true);
                                //sende Punktzahl
                                send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer1().getPoints());
                                send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer2().getPoints());
                            }else if(matches[i].getChoice2().equals(matches[i].getChoice1())){
                                matches[i].getPlayer1().addPoints(1);
                                matches[i].getPlayer2().addPoints(1);
                                matches[i].setWinnerKnown(true);
                                send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer1().getPoints());
                                send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer2().getPoints());

                            }else {
                                matches[i].getPlayer1().addPoints(-1);
                                matches[i].getPlayer2().addPoints(3);
                                matches[i].setWinnerKnown(true);
                                send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer1().getPoints());
                                send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer2().getPoints());
                            }
                            //sende Gegnerwahl
                            send(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort(),
                                    "gegner$auswahl$" + matches[i].getChoice2());
                            send(matches[i].getPlayer2().getpClientIP(), matches[i].getPlayer2().getpClientPort(),
                                    "gegner$auswahl$" + matches[i].getChoice1());
                            playedGames++;
                            if(playedGames==matches.length){
                                playedGames=0;
                                playRounds();
                            }

                        }
                    }
                }

                //TODO:  Ein Match muss  vergleichen wer gewinnt.
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
        enemies=new Player[amountOfParticipators];
        playerList.toFirst();
        int i=0;
        while(playerList.hasAccess()){
            if(playerList.getContent().isInGame()){
                enemies[i]=playerList.getContent();
            }
            playerList.next();
            i++;
        }

    }

    public void playRounds(){
        roundIsOver=false;
        roundNumber++;
        playedGames=0;
        int matchNum=0;
        matches= new Match[2];
        if(roundNumber<5) {
            //spielentstehung
            Player restPlayer = null;
            for (int i = 0; i <= enemies.length; i++) {
                int playerNum = i;
                int enemyNum = 2 - i + roundNumber;
                if (enemyNum < 1) {
                    enemyNum += 5;
                }
                //problem :so entstehn 4 matches!!!!!!!!!!!!!
                if (playerNum != enemyNum) {
                    Player p1 = enemies[playerNum];
                    Player p2 = enemies[enemyNum];
                    Match match = new Match(p1, p2);
                    if (matchNum == 0 ||
                            (matchNum == 1 && match.matchEquals(matches[0].getPlayer1(), matches[0].getPlayer2()))) {
                        matches[matchNum] = match;
                        matchNum++;
                    }
                } else {
                    restPlayer = enemies[playerNum];
                }
            }
            //spielvorgang
            for (int i = 0; i <= matches.length; i++) {
                send(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort(),
                        "sende$möglichkeiten");
                send(matches[i].getPlayer2().getpClientIP(), matches[i].getPlayer2().getpClientPort(),
                        "sende$möglichkeiten");
                send(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort(),
                        "gegner$name$" + matches[i].getPlayer2().getName());
                send(matches[i].getPlayer2().getpClientIP(), matches[i].getPlayer2().getpClientPort(),
                        "gegner$name$" + matches[i].getPlayer1().getName());
            }
            if (restPlayer != null) {
                send(restPlayer.getpClientIP(), restPlayer.getpClientPort(), "status$aussetzen");
            }
        }else{//Jeder hat gegen jeden gespielt.
            //TODO entscheide, ob das Match beendet ist, oder mache ein Sudden Death!
            if(roundIsOver){
                playerList.toFirst();
                while(playerList.hasAccess()){
                    if(playerList.getContent().playerEquals(getBestPlayer().getpClientIP(),getBestPlayer().getpClientPort())){
                        send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),
                                "status$gewonnen");
                    }else{
                        send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),
                                "status$verloren");
                    }
                    playerList.next();
                }
                startRound();

            }else{


            }
        }
    }

    public boolean roundIsOver(){
        if(playerList!=null && !playerList.isEmpty()){
            int maxPoints=Integer.MIN_VALUE;
            boolean over=true;
            playerList.toFirst();
            while(playerList.hasAccess()){
                if(playerList.getContent().isInGame()){// falls der Spieler am Spiel beteildigt ist
                    if(playerList.getContent().getPoints()>maxPoints){// Falls es eine neue maximale Punktzahl gibt --> rückgabewert wird true
                        maxPoints=playerList.getContent().getPoints();
                        over=true;

                    }else if(playerList.getContent().getPoints()==maxPoints){// Falls jemand den selben Maximalwert wird der Rückgabewert zu false, da es momentan keinen eindeutigen Spieler  gibt
                        over=false;
                    }
                }
                playerList.next();
            }
            return over;
        }
        return true;
    }
    public Player getBestPlayer(){
        if(playerList!=null && !playerList.isEmpty()){
            playerList.toFirst();
            Player maxPlayer=playerList.getContent();
            while(playerList.hasAccess()){
                if(playerList.getContent().getPoints()>maxPlayer.getPoints()){
                    maxPlayer=playerList.getContent();
                }
            }

            return maxPlayer;
        }
        return null;
    }

    public void seperateInAndOutGamePlayers(int points){
        playerList.toFirst();
        while(playerList.hasAccess()){
            if(playerList.getContent().getPoints()==points){
                playerList.getContent().setInGame(true);
            }else{
                playerList.getContent().setInGame(false);
            }
            playerList.next();
        }
    }





}
