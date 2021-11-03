package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DummyLayer implements LinkLayer {
    // Dummy layer for submission 3, only logs
    private final LinkLayer l;

    private final ConcurrentLinkedQueue<String> log;
    private final String output_path;

    public DummyLayer(int receivePort, List<Host> hosts, String outFile) throws SocketException {
        l = new FIFOLayer(receivePort, hosts, this);
        log = new ConcurrentLinkedQueue<>();
        output_path = outFile;
    }

    public DummyLayer(int receivePort, List<Host> hosts, TreeSet<Integer> influencers, String outFile) throws SocketException {
        l = new LCBLayer(receivePort, hosts, influencers, this);
        log = new ConcurrentLinkedQueue<>();
        output_path = outFile;
    }

    @Override
    public void deliver(PacketInfo p) {
        if (!p.isAck()) log.add("d " + p.getOriginalSenderId() + " " + p.getOriginalSequenceNumber() + "\n");
    }

    @Override
    public void sendMessage(PacketInfo p) {
        l.sendMessage(p);
        log.add("b " + p.getOriginalSequenceNumber() + "\n");
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

    @Override
    public int nextSeqNum() {
        return l.nextSeqNum();
    }

    public void writeOutput() {
        try {
            File out = new File(output_path);
            FileWriter f2 = new FileWriter(out, false);
            f2.write(String.join("", log));
            f2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
