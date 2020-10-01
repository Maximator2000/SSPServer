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

    /**
     *
     * @param pPort der Port des Servers
     */
    public SSPServer(int pPort) {
        super(pPort);
        playerList=new List<>();
        amountOfPlayers=0;
        roundIsOver=true;
        amountOfParticipators=2;
        System.out.println("Server wird erstellt");

    }

    /**
     * Speichert den Client in der Clientliste und fragt ihn nach dem Namen
     * @param pClientIP
     * @param pClientPort
     */
    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        playerList.append(new Player(pClientIP,pClientPort,true));
        askForName(pClientIP,pClientPort);
        System.out.println("Mit Server verbundnen "+pClientIP+" "+pClientPort);

    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        String[] messageParts = pMessage.split("\\$");
        if(messageParts[0].equals("name")){ //falls der NAme übergeben wird
            playerList.toFirst();
            while(playerList.hasAccess()){// gehe alle Spieler durch
                if(playerList.getContent().getName()==null) { // falls der NAme noch nicht festesteht
                    if (playerList.getContent().playerEquals(pClientIP, pClientPort)) {
                        playerList.getContent().setName(messageParts[1]);
                        amountOfPlayers++;//Anzahl der gemeldeten Spieler steigt
                        System.out.println("Spieler mit IP und Port"+pClientIP+" "+pClientPort+" heißt "+messageParts[1]);
                    }
                }
                playerList.next();
            }
            if(amountOfPlayers==AMOUNT && roundIsOver){
                System.out.println("-----Ab hier: START------");
                startRound();
            }
        } else if(messageParts[0].equals("spiele")){//falls es sich um Zug handel
            if(matches!=null && matches.length>0){
                for(int i=0;i<matches.length;i++){ // gehe alle vorhandenen Matches durch
                    if(matches[i]!=null && matches[i].contains(pClientIP,pClientPort)){
                        matches[i].setChoice(pClientIP,pClientPort,messageParts[1]);// setze den Zug für den Spieler
                        System.out.println("Spieler mit Ip und port"+pClientIP+" "+pClientPort+" wählt "+messageParts[1]);
                        if(matches[i].isFilled() && !matches[i].isWinnerKnown()) {//Falls im Match alle Züge bekannt sind, aber der gewiinner nicht
                            System.out.println(matches[i].getPlayer1().getName()+" : "+matches[i].getChoice1()+" vs. "
                                +matches[i].getPlayer2().getName()+" : "+matches[i].getChoice2());
                            if (matches[i].firstPlayerWins() ) {
                                matches[i].getPlayer1().addPoints(3);
                                matches[i].getPlayer2().addPoints(-1);
                                matches[i].setWinnerKnown(true);
                                System.out.println(matches[i].getPlayer1().getName()+" wins");
                                System.out.println(playedGames+" "+enemies.length);
                                //sende Punktzahl
                                send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer1().getPoints());
                                send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer2().getPoints());
                            }else if(matches[i].getChoice2().equals(matches[i].getChoice1())){ //falls es unentschieden steht
                                matches[i].getPlayer1().addPoints(1);
                                matches[i].getPlayer2().addPoints(1);
                                matches[i].setWinnerKnown(true);
                                send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer1().getPoints());
                                send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer2().getPoints());
                                System.out.println(playedGames+" "+enemies.length);



                            }else {
                                matches[i].getPlayer1().addPoints(-1);
                                matches[i].getPlayer2().addPoints(3);
                                matches[i].setWinnerKnown(true);
                                send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer1().getPoints());
                                send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),
                                        "punkte$"+matches[i].getPlayer2().getPoints());
                                System.out.println(matches[i].getPlayer2().getName()+" wins");
                                System.out.println("Spiel : "+playedGames+" Teilnehmer: "+enemies.length+" Spieler");


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

            }
        }

    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {
        if(enemies!=null && roundNumber>=enemies.length ){
            playerList.toFirst();
            while(playerList.hasAccess()){
                if(playerList.getContent().playerEquals(pClientIP,pClientPort)){
                    System.out.println(playerList.getContent().getName()+" hat das Spiel verlassen, weil er die Schönheit des Spieles nicht erkennt");
                    playerList.remove();
                }else{
                    playerList.next();
                }
            }
        }
    }

    /**
     * sendet eine Namesanfrage
     * @param pClientIP
     * @param pClientPort
     */
    public void askForName(String pClientIP,int pClientPort){
        send(pClientIP,pClientPort,"sende$name");
        System.out.println("Namensanfrage geschickt an "+pClientIP+" "+pClientPort);
    }

    /**
     * startet ein Turnier und setzt alle WERTE
     */
    public void startRound(){
        roundNumber=-1;
        if(roundIsOver) {
            playerList.toFirst();
            while (playerList.hasAccess()) {//Falls alle mit NAmen mitmachen sollen
                playerList.getContent().setPoints(0);
                playerList.next();
            }
            seperateInAndOutGamePlayers(0); //teilt die Spieler ein, die mitmachen dürfen
        }else{//Falls es zum SuddenDeath kommt
            seperateInAndOutGamePlayers(getBestPlayer(null).getPoints());
        }
        //erstelle ein Array mit allen Spielern, die im Spiel partiziieren
        enemies=new Player[amountOfPlayers];
        int i=0;
        playerList.toFirst();
        while(playerList.hasAccess()){
            if(playerList.getContent().isInGame()){
                enemies[i]=playerList.getContent();
                System.out.println(enemies[i].getName()+" macht mit");
                i++;
            }
            playerList.next();

        }
        playRounds();

    }

    /**
     * Eine Runde des Turniers.
     */
    public void playRounds(){
        roundIsOver=false;
        roundNumber++;
        playedGames=0;
        int matchNum=0;
        matches= new Match[Math.round((enemies.length)/2)];//Die Anzahl der MAtches wird festgelegt.
        // Bei ungerader Anzahl wird abgerundet
        if(youNeedToPlay()) {//solange es noch Spiele gibt
            //spielentstehung
            Player restPlayer = null;
            if(enemies.length>2){
                for (int i = 0; i < enemies.length; i++) {
                    int playerNum = i; // Für jeden Spieler
                    int enemyNum = - i + roundNumber;//wird ein Gegner ausgewählt
                    if (enemyNum < 0) {
                        enemyNum += enemies.length;
                    }
                    if(enemyNum>enemies.length-1){
                        enemyNum -= enemies.length;
                    }
                    //problem :so entstehn 4 matches!!!!!!!!!!!!!
                    if (playerNum != enemyNum) {//man darf nicht gegen sich selber Spielen
                        Player p1 = enemies[playerNum];
                        Player p2 = enemies[enemyNum];
                        //System.out.println(p1+" "+p2);
                        Match match = new Match(p1, p2);
                        if (matchNum == 0 || matchIstNichtVorhanden(match)) {//Zwei Matches dürfen nicht die
                            // selbern Spieler in gleicher reihenfolge haben
                            matches[matchNum] = match;
                            System.out.println(p1.getName()+" spielt gegen "+p2.getName());
                            matchNum++;
                        }
                    } else {
                        restPlayer = enemies[playerNum];
                    }
                }

            }else if(enemies.length==2){
                matches[0]=new Match(enemies[0],enemies[1]);
            }

            //spielvorgang
            System.out.println((roundNumber+1)+". Runde -------------------");
            for (int i = 0; i < matches.length; i++) {

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
            System.out.println("------ENDE------"+thereIsAWinner());
            if(thereIsAWinner()){
                System.out.println("Runde vorbei : "+getBestPlayer(null).getName()+ "hat gewonnen");
                playerList.toFirst();
                while(playerList.hasAccess()){
                    if(playerList.getContent().playerEquals(getBestPlayer(playerList.getContent()))){
                        send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),
                                "status$gewonnen");
                    }else{
                        send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),
                                "status$verloren");
                    }
                    playerList.next();
                }
                roundIsOver=true;
                startRound();

            }else{
                System.out.println("Runde vorbei : Es steht noch kein Sieger fest");
                startRound();
            }
        }
    }

    /**
     *
     * @param match
     * @return true, falls die Anordnung von match schon in matches vorhanden ist
     */

    public boolean matchIstNichtVorhanden(Match match){
        for( int i=0;i<matches.length;i++){
            if(matches[i]!=null) {
                if (match.matchEquals(matches[i].getPlayer1(), matches[i].getPlayer2())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     *
     * @return Angabe, ob noch Spiele nötog sind.
     */
    public boolean youNeedToPlay(){
        int num=enemies.length*((enemies.length-1)/2);
        if((num/matches.length)<roundNumber-1){
            return false;
        }
        return true;
    }
    /**
     *
     * @return true, falls es einen Spieler mit der meisten Punktzahl gibt
     */
    public boolean thereIsAWinner(){
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

    public Player getBestPlayer(Player lastPLayer){
        if(playerList!=null && !playerList.isEmpty()){
            playerList.toFirst();
            Player maxPlayer=playerList.getContent();
            while(playerList.hasAccess()){
                if(playerList.getContent().getPoints()>maxPlayer.getPoints()){
                    maxPlayer=playerList.getContent();
                }
                playerList.next();
            }
            if(lastPLayer!=null) {
                playerList.toFirst();
                boolean goOn = true;
                while (playerList.hasAccess() && goOn) {
                    if (playerList.getContent().playerEquals(lastPLayer)) {
                        goOn = false;
                    } else {
                        playerList.next();
                    }
                }
            }

            return maxPlayer;
        }
        return null;
    }
//Spieler, die keinen Namen haben oder nicht die geforderten Punkte haben werden ausgeschlossen
    public void seperateInAndOutGamePlayers(int points){
        playerList.toFirst();
        amountOfPlayers=0;
        while(playerList.hasAccess()){
            if(playerList.getContent().getPoints()==points && playerList.getContent().getName()!=null){
                playerList.getContent().setInGame(true);
                amountOfPlayers++;
            }else{
                playerList.getContent().setInGame(false);
            }
            playerList.next();
        }
    }





}
