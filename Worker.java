import javax.swing.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.SocketException;

/**
 *
 * Client class
 *
 * An instance accepts input from the user, marshalls this into a datagram, sends
 * it to a server instance and then waits for a reply. When a packet has been
 * received, the type of the packet is checked and if it is an acknowledgement,
 * a message is being printed and the waiting main method is being notified.
 *
 */
public class Worker extends MessengerAndReceiver {
    static InetSocketAddress dstAddress;

    /**
     * Constructor
     *
     * Attempts to create socket at given port and create an InetSocketAddress for the destinations
     */
    Worker(Terminal terminal, String dstHost, int dstPort, int srcPort) {

        super(terminal, srcPort);
        try {

            dstAddress= new InetSocketAddress(dstHost, dstPort);
        }
        catch(java.lang.Exception e) {e.printStackTrace();}
    }


    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void changePort(byte[] data) throws SocketException, InterruptedException {
        byte[] buffer= new byte[data[LENGTH_POS]];
        System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
        String content= new String(buffer);
        socket.close();
        socket = new DatagramSocket(Integer.parseInt(content));
        terminal.println("New port is: "+ Integer.parseInt(content));

    }
    public synchronized void onReceipt(DatagramPacket packet) throws IOException, InterruptedException {
        super.onReceipt(packet);

        byte[] data;
        String content;

        data = packet.getData();
        switch(data[TYPE_POS]) {
            case TYPE_WORKDESC:

                byte[] buffer = null;

                buffer= new byte[data[LENGTH_POS]];
                System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);
                content= new String(buffer);
                String[] splitContent = content.split("-");
                terminal.println("Work Description is: "+ splitContent[0]);
                String response = sendWorkResponse(terminal, dstAddress);
                if(response.equalsIgnoreCase("Y")||response.equalsIgnoreCase("yes")){
                    completeWork(terminal, dstAddress);
                }
                break;

            case TYPE_NEWPORT:
                changePort(data);
                break;

            case TYPE_ACK:
                terminal.println("Received ack\n");
                this.notify();
                break;

            default:
                terminal.println("Unexpected packet" + packet.toString());
        }
    }


    public synchronized void start() throws Exception {
        this.wait();
    }



    public synchronized void sendRegistration(Terminal terminal, InetSocketAddress dstAddress, String name) throws IOException {

        byte[] data = null;
        byte[] buffer = null;
        DatagramPacket packet = null;
        String input = null;

        buffer = name.getBytes();
        data = new byte[HEADER_LENGTH+buffer.length];

        data[TYPE_POS] = TYPE_REGISTER;
        data[LENGTH_POS] = (byte) buffer.length;
        System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

        terminal.println("Volunteering for work...");
        sendData(data, dstAddress);

        terminal.println("Volunteering request sent\n");

    }
    public synchronized void completeWork (Terminal terminal, InetSocketAddress dstAddress) throws IOException{

        byte[] data = null;
        byte[] buffer = null;
        DatagramPacket packet = null;

        terminal.println("Has work been done Y/N");
        String input= terminal.read("Response: ");
        buffer = input.getBytes();
        data = new byte[HEADER_LENGTH+buffer.length];

        data[TYPE_POS] = TYPE_WORKCOMPLETE;
        data[LENGTH_POS] = (byte)buffer.length;
        System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

        terminal.println("Sending completion...");
        sendData(data, dstAddress);
        terminal.println("Completion sent");
    }


    public synchronized String sendWorkResponse(Terminal terminal, InetSocketAddress dstAddress) throws IOException {

        byte[] data = null;
        byte[] buffer = null;
        DatagramPacket packet = null;

        terminal.println("Accept work Y/N");
        String input= terminal.read("Response: ");
        buffer = input.getBytes();
        data = new byte[HEADER_LENGTH+buffer.length];

        data[TYPE_POS] = TYPE_WORKRESPONSE;
        data[LENGTH_POS] = (byte)buffer.length;
        System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

        terminal.println("Sending response...");
        sendData(data,dstAddress);
        terminal.println("Response sent");
        return input;
    }

    /**
     * Test method
     *
     * Sends a packet to a given address
     */
    public static void main(String[] args) {
        try {
            String name = JOptionPane.showInputDialog("Enter client name");
            Terminal terminal= new Terminal(name);
            Worker worker = new Worker(terminal, DEFAULT_DST_NODE, Broker.DEFAULT_BROKER_PORT, DEFAULT_WORKER_PORT);
            worker.sendRegistration(terminal, dstAddress, name);


            worker.start();


        } catch(java.lang.Exception e) {e.printStackTrace();}
    }
}