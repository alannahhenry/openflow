import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Broker extends MessengerAndReceiver {
    private ArrayList<Integer> portList = new ArrayList<Integer>();
    InetSocketAddress dstAddress;
    /*
     *
     */
    Broker(Terminal terminal, int port) {
        super(terminal, port);

    }

    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void onReceipt(DatagramPacket packet)  {
        try {
            super.onReceipt(packet);

            String content;
            byte[] data;
            byte[] buffer;
            DatagramPacket response;

            data = packet.getData();

            buffer= new byte[data[LENGTH_POS]];
            System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
            content= new String(buffer);

            switch(data[TYPE_POS]) {
                case TYPE_WORKCOMPLETE:
                    if(content.equalsIgnoreCase("y")||content.equalsIgnoreCase("yes")){
                        terminal.println("Worker "+ packet.getPort() + " has completed the work");
                    }
                    else if(content.equalsIgnoreCase("n")||content.equalsIgnoreCase("no")){
                        terminal.println("Worker "+ packet.getPort() + " has not completed the work");
                    }
                    break;

                case TYPE_WORKRESPONSE:
                    if(content.equalsIgnoreCase("y")||content.equalsIgnoreCase("yes")){
                        terminal.println("Worker "+ packet.getPort() + " has accepted the work");
                    }
                    else if(content.equalsIgnoreCase("n")||content.equalsIgnoreCase("no")){
                        for (int i = 0; i < portList.size(); i++) {
                            if(packet.getPort()==portList.get(i)){
                                portList.remove(i);
                            }

                        }
                        terminal.println("Worker "+ packet.getPort() + " has withdrew work availability");

                    }
                    break;

                case TYPE_ACK:
                    terminal.println("Received ack\n");
                    this.notify();
                    break;

                case TYPE_REGISTER: // registering a new worker
                    terminal.println("Received registration request from " + content);

                    int newPort = addWorker();

                    break;

                case TYPE_WORKDESC: // accepting a work description
                    String[] splitContent = content.split("-");
                    terminal.println("Work Description is: "+ splitContent[0]);
                    dstAddress = new InetSocketAddress(DEFAULT_DST_NODE, DEFAULT_CNC_PORT);
                    //sendAck(terminal, dstAddress );
                    forwardWorkDescription(terminal, packet.getData(), portList);

                    break;

                default:
                    terminal.println("Unexpected packet" + packet.toString());
            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    private synchronized int addWorker() throws Exception {

        int newPort = 50000;
        while(portList.contains(newPort)||newPort==DEFAULT_BROKER_PORT||newPort==DEFAULT_WORKER_PORT||newPort==DEFAULT_CNC_PORT){
            newPort++;
        }
        portList.add(newPort);
        dstAddress = new InetSocketAddress(DEFAULT_DST_NODE, DEFAULT_WORKER_PORT);
        //sendAck(terminal, dstAddress);
        sendNewPort(terminal, newPort, dstAddress);
        return newPort;
    }


    public synchronized void start() throws Exception {
        terminal.println("Waiting for contact");
        this.wait();
    }

    public synchronized void forwardWorkDescription(Terminal terminal, byte[] data, ArrayList<Integer> portList) throws IOException {


        byte[] buffer = new byte[data[LENGTH_POS]];
        System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
        String content = new String(buffer);
        String[] splitContent = content.split("-");
        int limit = -1;
        if (splitContent[1].equalsIgnoreCase("all")) {
            limit = portList.size();
        } else{
            limit = Integer.parseInt(splitContent[1]);
        }


        for (int i = 0; i < limit; i++) {
            terminal.println("Sending work description to port: "+portList.get(i));
            InetSocketAddress dstAddress= new InetSocketAddress(DEFAULT_DST_NODE, portList.get(i));

            buffer = splitContent[0].getBytes();
            byte[] passData = new byte[HEADER_LENGTH+buffer.length];
            passData[TYPE_POS] = TYPE_WORKDESC;
            passData[LENGTH_POS] = (byte)buffer.length;

            System.arraycopy(buffer, 0, passData, HEADER_LENGTH, buffer.length);

            terminal.println("Sending work description packet...");

            //send
            sendData(passData, dstAddress);


            terminal.println("Packet sent to port: "+portList.get(i));
        }

    }
    public synchronized void sendNewPort(Terminal  terminal, int newPort, InetSocketAddress dstAddress) throws IOException {

        byte[] data = null;
        byte[] buffer = null;
        DatagramPacket packet = null;
        String input = null;

        input = Integer.toString(newPort);
        buffer = input.getBytes();
        data = new byte[HEADER_LENGTH+buffer.length];

        data[TYPE_POS] = TYPE_NEWPORT;
        data[LENGTH_POS] = (byte)buffer.length;
        System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

        terminal.println("Sending new port " +newPort+" ...");
        sendData(data, dstAddress);
    }

    /*
     *
     */
    public static void main(String[] args) {
        try {
           Terminal terminal = new Terminal("Broker");
            while(true){
                (new Broker(terminal, DEFAULT_BROKER_PORT)).start();
            }

        } catch(Exception e) {e.printStackTrace();}
    }
}