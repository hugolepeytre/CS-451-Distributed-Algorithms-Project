package cs451.Layers;

import cs451.Util.PacketInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class DummyLayer implements LinkLayer {
    // Dummy layer for submission 1, only logs
    private final PerfectLink l;

    private ArrayList<String> log;
    private final String output_path;

    public DummyLayer(int receivePort, int nHosts, String outFile) throws SocketException {
        l = new PerfectLink(receivePort, nHosts, this);
        log = new ArrayList<>();
        output_path = outFile;
    }

    @Override
    public void deliver(PacketInfo p) {
        log.add("d " + p.getSenderId() + " " + p.getSequenceNumber() + "\n");
    }

    @Override
    public void sendMessage(PacketInfo p) {
        l.sendMessage(p);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        log = new ArrayList<>();
    }
}
