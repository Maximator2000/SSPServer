/**
 * Stellt ein Duell zwischen zwei Spielern dar
 */
public class Match {

    private Player player1,player2;
    private String choice1,choice2;
    private boolean validation1,validation2,winnerKnown;

    public Match(Player player1,Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        validation1=true;
        validation2=true;
    }


    public boolean firstPlayerWins(){
        int c1,c2;
        c1=0;
        c2=0;
        if(choice1.equals("A")){
            c1=1;
        }
        if(choice1.equals("B")){
            c1=2;
        }
        if(choice1.equals("C")){
            c1=3;
        }
        if(choice1.equals("D")){
            c1=4;
        }
        if(choice1.equals("E")){
            c1=5;
        }
        if(choice2.equals("A")){
            c2=1;
        }
        if(choice2.equals("B")){
            c2=2;
        }
        if(choice2.equals("C")){
            c2=3;
        }
        if(choice2.equals("D")){
            c2=4;
        }
        if(choice2.equals("E")){
            c2=5;
        }
        int c1win1,c1win2;
        c1win1=c1+1;
        c1win2=c1+3;
        if(c1win1>5){
            c1win1-=5;
        }
        if(c1win2>5){
            c1win2-=5;
        }
        if(c2==c1win1||c2==c1win2){
            return true;
        }
        return false;
    }

    /**
     *
     * @param player1
     * @param player2
     * @return Falls die selben Spieler antreten, true
     */
    public boolean matchEquals(Player player1,Player player2){
        if((this.player1==player1&&this.player2==player2)||(this.player1==player2&&this.player2==player1)){
            return true;
        }
        return false;
    }
    public boolean contains(String pClientIp,int pClientPort){
        if((player1.getpClientIP().equals(pClientIp)&&player1.getpClientPort()==pClientPort)
                ||player2.getpClientIP().equals(pClientIp)&&player2.getpClientPort()==pClientPort){
            return true;
        }
        return false;
    }

    /**
     * sucht den passenden Spierl und setzt die jeweilige Entscheidung.
     * @param pClientIp
     * @param pClientPort
     * @param choice
     */
    public void setChoice(String pClientIp,int pClientPort,String choice){
        if(choice!=null &&(choice.equals("A")||choice.equals("B")||choice.equals("C")
                ||choice.equals("D")||choice.equals("E"))){
            if(player1.getpClientIP().equals(pClientIp)
                    &&player1.getpClientPort()==pClientPort
                    &&validation1){
                choice1=choice;
                validation1=false;
                System.out.println("Die Auswahl von "+player1.getName()+" wurde bestätigt");
            }else if(player2.getpClientIP().equals(pClientIp)
                    &&player2.getpClientPort()==pClientPort
                    &&validation2){
                choice2=choice;
                validation2=false;
                System.out.println("Die Auswahl von "+player2.getName()+" wurde bestätigt");

            }
        }
    }

    /**
     *
     * @return true, falls alle Entscheidungen getroffen wurden
     */
    public boolean isFilled(){
        if(choice1!=null && choice2!=null){
            return true;
        }
        return false;
    }


    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public String getChoice1() {
        return choice1;
    }

    public void setChoice1(String choice1) {
        this.choice1 = choice1;
    }

    public String getChoice2() {
        return choice2;
    }

    public void setChoice2(String choice2) {
        this.choice2 = choice2;
    }

    public boolean isWinnerKnown() {
        return winnerKnown;
    }

    public void setWinnerKnown(boolean winnerKnown) {
        this.winnerKnown = winnerKnown;
    }
}
