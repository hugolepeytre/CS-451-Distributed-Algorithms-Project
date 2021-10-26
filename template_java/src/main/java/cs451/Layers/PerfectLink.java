package cs451.Layers;

import cs451.Util.PacketInfo;

import java.net.*;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.*;

// TODO : Check speed logic for searching for already seen and toBeAcked packets
// TODO : Maybe for toBeAcked should be stored as sequence number and who we sent it to,
//  and whole packets to be resent should be stored somewhere else
// TODO : Improve logic for re-sending non-acked packets with host-specific timer
class PerfectLink implements LinkLayer {

    private final UDPLink l;
    private final LinkLayer upperLayer;
    private final LinkedBlockingQueue<PacketInfo> toTreat;
    private final ConcurrentLinkedQueue<PacketInfo> toBeAcked;
    private final ArrayList<TreeSet<Integer>> delivered;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public PerfectLink(int port, int nHosts, LinkLayer up) throws SocketException {
        this.upperLayer = up;
        this.l = new UDPLink(port, this);

        toTreat = new LinkedBlockingQueue<>();
        toBeAcked = new ConcurrentLinkedQueue<>();
        delivered = new ArrayList<>();
        for (int i = 0; i < nHosts; i++) {
            delivered.add(new TreeSet<>());
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
            for (PacketInfo p : toBeAcked) {
                l.sendMessage(p);
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
        String message = p.getPayload();
        if (message.equals(ACK)) {
            toBeAcked.remove(p);
        } else {
            // Sending ACK
            l.sendMessage(p.getACK());

            // Logging and delivering
            TreeSet<Integer> deliveredFromSender = delivered.get(p.getSenderId() - 1);
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
        toBeAcked.add(p);
        l.sendMessage(p);
    }

    @Override
    public void close() {
        running.set(false);
    }
}
