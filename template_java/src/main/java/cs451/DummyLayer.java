package cs451;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class DummyLayer extends LinkLayer{
    // Dummy layer for submission 1, only logs
    private final PerfectLink l;
//    private final long timeBegin;

    private ArrayList<String> log;
    private final String output_path;

    public DummyLayer(int receivePort, int id, int[] portToID, String outFile) throws SocketException {
//        timeBegin = System.nanoTime();
        l = new PerfectLink(receivePort, portToID, this);
        log = new ArrayList<>();
        output_path = outFile + id + ".txt";
    }

    @Override
    public void deliver(PacketInfo p, int sender) {
        log.add("d " + sender + " " + p.getPacketNumber() + "\n");
//        long elapsed = (System.nanoTime() - timeBegin)/1_000_000;
//        log.add("d " + sender + " " + p.getPacketNumber() + " " + p.getMessage() + " " + elapsed + "\n");
    }

    @Override
    public void sendMessage(int port, InetAddress address, int packetNumber, String message) {
        l.sendMessage(port, address, packetNumber, message);
        log.add("b " + packetNumber + "\n");
//        long elapsed = (System.nanoTime() - timeBegin)/1_000_000;
//        log.add("b " + packetNumber + " " + message + " " + elapsed + "\n");
    }

    @Override
    public void close() {
        l.close();
        writeOutput();
    }

    @Override
    public boolean isDone() {
        return l.isDone();
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
