package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.net.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.*;

// Has a lower layer used for actual UDP implementation. 2 loops, one for treating delivered packets + propagate upwards
// and one for retransmission. Can be called from below to deliver, and from above to send a new message.
// Uses only UDP sequence numbers, they uniquely identify some message or its ACK but not the sender. Messages have to
// be separated by hosts they are being sent to/from too.
class PerfectLink implements LinkLayer {
    // Lower and upper links
    private final UDPLink l;
    private final LinkLayer upperLayer;

    // Buffers :
    // Concurrent (treat loop + deliver). Make sure to not compare by seq num, as it contains both acks and packets
    // with same seqNum, and both should be sent.
    private final LinkedBlockingQueue<PacketInfo> toTreat;
    // Not concurrent (treat loop). One set of delivered messages per host.
    private final TreeSet<PacketInfo>[] delivered;
    // Concurrent (retransmit loop + treat loop + send message). Add when sending a message, remove when treating
    // and go through when retransmitting. Sorted by UDP sequence number. Does not contain any acks, only sent packets.
    private final ConcurrentSkipListSet<PacketInfo>[] toBeAcked;
    private final int nHosts;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean retransmit = new AtomicBoolean(false);
    private final ConcurrentSkipListSet<Integer> retransmitTargets = new ConcurrentSkipListSet<>();

    public PerfectLink(int port, List<Host> hosts, LinkLayer up) throws SocketException {
        this.upperLayer = up;
        nHosts = hosts.size();
        toTreat = new LinkedBlockingQueue<>();
        toBeAcked = new ConcurrentSkipListSet[nHosts];
        delivered = new TreeSet[nHosts];
        for (int i = 0; i < nHosts; i++) {
            delivered[i] = new TreeSet<>(Comparator.comparingInt(PacketInfo::getSequenceNumber)
                    .thenComparingInt(PacketInfo::getOriginalSenderId));
            toBeAcked[i] = new ConcurrentSkipListSet<>(Comparator.comparingInt(PacketInfo::getSequenceNumber)
                    .thenComparingInt(PacketInfo::getOriginalSenderId));
        }

        running.set(true);
        this.l = new UDPLink(port, hosts, this);
        new Thread(this::treatLoop).start();
        new Thread(this::retransmitLoop).start();
    }

    /**
     *  While layer is running, add all un-acked packets to send buffer every BLOCK_TIME
     */
    private void retransmitLoop() { // OPT : Only retransmit 100 messages per host every time
        while (running.get()) {
            for (int i = 0; i < nHosts; i++) {
                if (retransmitTargets.contains(i)) {
                    ConcurrentSkipListSet<PacketInfo> list = toBeAcked[i];
                    int count = 0;
                    for (Iterator<PacketInfo> it = list.iterator(); it.hasNext() && count < RETRANSMIT_PER_HOST; count++) {
                        PacketInfo p = it.next();
                        l.sendMessage(p);
                    }
                }
                retransmitTargets.remove(i);
            }
        }
    }

    public void retransmit(int i) {
        retransmit.set(true);
        retransmitTargets.add(i);
    }

    /**
     * While layer is running, take a received packet and treat it
     */
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

    /**
     * Removes ACKs from toBeAcked, otherwise send ACK and deliver if not done already
     * @param p : Packet to be treated
     */
    private void treat(PacketInfo p) {
        if (p.isAck()) {
            toBeAcked[p.getSenderId() - 1].remove(p);
        } else {
            // Sending ACK
            l.sendMessage(p.getACK());

            // Logging and delivering
            TreeSet<PacketInfo> deliveredFromSender = delivered[p.getSenderId() - 1];
            if (!deliveredFromSender.contains(p)) {
                deliveredFromSender.add(p);
                upperLayer.deliver(p);
            }
        }
    }

    /**
     * Add received packet to a buffer
     */
    @Override
    public void deliver(PacketInfo p) {
        toTreat.add(p);
    }

    /**
     * Mark as to be acked and send a packet through lower layer
     */
    @Override
    public void sendMessage(PacketInfo p) {
        toBeAcked[p.getTargetId() - 1].add(p);
        // l.sendMessage(p); OPT : Don't send right away, let retransmitLoop take care of which packets to send in priority
    }

    /**
     * Does not wait for threads to actually finish running
     */
    @Override
    public void close() {
        running.set(false);
    }
}
