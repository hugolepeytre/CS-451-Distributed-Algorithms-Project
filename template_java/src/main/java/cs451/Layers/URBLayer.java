package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.MessageList;
import cs451.Util.PacketInfo;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.BLOCK_TIME;

public class URBLayer implements LinkLayer {
    private final PerfectLink l;
    private final LinkLayer upperLayer;
    private final LinkedBlockingQueue<PacketInfo> treatBuffer;
    private final ArrayList<MessageList> acks;

    private final ArrayList<Host> hosts;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public URBLayer(int port, ArrayList<Host> hosts, LinkLayer upperLayer) throws SocketException {
        this.hosts = hosts;
        this.upperLayer = upperLayer;
        l = new PerfectLink(port, hosts.size(), this);
        treatBuffer = new LinkedBlockingQueue<>();
        acks = new ArrayList<>();
        for (int i = 0; i < hosts.size(); i++) {
            acks.add(new MessageList(hosts.size()));
        }

        running.set(true);
        new Thread(this::treatLoop).start();
    }

    private void treatLoop() {
        while (running.get()) {
            try {
                PacketInfo next = treatBuffer.poll(BLOCK_TIME, TimeUnit.MILLISECONDS);
                if (next != null) {
                    treat(next);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void treat(PacketInfo p) {
        if (!p.isAck()) {
            if (acks.get(p.getOriginalSenderId() - 1).forwarded(p)) {
                int newSeqNum = generateNewSeqNum();
                PacketInfo newP = p.becomeSender(newSeqNum);
                sendMessage(newP);
            }
            PacketInfo toDeliver = acks.get(p.getOriginalSenderId() - 1).addAck(p);
            if (toDeliver != null) {
                upperLayer.deliver(toDeliver);
            }
        }
    }

    private int generateNewSeqNum() {
        // TODO
        return 0;
    }

    @Override
    public void deliver(PacketInfo p) {
        treatBuffer.add(p);
    }

    @Override
    public void sendMessage(PacketInfo p) {
        for (Host h : hosts) {
            l.sendMessage(p.newDestination(h.getId(), h.getPort(), h.getAddress()));
        }
        int id = p.getSenderId();
        acks.get(id - 1).addMessage(p);
    }

    @Override
    public void close() {
        running.set(false);
        l.close();
    }

    @Override
    public boolean isDone() {
        return false;
    }
}
