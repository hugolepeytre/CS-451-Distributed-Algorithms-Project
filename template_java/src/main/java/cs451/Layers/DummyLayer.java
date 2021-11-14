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

import static cs451.Util.Constants.BROADCAST_BATCH_SIZE;

public class DummyLayer implements LinkLayer {
    // Dummy layer for submission 3, only logs
    private final LinkLayer l;
    // private final long begin;

    private ConcurrentLinkedQueue<String> log;
    private final String output_path;

    private final int id;
    private final int nHosts;
    private final int nMessages;
    private int nSent = 1;

    private int nextThreshold = 0;
    private int nHostsReachedThreshold = 0;

    public DummyLayer(int receivePort, List<Host> hosts, int nMessages, int id, String outFile) throws SocketException {
        log = new ConcurrentLinkedQueue<>();
        output_path = outFile;
        // begin = System.nanoTime();
        createOutputFile(output_path);
        nHosts = hosts.size();
        this.nMessages = nMessages;
        this.id = id;
        l = new FIFOLayer(receivePort, hosts, this);
        sendBatch();
    }

    public DummyLayer(int receivePort, List<Host> hosts, int nMessages, int id, TreeSet<Integer> influencers, String outFile) throws SocketException {
        log = new ConcurrentLinkedQueue<>();
        output_path = outFile;
        // begin = System.nanoTime();
        createOutputFile(output_path);
        nHosts = hosts.size();
        this.nMessages = nMessages;
        this.id = id;
        l = new LCBLayer(receivePort, hosts, influencers, this);
        sendBatch();
    }

    @Override
    public void deliver(PacketInfo p) {
        // long currentTime = (System.nanoTime() - begin)/1_000_000_000;
        // if (!p.isAck()) log.add("d " + p.getOriginalSenderId() + " " + p.getSequenceNumber() + ", seconds since start : " + currentTime + "\n");
        if (!p.isAck()) log.add("d " + p.getOriginalSenderId() + " " + p.getSequenceNumber() + "\n");
        if (p.getSequenceNumber() == nextThreshold) {
            if (++nHostsReachedThreshold >= (nHosts/2 + 1)) {
                sendBatch();
            }
        }
    }

    @Override
    public void sendMessage(PacketInfo p) {
        l.sendMessage(p);
        // long currentTime = (System.nanoTime() - begin)/1_000_000_000;
        // log.add("b " + p.getSequenceNumber() + ", seconds since start : " + currentTime + "\n");
        log.add("b " + p.getSequenceNumber() + "\n");
    }

    @Override
    public void close() {
        l.close();
        writeOutput();
    }

    public void writeOutput() {
        try {
            File out = new File(output_path);
            FileWriter f2 = new FileWriter(out, true);
            f2.write(String.join("", log));
            f2.close();
            log = new ConcurrentLinkedQueue<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createOutputFile(String outputPath) {
        try {
            File out = new File(outputPath);
            if (!out.createNewFile()) {
                FileWriter f2 = new FileWriter(out, false);
                f2.write("");
                f2.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBatch() {
        nextThreshold += BROADCAST_BATCH_SIZE;
        nHostsReachedThreshold = 0;
        for (int i = 0; i < BROADCAST_BATCH_SIZE && nSent <= nMessages; i++) {
            String payload = Integer.toString(nSent);
            PacketInfo toSend = PacketInfo.newPacket(id, 0, nSent, payload, null);
            sendMessage(toSend);
            nSent++;
        }
    }
}
