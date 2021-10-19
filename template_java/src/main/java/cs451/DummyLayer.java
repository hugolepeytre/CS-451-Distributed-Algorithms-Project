package cs451;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
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
        output_path = outFile;
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
        System.out.println("added to log");
        System.out.println(log.get(0));
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
        try {
            File out = new File(output_path);
            out.createNewFile();
            FileWriter f2 = new FileWriter(out, true);
            f2.write(String.join("", log));
            f2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log = new ArrayList<>();
    }
}
