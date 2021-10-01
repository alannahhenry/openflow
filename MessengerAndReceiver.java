import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MessengerAndReceiver extends Node {
    static final String DEFAULT_DST_NODE = "localhost";	// Name of the host for the server
    static final int HEADER_LENGTH = 3; // Length of the header of the packet

    static final int DEFAULT_WORKER_PORT = 50000;
    public static final int DEFAULT_BROKER_PORT= 50001;
    static final int DEFAULT_CNC_PORT = 50002;


    static final int TYPE_POS = 0; // Position of the type within the header
    static final int LENGTH_POS = 1; // Position of the length of the buffer within the header
    static final int ID_POS= 2;

    static final byte TYPE_UNKNOWN = 0;
    static final byte TYPE_ACK = 1;
    static final byte TYPE_REGISTER = 2;
    static final byte TYPE_WORKDESC = 3;
    static final byte TYPE_WORKREFUSAL = 4;
    static final byte TYPE_WORKCOMPLETE = 5;
    static final byte TYPE_NEWPORT = 6;
    static final byte TYPE_WORKRESPONSE= 7;

    public Terminal terminal;
    private int packetId=1;

    Timer timer;

    MessengerAndReceiver(Terminal terminal, int port) {
        this.terminal= terminal;

        try {
            socket= new DatagramSocket(port);
            listener.go();
        }
        catch(Exception e) {e.printStackTrace();}
    }


    ArrayList<GoBackNPacket> packetsPending =  new ArrayList<>();


    public synchronized void sendAck (Terminal terminal, InetSocketAddress dstAddress, int ID) throws IOException {
        byte[] data = null;
        DatagramPacket packet = null;

        data = new byte[HEADER_LENGTH];
        data[TYPE_POS] = TYPE_ACK;
        data[LENGTH_POS] = 0;

        packet= new DatagramPacket(data, data.length);
        packet.setSocketAddress(dstAddress);
        socket.send(packet);
    }



    int packetsInTransit = 0;

    public synchronized void processBuffer() throws IOException {
        for (int i = 0; i <packetsPending.size() ; i++) {
            if (!packetsPending.get(i).getIsSent()) {

                if (packetsInTransit <= 5) {
                    DatagramPacket p = null;
                    GoBackNPacket n = packetsPending.get(i);
                    p=n.getPacket();
                    socket.send(p);
                    packetsPending.get(i).setIsSent(true);
                    long delay = 300000L;
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                handleTimeout();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, delay);
                    packetsInTransit++;

                }
            }
        }


    }

    private void handleTimeout() throws IOException {
        for (int i = 0; i < packetsPending.size(); i++) {
            socket.send(packetsPending.get(i).getPacket());
        }
    }

    public synchronized void sendData(byte[] data, InetSocketAddress destinationAddress) throws IOException {
        data[ID_POS] = (byte)packetId;
        packetId++;

        DatagramPacket packet= new DatagramPacket(data, data.length);

        packet.setSocketAddress(destinationAddress);
        packetsPending.add(new GoBackNPacket(packet));
        processBuffer();
        //socket.send(packet);
    }

    // RECEIVING FUNCTION
    public  void onReceipt(DatagramPacket packet) throws IOException, InterruptedException{
        if(packet.getData()[TYPE_POS]!=TYPE_ACK) {
            int ID = packet.getData()[ID_POS];
            sendAck(terminal, new InetSocketAddress(DEFAULT_DST_NODE, packet.getPort()), ID);
        }
        else {
            int packetId = packet.getData()[ID_POS];
            if(packetId==packetsPending.get(0).getPacket().getData()[ID_POS]){
                for (int i = 0; i < packetsPending.size(); i++) {
                    if(packetId==packetsPending.get(i).getPacket().getData()[ID_POS]){
                        packetsPending.remove(i);
                        packetsInTransit--;
                        if (timer!=null){
                            timer.cancel();
                        }
                    }
                }
                processBuffer();
            }
        }


    }




}
