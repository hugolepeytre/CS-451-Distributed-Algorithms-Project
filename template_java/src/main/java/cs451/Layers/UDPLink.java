package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.*;

public class UDPLink implements LinkLayer {
    private final PerfectLink upperLayer;
    private final DatagramSocket receiveSocket;
    private final DatagramSocket sendSocket;
    private final byte[] receiveBuffer;
    private final byte[] sendBuffer;
    private final long[] lastSends;
    private final int[] waitTimes;
    private final LinkedBlockingDeque<PacketInfo>[] sendBuffers;
    private final List<Host> hosts;

    private final Thread listenThread;
    private final Thread sendThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public UDPLink(int port, List<Host> hosts, PerfectLink up) throws SocketException {
        upperLayer = up;
        this.hosts = hosts;
        receiveSocket = new DatagramSocket(port);
        sendSocket = new DatagramSocket();
        receiveBuffer = new byte[BUF_SIZE];
        sendBuffer = new byte[BUF_SIZE];
        int nHosts = hosts.size();
        sendBuffers = new LinkedBlockingDeque[nHosts];
        lastSends = new long[nHosts];
        waitTimes = new int[nHosts];
        long now = System.nanoTime();
        for (int i = 0; i < nHosts; i++) {
            sendBuffers[i] = new LinkedBlockingDeque<>();
            lastSends[i] = now;
            waitTimes[i] = BASE_RESET_MILLIS;
        }

        running.set(true);
        listenThread = new Thread(this::listenLoop);
        sendThread = new Thread(this::sendLoop);
        listenThread.start();
        sendThread.start();
    }

    private void sendLoop() {
        while (running.get()) {
            for (int i = 0; i < hosts.size(); i++) {
                if (timeToResend(i)) {
                    if (sendBuffers[i].size() < 2 * PACKET_GROUP_SIZE) {
                        upperLayer.retransmit(i);
                    }
                    try {
                        int nSent = 0;
                        PacketInfo next;
                        do {
                            next = sendBuffers[i].poll();
                            if (next != null) {
                                byte[] b = next.toPacket();
                                System.arraycopy(b, 0, sendBuffer, 4 + nSent * MAX_PACKET_SIZE, b.length);
                                nSent++;
                            }
                        } while (nSent < PACKET_GROUP_SIZE && next != null);
                        System.arraycopy(ByteBuffer.allocate(4).putInt(nSent).array(), 0, sendBuffer, 0, 4);
                        sendSocket.send(new DatagramPacket(sendBuffer, BUF_SIZE,
                                hosts.get(i).getAddress(), hosts.get(i).getPort()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean timeToResend(int i) {
        long now = System.nanoTime();
        long milisSinceLastResend = (now - lastSends[i])/1_000_000;
        boolean r = milisSinceLastResend > waitTimes[i];
        if (r) {
            lastSends[i] = now;
            waitTimes[i] *= 2;
        }
        return r;

    }

    private void listenLoop() {
        while (running.get()) {
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            boolean received = false;
            try {
                receiveSocket.setSoTimeout(BLOCK_TIME);
                receiveSocket.receive(packet);
                received = true;
            } catch (SocketTimeoutException e) {
                // Do nothing, we loop
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (received) {
                byte[] data = packet.getData();
                int nSent = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
                PacketInfo rP;
                for (int i = 0; i < nSent; i++) {
                    rP = PacketInfo.fromPacket(Arrays.copyOfRange(data, 4 + i*MAX_PACKET_SIZE, 4 + (i+1)*MAX_PACKET_SIZE));
                    if (i == 0) { waitTimes[rP.getSenderId() - 1] = BASE_RESET_MILLIS; }
                    upperLayer.deliver(rP);
                }
            }
        }
    }

    @Override
    public void sendMessage(PacketInfo p) { // OPT : Make acks prioritary
        if (p.isAck()) {
            sendBuffers[p.getTargetId() - 1].addFirst(p);
        }
        else {
            sendBuffers[p.getTargetId() - 1].addLast(p);
        }
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
        receiveSocket.close();
        sendSocket.close();
    }

    @Override
    public void deliver(PacketInfo p) {
        // Does nothing, this is the lowest layer
    }
}
