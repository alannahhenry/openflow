import java.net.DatagramPacket;

public class GoBackNPacket {
    DatagramPacket packet;
    boolean isSent;

    GoBackNPacket(DatagramPacket packet){
        this.packet = packet;
        this.isSent = false;
    }




    public DatagramPacket getPacket() {
        return packet;
    }

    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }

    public void setIsSent(boolean sent) {
        isSent = sent;
    }

    public boolean getIsSent() {
        return isSent;
    }
}
