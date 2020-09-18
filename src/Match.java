public class Match {

    private Player player1,player2;

    public Match(Player player1,Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public void compareChoices(){

    }
    public boolean equals(Player player1,Player player2){
        if(this.player1==player1&&this.player2==player2){
            return true;
        }
        return false;
    }
}
