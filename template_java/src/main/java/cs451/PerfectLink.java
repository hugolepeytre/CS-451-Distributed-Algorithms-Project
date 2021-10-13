package cs451;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;

import static cs451.Constants.ACK;
import static cs451.Constants.PORTS_BEGIN;

class PerfectLink {
    private final DatagramSocket socket;
    private InetAddress address;
    private final int[] portToID;
    private final byte[] bufAck = ACK.getBytes();
    private ArrayList<String> log;
    private final String output_path;
    private final ArrayList<HashSet<String>> delivered;

    private final byte[] buf;

    public PerfectLink(int receivePort, int id, int[] portToID) throws SocketException {
        this.portToID = portToID;
        socket = new DatagramSocket(receivePort);
        buf = new byte[256];
        delivered = new ArrayList<>();
        for (int i : portToID) {
            if (i != 0) {
                delivered.add(new HashSet<>());
            }
        }
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        log = new ArrayList<>();
        output_path = "../config_files/outputs/" + id + ".txt";
    }

    public void sendMessage(String toSend, int sendPort) {
        boolean ack = false;
        log(toSend);
        byte[] bufSend = toSend.getBytes();
        while (!ack) {
            DatagramPacket packet = new DatagramPacket(bufSend, bufSend.length, address, sendPort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.setSoTimeout(100);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                ack = received.equals(ACK);
            } catch (SocketTimeoutException e) {

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void listen(int timeout) {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.setSoTimeout(timeout);
            socket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            int senderPort = packet.getPort();
            int sender = portToID[senderPort - PORTS_BEGIN];
            log(sender, received);

            InetAddress address = packet.getAddress();
            packet = new DatagramPacket(bufAck, bufAck.length, address, senderPort);
            socket.send(packet);
        } catch (SocketTimeoutException e) {
            // Do nothing, we will loop
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(int sender, String message) {
        HashSet<String> deliveredFromSender = delivered.get(sender);
        if (!deliveredFromSender.contains(message)) {
            deliveredFromSender.add(message);
            log.add("d " + sender + " " + message + "\n");
        }
    }

    private void log(String message) {
        log.add("b " + message + "\n");
    }

    public void close() {
        writeOutput();
        socket.close();
    }

    public void writeOutput() {
        Path path = Paths.get(output_path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND,StandardOpenOption.CREATE)) {
            for (String line : log) {
                writer.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log = new ArrayList<>();
    }
}
