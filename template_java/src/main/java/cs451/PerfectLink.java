package cs451;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Constants.*;

class PerfectLink extends LinkLayer {
    private final DatagramSocket socket;
    private InetAddress address;
    private final int[] portToID;

    private final static int MS_WAIT_ACK = 1000;
    private long stamp = System.nanoTime();

    private final LinkLayer upperLayer;
    private final ArrayList<TreeSet<Integer>> delivered;
    private final ConcurrentLinkedQueue<PacketInfo> toBeAcked;
    private final ConcurrentLinkedQueue<PacketInfo> sendBuffer;

    private final Thread listenThread;
    private final Thread sendThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final byte[] buf;

    public PerfectLink(int receivePort, int[] portToID, LinkLayer up) throws SocketException {
        this.portToID = portToID;
        this.upperLayer = up;
        socket = new DatagramSocket(receivePort);
        buf = new byte[256];
        delivered = new ArrayList<>();
        sendBuffer = new ConcurrentLinkedQueue<>();
        toBeAcked = new ConcurrentLinkedQueue<>();
        for (int i : portToID) {
            if (i != 0) {
                delivered.add(new TreeSet<>());
            }
        }
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        running.set(true);
        listenThread = new Thread(this::listenLoop);
        sendThread = new Thread(this::sendLoop);
        listenThread.start();
        sendThread.start();
    }

    private void sendLoop() {
        while (running.get()) {
            sendNextInQueue();
            long currentTime = System.nanoTime();
            long elapsedNanoSecs = currentTime - stamp;
            if (elapsedNanoSecs > (NANOSECS_IN_MS * MS_WAIT_ACK)) {
                stamp = currentTime;
                emptyACKQueue();
            }
        }
    }

    private void sendNextInQueue() {
        PacketInfo next = sendBuffer.poll();
        if (next == null) return;
        try {
            socket.send(next.getSendPacket());
        } catch (IOException e) {
            e.printStackTrace();
        }
        toBeAcked.add(next);
    }

    private void emptyACKQueue() {
        for (PacketInfo p : toBeAcked) {
            try {
                socket.send(p.getSendPacket());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendMessage(int port, InetAddress address, int packetNumber, String message) {
        sendBuffer.add(new PacketInfo(port, address, packetNumber, message));
    }

    private void listenLoop() {
        while (running.get()) {
            // Listening
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            boolean received = false;
            try {
                socket.setSoTimeout(10*1000);
                socket.receive(packet);
                received = true;
            } catch (SocketTimeoutException e) {
                // Do nothing, we loop
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Treating received data
            if (received) {
                byte[] payload = packet.getData();
                int packetLength = packet.getLength();
                PacketInfo receivedP = new PacketInfo(packet.getPort(), packet.getAddress(), payload, packetLength);
                int senderPort = receivedP.getPort();
                int packetNumber = receivedP.getPacketNumber();
                int sender = portToID[senderPort - PORTS_BEGIN];
                String message = receivedP.getMessage();

                if (message.equals(ACK)) {
                    toBeAcked.remove(receivedP);
                } else {
                    // Sending ACK
                    sendBuffer.add(new PacketInfo(senderPort, address, packetNumber, ACK));

                    // Logging and delivering
                    TreeSet<Integer> deliveredFromSender = delivered.get(sender - 1);
                    if (!deliveredFromSender.contains(packetNumber)) {
                        deliveredFromSender.add(packetNumber);
                        upperLayer.deliver(receivedP, sender);
                    }
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return toBeAcked.isEmpty() && sendBuffer.isEmpty();
    }

    @Override
    public void close() {
        running.set(false);
        try {
            sendThread.join();
            listenThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socket.close();
    }

    @Override
    public void deliver(PacketInfo p, int sender) {
        // Does nothing, this is the lowest layer
    }
}
