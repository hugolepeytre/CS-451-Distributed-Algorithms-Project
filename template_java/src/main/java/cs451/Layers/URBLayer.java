package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.MessageList;
import cs451.Util.PacketInfo;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.BLOCK_TIME;

public class URBLayer implements LinkLayer {
    private final PerfectLink l;
    private final LinkLayer upperLayer;
    private final LinkedBlockingQueue<PacketInfo> treatBuffer;
    private final MessageList[] acks;

    private final int nHosts;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public URBLayer(int port, List<Host> hosts, LinkLayer upperLayer) throws SocketException {
        nHosts = hosts.size();
        this.upperLayer = upperLayer;
        treatBuffer = new LinkedBlockingQueue<>();
        acks = new MessageList[nHosts];
        for (int i = 0; i < acks.length; i++) {
            acks[i] = new MessageList(nHosts);
        }

        running.set(true);
        l = new PerfectLink(port, hosts, this);
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
            MessageList ml = acks[p.getOriginalSenderId() - 1];
            if (!ml.wasForwarded(p)) {
                PacketInfo newP = p.becomeSender();
                sendMessage(newP);
            }
            PacketInfo toDeliver = ml.addAck(p);
            if (toDeliver != null) {
                upperLayer.deliver(toDeliver);
            }
        }
    }

    @Override
    public void deliver(PacketInfo p) {
        treatBuffer.add(p);
    }

    @Override
    public void sendMessage(PacketInfo p) {
        int id = p.getSenderId();
        for (int i = 1; i <= nHosts; i++) {
            if (i != id) {
                l.sendMessage(p.newDestination(i));
            }
        }
        if (id == p.getOriginalSenderId()) {
            acks[id - 1].addMessage(p);
        }
    }

    @Override
    public void close() {
        running.set(false);
        l.close();
    }
}
