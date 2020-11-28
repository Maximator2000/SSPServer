import Datenstrukturen.List;
import com.sun.xml.internal.bind.v2.TODO;
import netz.Client;
import netz.Server;

import java.util.Timer;
import java.util.TimerTask;

public class SSPServer extends Server {

    private int amountOfPlayers;
    private int roundNumber;
    private List<Player> playerList;
    private Player[] enemies;
    private Match[] matches;
    private int playedGames;
    private boolean roundIsOver;
    private int amountOfParticipators;
    private Timer timer;
    private int delay,period,time;

    /**
     *
     * @param pPort der Port des Servers
     */
    public SSPServer(int pPort) {
        super(pPort);
        timer=new Timer();
        delay=1000;
        period=1000;
        time=30;
        playerList=new List<>();
        amountOfPlayers=0;
        roundIsOver=true;
        //amountOfParticipators=4;
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
        if(messageParts[0].equals("name")){ //falls der Name übergeben wird
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
            if(amountOfPlayers==2){
                timer=new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        sendToAll("zeit$"+(time));
                        time--;
                        System.out.println("Bis start "+time);
                        if(time==0){
                            timer.cancel();
                            if(roundIsOver){
                                time=20;
                                System.out.println("-----Ab hier: START------");
                                startRound();
                            }
                        }
                    }
                }, delay, period);
            }
        } else if(messageParts[0].equals("spiele")){//falls es sich um Zug handel
            if(matches!=null && matches.length>0){
                for(Match i : matches){ // gehe alle vorhandenen Matches durch
                    processRound(pClientIP,pClientPort,i,messageParts[1]);
                }

            }
        }

    }

    public void processRound(String pClientIP,int pClientPort, Match match,String message){
        if(match!=null && match.contains(pClientIP,pClientPort)){
            match.setChoice(pClientIP,pClientPort,message);// setze den Zug für den Spieler
            System.out.println("Spieler mit Ip und port"+pClientIP+" "+pClientPort+" wählt "+message);
            if(match.otherPlayer(pClientIP,pClientPort)!=null){
                send(match.otherPlayer(pClientIP,pClientPort).getpClientIP(),
                        match.otherPlayer(pClientIP,pClientPort).getpClientPort(),
                        "gegner$auswahl$"+message);
            }
            if(match.isFilled() && !match.isWinnerKnown()) {//Falls im Match alle Züge bekannt sind, aber der gewiinner nicht
                System.out.println(match.getPlayer1().getName()+" : "+match.getChoice1()+" vs. "
                        +match.getPlayer2().getName()+" : "+match.getChoice2());
                if (match.firstPlayerWins() ) {
                    match.getPlayer1().addPoints(3);
                    match.getPlayer2().addPoints(-1);
                    match.setWinnerKnown(true);
                    System.out.println(match.getPlayer1().getName()+" wins");
                    System.out.println(playedGames+" "+enemies.length);
                }else if(match.getChoice2().equals(match.getChoice1())){ //falls es unentschieden steht
                    match.getPlayer1().addPoints(1);
                    match.getPlayer2().addPoints(1);
                    match.setWinnerKnown(true);
                }else {
                    match.getPlayer1().addPoints(-1);
                    match.getPlayer2().addPoints(3);
                    match.setWinnerKnown(true);
                    System.out.println(match.getPlayer2().getName()+" wins");
                    System.out.println("Spiel : "+playedGames+" Teilnehmer: "+enemies.length+" Spieler");
                }
                //sende Gegnerwahl

                playedGames++;
                if(playedGames==matches.length){
                    playedGames=0;
                    playRounds();
                }

            }
        }
    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {
        if(!playerList.isEmpty()) {
            Player lastOne = playerList.getContent();
            playerList.toFirst();
            while (playerList.hasAccess()) {
                if (playerList.getContent().playerEquals(pClientIP, pClientPort)) {
                    playerList.getContent().setcI(true);
                }
                playerList.next();
            }
            playerList.toFirst();
            if (lastOne != null) {
                while (playerList.hasAccess() && !playerList.getContent().playerEquals(lastOne)) {
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
        if(amountOfPlayers>1) {
            //alle Spieler in playerList sollten jetzt mitmachen
            uebergebeZwischenstand();
            //erstelle ein Array mit allen Spielern, die im Spiel partiziieren
            enemies = new Player[amountOfPlayers];
            int i = 0;
            playerList.toFirst();
            while (playerList.hasAccess()) {
                if (playerList.getContent().isInGame()) {
                    enemies[i] = playerList.getContent();
                    System.out.println(enemies[i].getName() + " macht mit");
                    i++;
                }
                playerList.next();

            }
            System.out.println("Anzahl der Spieler: " + enemies.length);
            playRounds();
        }

    }

    /**
     * Eine Runde des Turniers.
     */
    public void playRounds(){
        roundIsOver=false;
        roundNumber++;
        playedGames=0;
        int matchNum=0;
        time=20;
        matches= new Match[Math.round((enemies.length)/2)];//Die Anzahl der MAtches wird festgelegt.
        // Bei ungerader Anzahl wird abgerundet
        if(youNeedToPlay()) {//solange es noch Spiele gibt
            //spielentstehung
            Player restPlayer = null;
            if(enemies.length>2){
                for (int i = 0; i < enemies.length; i++) {
                    int playerNum=i; // Für jeden Spieler
                    int enemyNum=0;
                    if(enemies.length%2==1) {
                        enemyNum = -i + roundNumber;//wird ein Gegner ausgewählt
                        if (enemyNum < 0) {
                            enemyNum += enemies.length;
                        }
                        if (enemyNum > enemies.length - 1) {
                            enemyNum -= enemies.length;
                        }
                    }else if(enemies.length%2==0){
                        enemyNum=i+roundNumber+1;
                        if(enemyNum > enemies.length - 1) {
                            enemyNum -= enemies.length;
                        }
                    }
                    if (playerNum != enemyNum) {//man darf nicht gegen sich selber Spielen
                        Player p1 = enemies[playerNum];
                        Player p2 = enemies[enemyNum];
                        //System.out.println(p1+" "+p2);
                        Match match = new Match(p1, p2);
                        if (matchNum == 0 || matchDieRichitgenSpielerBeansprucht(match)) {//Ein AMtch darf nicht einen zugeordnetetn Speiler benutzen
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
            timer=new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendToAll("zeit$"+(time));
                    time--;
                    System.out.println("zeit$"+(time+1));
                    if(time==0){
                        time=20;
                        System.out.println("Zeit um");
                        timer.cancel();
                        for(int i=0;i<matches.length;i++){
                            if(matches[i]!=null) {
                                if (!matches[i].isFilled()) {
                                    if (matches[i].getChoice1() == null) {
                                        if(isConnectedTo(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort())){
                                            send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),"status$rausgeworfen$Du hast keine Auswahl gesendet !");
                                        }
                                        closeConnection(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort());
                                        processRound(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort(),
                                                matches[i], matches[i].getPlayer1().giveRandomChoice());
                                    }
                                    if (matches[i].getChoice2() == null) {
                                        if(isConnectedTo(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort())){
                                            send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),"status$rausgeworfen$Du hast keine Auswahl gesendet !");
                                        }
                                        closeConnection(matches[i].getPlayer2().getpClientIP(), matches[i].getPlayer2().getpClientPort());
                                        processRound(matches[i].getPlayer2().getpClientIP(), matches[i].getPlayer2().getpClientPort(),
                                                matches[i], matches[i].getPlayer2().giveRandomChoice());
                                    }

                                }
                            }
                        }
                    }
                }
            }, delay, period);
            System.out.println((roundNumber+1)+". Runde ------------------- :"+youNeedToPlay());
           /* for (int i = 0; i < matches.length; i++) {
                //System.out.println(matches[i]);
                //System.out.println(matches[i].getPlayer1());
                //System.out.println(matches[i].getPlayer1().getName());
               //System.out.println(matches[i].getPlayer1().getName()+" vs. "+matches[i].getPlayer2().getName());
                send(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort(),
                        "gegner$name$" + matches[i].getPlayer2().getName());
                send(matches[i].getPlayer2().getpClientIP(), matches[i].getPlayer2().getpClientPort(),
                        "gegner$name$" + matches[i].getPlayer1().getName());
                send(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort(),
                        "sende$möglichkeiten");
                send(matches[i].getPlayer2().getpClientIP(), matches[i].getPlayer2().getpClientPort(),
                        "sende$möglichkeiten");

            }*/
            for(Player player: enemies){
                for (Match match : matches) {
                    if (match.contains(player.getpClientIP(), player.getpClientPort())) {
                        if(player.iscI()) {
                            processRound(player.getpClientIP(), player.getpClientPort(), match, player.giveRandomChoice());
                        }else{
                            send(player.getpClientIP(), player.getpClientPort(),
                                    "gegner$name$" + match.otherPlayer(player.getpClientIP(),player.getpClientPort()).getName());
                            send(player.getpClientIP(), player.getpClientPort(),
                                    "sende$auswahl");
                        }
                    }

                }
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
            uebergebeZwischenstand();
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
    public void uebergebeZwischenstand(){
        String nachricht="punkte";
        playerList.toFirst();
        while (playerList.hasAccess()){
            nachricht+="$"+playerList.getContent().getName()+"$"+playerList.getContent().getPoints();
            playerList.next();
        }
        System.out.println("An alle wird gesendet : "+nachricht);
        sendToAll(nachricht);

    }
    /**
     *
     * @param match
     * @return true, falls die Anordnung von match schon in matches vorhanden ist
     */

    /*public boolean matchIstNichtVorhanden(Match match){
        for( int i=0;i<matches.length;i++){
            if(matches[i]!=null) {
                if (match.matchEquals(matches[i].getPlayer1(), matches[i].getPlayer2())) {
                    return false;
                }
            }
        }
        return true;
    }*/

    public boolean matchDieRichitgenSpielerBeansprucht(Match match){
        for( int i=0;i<matches.length;i++){
            if(matches[i]!=null) {
                if(match.getPlayer1().playerEquals(matches[i].getPlayer1())){
                    return false;
                } else if (match.getPlayer1().playerEquals(matches[i].getPlayer2())) {
                    return false;
                }else if(match.getPlayer2().playerEquals(matches[i].getPlayer1())){
                    return false;
                }else if(match.getPlayer2().playerEquals(matches[i].getPlayer2())){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     *
     * @return Angabe, ob noch Spiele nötig sind.
     */
    public boolean youNeedToPlay(){
        double num=enemies.length*(enemies.length-1)/2;
        if((num/matches.length)==roundNumber){
            System.out.println("roundNumber: "+roundNumber+" num : "+num+" matches "+matches.length);
            return false;
        }
        System.out.println("roundNumber: "+roundNumber+" num : "+num+" matches "+matches.length);

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
                if(playerList.getContent().isInGame() && !playerList.getContent().iscI()){// falls der Spieler am Spiel beteildigt ist
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
                if(playerList.getContent().getPoints()>maxPlayer.getPoints() && !playerList.getContent().iscI()){
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
            if(playerList.getContent().getName()!=null) {
                if (playerList.getContent().getPoints() == points) {
                    if(playerList.getContent().iscI()){
                        System.out.println(playerList.getContent().getName()+" wurde bseitigt, da er seinen eigenen Charackter nicht steuern konnte ...\n selbs die KI kann es besser");
                        playerList.remove();
                    }else{
                        playerList.getContent().setInGame(true);
                        amountOfPlayers++;
                        playerList.next();
                    }

                } else {
                    playerList.getContent().setInGame(false);
                    playerList.next();
                    send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),"status$aussetzen");
                }

            }else{
                send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),"status$rausgeworfen$Du hast deinen NAmen nicht gesagt! Dummkopf!");
                closeConnection(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort());
                System.out.println("Spieler mit IP "+playerList.getContent().getpClientIP()+" und Port "+playerList.getContent().getpClientPort()+" wurde rausgeworfen! Er/Sie/Es war unfähig, den eigenen Namen zu nennen");
                playerList.remove();
            }

        }
    }





}
