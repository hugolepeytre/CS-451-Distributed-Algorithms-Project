package cs451.Layers;

import cs451.Util.PacketInfo;

import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.*;

// TODO : Improve logic for re-sending non-acked packets with host-specific timer
// TODO : Make lower-number packets prioritary
class PerfectLink implements LinkLayer {
    private final UDPLink l;
    private final LinkLayer upperLayer;
    private final LinkedBlockingQueue<PacketInfo> toTreat;
    private final TreeSet<Integer>[] delivered; // Not concurrent
    private final ConcurrentSkipListSet<PacketInfo>[] toBeAcked;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public PerfectLink(int port, int nHosts, LinkLayer up) throws SocketException {
        this.upperLayer = up;
        this.l = new UDPLink(port, this);

        toTreat = new LinkedBlockingQueue<>();
        toBeAcked = new ConcurrentSkipListSet[nHosts];
        delivered = new TreeSet[nHosts];
        for (int i = 0; i < nHosts; i++) {
            delivered[i] = new TreeSet<>();
            toBeAcked[i] = new ConcurrentSkipListSet<>(Comparator.comparingInt(PacketInfo::getSequenceNumber));
        }

        running.set(true);
        new Thread(this::treatLoop).start();
        new Thread(this::retransmitLoop).start();
    }

    private void retransmitLoop() {
        while (running.get()) {
            try {
                TimeUnit.MILLISECONDS.sleep(BLOCK_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (ConcurrentSkipListSet<PacketInfo> list : toBeAcked) {
                for (PacketInfo p : list) {
                    l.sendMessage(p);
                }
            }
        }
    }

    private void treatLoop() {
        while (running.get()) {
            try {
                PacketInfo next = toTreat.poll(BLOCK_TIME, TimeUnit.MILLISECONDS);
                if (next != null) {
                    treat(next);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void treat(PacketInfo p) {
        if (p.isAck()) {
            toBeAcked[p.getSenderId() - 1].remove(p);
        } else {
            // Sending ACK
            l.sendMessage(p.getACK());

            // Logging and delivering
            TreeSet<Integer> deliveredFromSender = delivered[p.getSenderId() - 1];
            int seqNum = p.getSequenceNumber();
            if (!deliveredFromSender.contains(seqNum)) {
                deliveredFromSender.add(seqNum);
                upperLayer.deliver(p);
            }
        }
    }

    @Override
    public void deliver(PacketInfo p) {
        toTreat.add(p);
    }

    @Override
    public void sendMessage(PacketInfo p) {
        toBeAcked[p.getTargetId() - 1].add(p);
        l.sendMessage(p);
    }

    @Override
    public void close() {
        running.set(false);
    }

    @Override
    public boolean isDone() {
        for (ConcurrentSkipListSet<PacketInfo> list : toBeAcked) {
            if (!list.isEmpty()) {
                return false;
            }
        }
        return l.isDone() && toTreat.isEmpty();
    }

    @Override
    public int nextSeqNum() {
        return upperLayer.nextSeqNum();
    }
}
