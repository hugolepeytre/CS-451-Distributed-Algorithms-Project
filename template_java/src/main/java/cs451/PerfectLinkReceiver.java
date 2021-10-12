package cs451;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class PerfectLinkReceiver extends PerfectLink {

    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private int[] portToID;
    private int port;

    public PerfectLinkReceiver(int port, int id, int[] portToID) throws SocketException {
        super(id);
        this.port = port;
        this.portToID = portToID;
        socket = new DatagramSocket(port);
    }

    // TODO : Message receiving
    @Override
    public void run() {
        while (true) {
            buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String received = new String(packet.getData(), 0, packet.getLength());
            int seqNum = Integer.parseInt(received);
            int senderPort = packet.getPort();
            int sender = portToID[senderPort - 11000];
            super.addToLog("d " + sender + " " + seqNum + "\n");

            InetAddress address = packet.getAddress();
            String s = "ack";
            buf = s.getBytes();
            packet = new DatagramPacket(buf, buf.length, address, senderPort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        socket.close(); // unreachable
    }

    @java.lang.Override
    public void writeOutput() {
        super.writeOutput();
        socket.close();
    }
}