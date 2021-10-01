import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

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
public class CommandAndControl extends MessengerAndReceiver {

    static InetSocketAddress dstAddress;

    /**
     * Constructor
     *
     * Attempts to create socket at given port and create an InetSocketAddress for the destinations
     */
    CommandAndControl(Terminal terminal, String dstHost, int dstPort, int srcPort) {
        super(terminal, srcPort);
        try {
            dstAddress= new InetSocketAddress(dstHost, dstPort);
        }
        catch(Exception e) {e.printStackTrace();}
    }


    /**
     * Assume that incoming packets contain a String and print the string.
     */
    public synchronized void onReceipt(DatagramPacket packet) throws IOException {
        byte[] data;

        data = packet.getData();
        switch(data[TYPE_POS]) {
            case TYPE_ACK:
                terminal.println("Received ack\n");
                sendWorkDescription(terminal, dstAddress);
                break;
            default:
                terminal.println("Unexpected packet" + packet.toString());
        }
    }

    //  SENDING FUNCTIONS
    public synchronized void sendWorkDescription(Terminal terminal, InetSocketAddress dstAddress) throws IOException {

        byte[] data = null;
        byte[] buffer = null;
        DatagramPacket packet = null;

        //set up packet
        String input = terminal.read("Work Description: ");

        terminal.println("No. of workers to receive description (1,2,,...all)");
        String workerNum = terminal.read("No. of Workers: ");
        input = input + "-" + workerNum;

        buffer = input.getBytes();
        data = new byte[HEADER_LENGTH+buffer.length];

        data[TYPE_POS] = TYPE_WORKDESC;
        data[LENGTH_POS] = (byte)buffer.length;
        System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

        terminal.println("Sending work description packet...");

        //send
        sendData(data, dstAddress);

        terminal.println("Work description packet sent");
    }

    /**
     * Test method
     *
     * Sends a packet to a given address
     */
    public static void main(String[] args) {
        try {

            Terminal terminal= new Terminal("Command and Control");
            (new CommandAndControl(terminal, DEFAULT_DST_NODE, DEFAULT_BROKER_PORT, DEFAULT_CNC_PORT)).sendWorkDescription(terminal, dstAddress);

        } catch(Exception e) {e.printStackTrace();}
    }
}