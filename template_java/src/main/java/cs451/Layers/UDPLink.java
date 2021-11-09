package cs451.Layers;

import cs451.Util.PacketInfo;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.*;

public class UDPLink implements LinkLayer {
    private final LinkLayer upperLayer;
    private final DatagramSocket socket;
    private final byte[] receiveBuffer;
    private final LinkedBlockingQueue<PacketInfo> sendBuffer;

    private final Thread listenThread;
    private final Thread sendThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public UDPLink(int port, LinkLayer up) throws SocketException {
        upperLayer = up;
        socket = new DatagramSocket(port);
        receiveBuffer = new byte[BUF_SIZE];
        sendBuffer = new LinkedBlockingQueue<>();

        running.set(true);
        listenThread = new Thread(this::listenLoop);
        sendThread = new Thread(this::sendLoop);
        listenThread.start();
        sendThread.start();
    }

    private void sendLoop() {
        while (running.get()) {
            try {
                PacketInfo next = sendBuffer.poll(BLOCK_TIME, TimeUnit.MILLISECONDS);
                if (next != null){
                    socket.send(next.toPacket());
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
                socket.setSoTimeout(BLOCK_TIME);
                socket.receive(packet);
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
    public void sendMessage(PacketInfo p) {
        sendBuffer.add(p);
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
    public void deliver(PacketInfo p) {
        // Does nothing, this is the lowest layer
    }
}
