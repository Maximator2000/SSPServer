public class Match {

    private Player player1,player2;
    private String choice1,choice2;

    public Match(Player player1,Player player2) {
        this.player1 = player1;
        this.player2 = player2;
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

    public boolean matchEquals(Player player1,Player player2){
        if(this.player1==player1&&this.player2==player2){
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
    public void setChoice(String pClientIp,int pClientPort,String choice){
        if(choice!=null &&(choice.equals("A")||choice.equals("B")||choice.equals("C")
                ||choice.equals("D")||choice.equals("E"))){
            if(player1.getpClientIP().equals(pClientIp)&&player1.getpClientPort()==pClientPort){
                choice1=choice;
            }else if(player2.getpClientIP().equals(pClientIp)&&player2.getpClientPort()==pClientPort){
                choice2=choice;
            }
        }
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
}
