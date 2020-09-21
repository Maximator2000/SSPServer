import Datenstrukturen.List;
import com.sun.xml.internal.bind.v2.TODO;
import netz.Client;
import netz.Server;

public class SSPServer extends Server {

    private int amountOfPlayers;
    private int roundNumber;
    public static final int AMOUNT=2;
    private List<Player> playerList;
    private Player[] enemies;
    private Match[] matches;
    private int playedGames;
    private boolean roundIsOver;
    private int amountOfParticipators;


    public SSPServer(int pPort) {
        super(pPort);
        playerList=new List<>();
        amountOfPlayers=0;
        roundIsOver=true;
        amountOfParticipators=2;
        System.out.println("Server wird erstellt");

    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        playerList.append(new Player(pClientIP,pClientPort,true));
        askForName(pClientIP,pClientPort);
        System.out.println("Mit Server verbundnen "+pClientIP+" "+pClientPort);

    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        String[] messageParts = pMessage.split("\\$");
        if(messageParts[0].equals("name")){
            boolean allNames=true;// alle Namen werden als vorhanden angenommen
            playerList.toFirst();
            while(playerList.hasAccess()){
                if(playerList.getContent().getName()==null) {
                    if (playerList.getContent().playerEquals(pClientIP, pClientPort)) {
                        playerList.getContent().setName(messageParts[1]);
                        amountOfPlayers++;
                        System.out.println("Spieler mit IP und Port"+pClientIP+" "+pClientPort+" heißt"+messageParts[1]);
                    }else{
                        allNames=false; // falls ein Name nicht vorhanden ist und nicht eingesetzt wurde
                    }
                }
                playerList.next();
            }
            if(amountOfPlayers==AMOUNT && allNames){
                System.out.println("Erste runde gestartet");
                startRound();
            }
        } else if(messageParts[0].equals("spiele")){
            if(matches!=null && matches.length>0){
                for(int i=0;i<=matches.length;i++){
                    if(matches[i].contains(pClientIP,pClientPort)){
                        matches[i].setChoice(pClientIP,pClientPort,messageParts[1]);
                        System.out.println("Spieler mit Ip und port"+pClientIP+" "+pClientPort+" wählt"+messageParts[1]);
                        if(matches[i].isFilled() && !matches[i].isWinnerKnown()) {
                            System.out.println(matches[i].getPlayer1().getName()+" : "+matches[i].getChoice1()+" vs. "
                                +matches[i].getPlayer2().getName()+" : "+matches[i].getChoice2());
                            if (matches[i].firstPlayerWins() ) {
                                matches[i].getPlayer1().addPoints(3);
                                matches[i].getPlayer2().addPoints(-1);
                                matches[i].setWinnerKnown(true);
                                System.out.println(matches[i].getPlayer1().getName()+" wins");
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
                                System.out.println(matches[i].getPlayer2().getName()+" wins");


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
                            if(playedGames==enemies.length){
                                playedGames=0;
                                playRounds();
                            }

                        }
                    }
                }

            }
        }

    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {

    }

    public void askForName(String pClientIP,int pClientPort){
        send(pClientIP,pClientPort,"sende$name");
        System.out.println("Namensanfrage geschickt an "+pClientIP+" "+pClientPort);
    }

    public void startRound(){
        roundNumber=-1;
        enemies=new Player[amountOfPlayers];
        playerList.toFirst();
        int i=0;
        while(playerList.hasAccess()){
            if(playerList.getContent().isInGame()){
                enemies[i]=playerList.getContent();
            }
            playerList.next();
            i++;
        }
        playRounds();

    }

    public void playRounds(){
        roundIsOver=false;
        roundNumber++;
        playedGames=0;
        int matchNum=0;
        matches= new Match[Math.round((enemies.length-1)/2)];
        if(roundNumber<enemies.length) {
            //spielentstehung
            Player restPlayer = null;
            if(enemies.length>2){
                for (int i = 0; i < enemies.length; i++) {
                    int playerNum = i;
                    int enemyNum = 2 - i + roundNumber;
                    if (enemyNum < 0) {
                        enemyNum += enemies.length;
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
            }else if(enemies.length==2){
                //ToDO
                matches[0]=new Match(enemies[0],enemies[1]);
            }

            //spielvorgang
            System.out.println(roundIsOver+". Runde -------------------");
            for (int i = 0; i <= matches.length; i++) {
                System.out.println(matches[i].getPlayer1().getName()+" vs. "+matches[i].getPlayer2().getName());
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
                System.out.println(restPlayer.getName()+" setzt aus .(");
                send(restPlayer.getpClientIP(), restPlayer.getpClientPort(), "status$aussetzen");
            }
            if(matches.length==0){
                playRounds();
            }
        }else{//Jeder hat gegen jeden gespielt.
            if(roundIsOver){
                System.out.println("Runde vorbei : "+getBestPlayer().getName()+ "hat gewonnen");
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
                playerList.toFirst();
                while(playerList.hasAccess()){
                    playerList.getContent().setInGame(true);
                    playerList.next();
                }
                startRound();

            }else{
                System.out.println("Runde vorbei : Es steht noch kein Sieger fest");
                seperateInAndOutGamePlayers(getBestPlayer().getPoints());
                startRound();
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
