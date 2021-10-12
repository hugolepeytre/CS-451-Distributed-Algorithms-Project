package cs451;

import java.io.IOException;
import java.net.*;

public class PerfectLinkSender extends PerfectLink {
    private DatagramSocket socket;
    private InetAddress address;
    private int receivePort;
    private int sendPort;
    private int n;
    private int sent = 1;

    private byte[] buf;

    // Port : where messages are sent, n : number of messages to send
    public PerfectLinkSender(int sendPort, int receivePort, int id, int n) throws SocketException {
        super(id);
        socket = new DatagramSocket(receivePort);
        this.receivePort = receivePort;
        this.n = n;
        this.sendPort = sendPort;
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendNext() {
        boolean ack = false;
        String toSend = Integer.toString(sent);
        super.addToLog("b " + sent + "\n");
        while (!ack) {
            buf = toSend.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, sendPort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            buf = new byte[256];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.setSoTimeout(100);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                ack = received.equals("ack");
            } catch (SocketTimeoutException e) {

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sent++;
    }

    public void close() {
        socket.close();
    }

    // TODO : Message sending
    @Override
    public void run() {
        while (sent <= n) {
            sendNext();
        }
        super.writeOutput();
    }
}