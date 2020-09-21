public class Player {

    private String name;
    private String pClientIP;
    private boolean inGame;
    private double timeToReact;
    private int points;
    private String choices;
    private int pClientPort;

    public Player(String pClientIP,int pClientPort, boolean inGame){
        this.pClientIP=pClientIP;
        this.pClientPort=pClientPort;
        this.inGame=inGame;
        points=0;
    }

    public boolean playerEquals(String pClientIP,int pClientPort){
        if(pClientIP.equals(this.pClientIP)&& pClientPort==this.pClientPort){
            return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public String getpClientIP() {
        return pClientIP;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public int getpClientPort() {
        return pClientPort;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addPoints(int points){
        this.points+=points;
    }
}
