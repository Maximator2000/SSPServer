import Datenstrukturen.List;
import com.sun.xml.internal.bind.v2.TODO;
import netz.Client;
import netz.Server;

import java.util.Timer;
import java.util.TimerTask;

public class SSPServer extends Server {

    private int amountOfPlayers;
    private int sizeOfMatch;
    private int roundNumber;
    private List<Player> playerList;
    private List <Player> waitingPlayers;
    private Player[] enemies;
    private Match[] matches;
    private int playedGames;
    private boolean roundIsOver;
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
        time=20;
        playerList=new List<>();
        waitingPlayers=new List<>();
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
        if(roundIsOver) {
            playerList.append(new Player(pClientIP, pClientPort, true));
            askForName(pClientIP, pClientPort);
            System.out.println("Mit Server verbundnen " + pClientIP + " " + pClientPort);
        }else{
            waitingPlayers.append(new Player(pClientIP, pClientPort, true));
            askForName(pClientIP, pClientPort);
            System.out.println("Mit Server verbundnen " + pClientIP + " " + pClientPort);
        }

    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        String[] messageParts = pMessage.split("\\$");
        if(messageParts[0].equals("name")){ //falls der Name übergeben wird
            boolean gefunden=false;
            playerList.toFirst();
            while(playerList.hasAccess()){// gehe alle Spieler durch
                if(playerList.getContent().getName()==null) { // falls der NAme noch nicht festesteht
                    if (playerList.getContent().playerEquals(pClientIP, pClientPort)) {
                        playerList.getContent().setName(messageParts[1]);
                        amountOfPlayers++;//Anzahl der gemeldeten Spieler steigt
                        System.out.println("Spieler mit IP und Port"+pClientIP+" "+pClientPort+" heißt "+messageParts[1]);
                        gefunden=true;
                    }
                }
                playerList.next();
            }
            if(gefunden){
                waitingPlayers.toFirst();
                while(waitingPlayers.hasAccess()){
                    if(waitingPlayers.getContent().getName()==null) { // falls der NAme noch nicht festesteht
                        if (waitingPlayers.getContent().playerEquals(pClientIP, pClientPort)) {
                            waitingPlayers.getContent().setName(messageParts[1]);
                            System.out.println("Spieler mit IP und Port"+pClientIP+" "+pClientPort+" heißt "+messageParts[1]);
                        }
                    }
                }
            }
            if(amountOfPlayers==2 && roundIsOver){
                timer=new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        sendToAll("zeit$"+(time));
                        time--;
                        System.out.println("Bis start "+time);
                        if(time==0){
                            timer.cancel();
                            timer.purge();
                            time=40;
                            System.out.println("-----Ab hier: START------");
                            startRound();
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
        }else if(messageParts[0].equals("weiter")){
            boolean suche=true;
            if(messageParts[1].equals("true")){
                playerList.toFirst();
                while (playerList.hasAccess() && suche){
                    if(playerList.getContent().playerEquals(pClientIP,pClientPort)){
                        System.out.println(playerList.getContent().getName()+" spielt weiter");
                        playerList.getContent().setPoints(0);
                        playerList.getContent().setInGame(true);
                        suche=false;
                    }
                    playerList.next();
                }
            }
            if(messageParts[1].equals("false")){
                playerList.toFirst();
                while (playerList.hasAccess() && suche){
                    if(playerList.getContent().playerEquals(pClientIP,pClientPort)){
                        suche=false;
                        playerList.remove();
                        System.out.println(playerList.getContent().getName()+" macht Schluss");
                    }
                    playerList.next();
                }
            }
        }

    }

    public void processRound(String pClientIP,int pClientPort, Match match,String message){
        if(match!=null && match.contains(pClientIP,pClientPort)){
            match.setChoice(pClientIP,pClientPort,message);// setze den Zug für den Spieler
            System.out.println("Spieler mit Ip und port"+pClientIP+" "+pClientPort+" wählt "+message);
            Player p= match.otherPlayer(pClientIP,pClientPort);
            //send(p.getpClientIP(),p.getpClientPort(),"gegner$name$")
            if(match.isFilled() && !match.isWinnerKnown()) {//Falls im Match alle Züge bekannt sind, aber der gewiinner nicht
                System.out.println(match.getPlayer1().getName()+" : "+match.getChoice1()+" vs. "
                        +match.getPlayer2().getName()+" : "+match.getChoice2());
                if (match.firstPlayerWins() ) {
                    match.getPlayer1().addPoints(3);
                    match.getPlayer2().addPoints(-1);
                    match.setWinnerKnown(true);
                    send(match.getPlayer1().getpClientIP(),match.getPlayer1().getpClientPort(),"gegner$auswahl$"+match.getChoice2());
                    send(match.getPlayer1().getpClientIP(),match.getPlayer1().getpClientPort(),"gegner$auswahl$"+match.getChoice1());
                    send(match.getPlayer1().getpClientIP(),match.getPlayer1().getpClientPort(),"status$ausgang$gewonnen");
                    send(match.getPlayer2().getpClientIP(),match.getPlayer2().getpClientPort(),"status$ausgang$verloren");
                    System.out.println(match.getPlayer1().getName()+" wins");
                    System.out.println(playedGames+" "+enemies.length);
                }else if(match.getChoice2().equals(match.getChoice1())){ //falls es unentschieden steht
                    match.getPlayer1().addPoints(1);
                    match.getPlayer2().addPoints(1);
                    send(match.getPlayer1().getpClientIP(),match.getPlayer1().getpClientPort(),"status$ausgang$unentschieden");
                    send(match.getPlayer2().getpClientIP(),match.getPlayer2().getpClientPort(),"status$ausgang$unentschieden");
                    match.setWinnerKnown(true);
                }else {
                    match.getPlayer1().addPoints(-1);
                    match.getPlayer2().addPoints(3);
                    match.setWinnerKnown(true);
                    send(match.getPlayer1().getpClientIP(),match.getPlayer1().getpClientPort(),"status$ausgang$verloren");
                    send(match.getPlayer2().getpClientIP(),match.getPlayer2().getpClientPort(),"status$ausgang$gewonnen");
                    System.out.println(match.getPlayer2().getName()+" wins");
                    System.out.println("Spiel : "+playedGames+" Teilnehmer: "+enemies.length+" Spieler");
                }

                playedGames++;
                if(playedGames==matches.length){
                    timer.cancel();
                    timer.purge();
                    playedGames=0;
                    Timer interval= new Timer();
                    time=5;
                    interval.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            System.out.println("zeit bis ende pause"+time);
                            sendToAll("zeit$"+time);
                            time--;
                            if(time==0){
                                time=30;
                                System.out.println("");
                                uebergebeZwischenstand();
                                playRounds();
                                interval.cancel();
                                interval.purge();
                            }
                        }
                    },delay,period);

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
        if(roundIsOver) {
            waitingPlayers.toFirst();
            while (waitingPlayers.hasAccess()) {
                playerList.append(waitingPlayers.getContent());
            }
        }
        roundNumber=-1;
        if(roundIsOver) {
            seperateInAndOutGamePlayers(0); //teilt die Spieler ein, die mitmachen dürfen
        }else{//Falls es zum SuddenDeath kommt
            seperateInAndOutGamePlayers(getBestPlayer(null).getPoints());
        }
        if(amountOfPlayers>=2) {
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
                        time=40;
                        System.out.println("Zeit um");
                        timer.cancel();
                        timer.purge();
                        for(int i=0;i<matches.length;i++){
                            if(matches[i]!=null) {
                                if (!matches[i].isFilled()) {
                                    if (matches[i].getChoice1() == null) {
                                        if(isConnectedTo(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort())){
                                            send(matches[i].getPlayer1().getpClientIP(),matches[i].getPlayer1().getpClientPort(),"status$rausgeworfen$Du hast keine Auswahl gesendet ! Du Feigling");
                                        }
                                        closeConnection(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort());
                                        processRound(matches[i].getPlayer1().getpClientIP(), matches[i].getPlayer1().getpClientPort(),
                                                matches[i], matches[i].getPlayer1().giveRandomChoice());
                                    }
                                    if (matches[i].getChoice2() == null) {
                                        if(isConnectedTo(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort())){
                                            send(matches[i].getPlayer2().getpClientIP(),matches[i].getPlayer2().getpClientPort(),"status$rausgeworfen$Du hast keine Auswahl gesendet ! Du Feiging");
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
                    send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),
                                "status$spielende");
                    playerList.next();
                }
                roundIsOver=true;
                processEnd();

            }else{
                System.out.println("Runde vorbei : Es steht noch kein Sieger fest");
                startRound();
            }
        }
    }

    public void processEnd(){
        timer.purge();
        timer.cancel();
        sendToAll("sende$weiterMachen");
        playerList.toFirst();
        while (playerList.hasAccess()){
            playerList.getContent().setInGame(false);
            playerList.next();
        }
        Timer tBreak=new Timer();
        time=20;
        tBreak.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println(time);
                time--;
                if(time==0){
                    startRound();
                    time=30;
                    playerList.toFirst();
                    while (playerList.hasAccess()){
                        if(!playerList.getContent().isInGame()){
                            send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),"status$rausgeworfen$Du hast vergessen, dich höflich zu verabschieden!");
                            playerList.remove();
                        }else {
                            playerList.next();
                        }
                    }
                    tBreak.cancel();
                    tBreak.purge();
                }
            }
        },delay,period);
    }
