public class Player {

    private String name;
    private String pClientIP;
    private boolean inGame;
    private double timeToReact;
    private int points;
    private int pClientPort;

    public Player(String pClientIP,int pClientPort, boolean inGame){
        this.pClientIP=pClientIP;
        this.pClientPort=pClientPort;
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

    public void setName(String name) {
        this.name = name;
    }
}