package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.*;

public class UDPLink implements LinkLayer {
    private final PerfectLink upperLayer;
    private final DatagramSocket receiveSocket;
    private final DatagramSocket sendSocket;
    private final byte[] receiveBuffer;
    private final LinkedBlockingDeque<PacketInfo> sendBuffer;
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
        sendBuffer = new LinkedBlockingDeque<>();

        running.set(true);
        listenThread = new Thread(this::listenLoop);
        sendThread = new Thread(this::sendLoop);
        listenThread.start();
        sendThread.start();
    }

    private void sendLoop() {
        while (running.get()) {
            if (sendBuffer.size() < 10) {
                System.out.println(sendBuffer.size());
                upperLayer.retransmit();
            }
            try {
                PacketInfo next = sendBuffer.poll(BLOCK_TIME, TimeUnit.MILLISECONDS);
                if (next != null){
                    byte[] b = next.toPacket();
                    int targetId = next.getTargetId();
                    Host targetHost = hosts.get(targetId - 1);
                    sendSocket.send(new DatagramPacket(b, b.length, targetHost.getAddress(), targetHost.getPort()));
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
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
            if (received) upperLayer.deliver(PacketInfo.fromPacket(packet));
        }
    }

    @Override
    public void sendMessage(PacketInfo p) { // OPT : Make acks prioritary
        if (p.isAck()) {
            sendBuffer.addFirst(p);
        }
        else {
            sendBuffer.addLast(p);
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