//10.17.130.45 Joshuas PC
    public void uebergebeZwischenstand(){
        int size;
        if(enemies==null){
            size=amountOfPlayers;
        }else{
            size=enemies.length;

        }
        String nachricht="punkte";
        playerList.toFirst();
        Player[] sortedPlayers= new Player[size];
        playerList.toFirst();
        for(int i=0;i<size;i++){
            if(playerList.getContent()!=null){
               sortedPlayers[i]=playerList.getContent();
               playerList.next();
            }
        }
        for(int i=0;i<sortedPlayers.length-1;i++){
            int index=i;
            for(int j=i+1;j<sortedPlayers.length;j++){
                if(sortedPlayers[j].getPoints()>sortedPlayers[index].getPoints()){
                    index=j;
                }
            }
            Player tmp=sortedPlayers[index];
            sortedPlayers[index]=sortedPlayers[i];
            sortedPlayers[i]=tmp;
        }

        /*List <Player> tmp=playerList;
        for(int i=0;i<size;i++) {
            int max=-1;
            Player maxPlayer=null;
            while (tmp.hasAccess()) {
                if (tmp.getContent() != null) {
                    if (tmp.getContent().getPoints() > max) {
                        maxPlayer = tmp.getContent();
                        max = tmp.getContent().getPoints();
                    }
                }
            }
            while (tmp.hasAccess()) {
                if (tmp.getContent() != null) {
                    if (tmp.getContent().playerEquals(maxPlayer)) {
                        tmp.remove();
                    }else{
                        tmp.next();
                    }
                }
            }
            sortedPlayers[i]=maxPlayer;
        }
                /*boolean weiter = true;
                for (int i = 0; i < sortedPlayers.length && weiter; i++) {
                    if (sortedPlayers[i] == null || sortedPlayers[i].getPoints() < playerList.getContent().getPoints()) {
                        sortedPlayers[i] = playerList.getContent();
                        weiter = false;
                    }
                }
            }
            playerList.next();
        }*/
        for(Player p:sortedPlayers){
            System.out.println(p);
            if(p!=null) {
                System.out.println(p.getName());
                nachricht = nachricht + "$" + p.getName() + "$" + p.getPoints();
            }else{
                System.out.println(p);
            }
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
                        System.out.println("amountOfP"+amountOfPlayers);
                        playerList.next();
                    }

                } else {
                    if(playerList.getContent()!=null) {
                        playerList.getContent().setInGame(false);
                        send(playerList.getContent().getpClientIP(), playerList.getContent().getpClientPort(), "status$aussetzen");
                        playerList.next();
                    }
                }

            }else{
                send(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort(),"status$rausgeworfen$Du hast deinen Namen nicht gesagt! Dummkopf!");
                closeConnection(playerList.getContent().getpClientIP(),playerList.getContent().getpClientPort());
                System.out.println("Spieler mit IP "+playerList.getContent().getpClientIP()+" und Port "+playerList.getContent().getpClientPort()+" wurde rausgeworfen! Er/Sie/Es war unfähig, den eigenen Namen zu nennen");
                playerList.remove();
            }

        }
    }





}
