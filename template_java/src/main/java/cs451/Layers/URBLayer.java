package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

public class URBLayer implements LinkLayer {
    private final PerfectLink l;
    private final LinkedBlockingQueue<PacketInfo> treatBuffer;
    private final ArrayList<TreeSet<Integer>> acks;

    private final ArrayList<Host> hosts;

    public URBLayer(int port, ArrayList<Host> hosts) throws SocketException {
        this.hosts = hosts;
        l = new PerfectLink(port, hosts.size(), this);
        treatBuffer = new LinkedBlockingQueue<>();
        acks = new ArrayList<>();
    }

    @Override
    public void deliver(PacketInfo p) {

    }

    @Override
    public void sendMessage(PacketInfo p) {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isDone() {
        return false;
    }
}
