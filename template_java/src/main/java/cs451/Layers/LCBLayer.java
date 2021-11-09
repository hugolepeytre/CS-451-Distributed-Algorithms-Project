package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.net.SocketException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.BLOCK_TIME;

public class LCBLayer implements LinkLayer {
    private final URBLayer l;
    private final LinkLayer upperLayer;

    private final LinkedBlockingQueue<PacketInfo> treatBuffer; // Accessed in deliver and treat loop
    private final PriorityQueue<PacketInfo>[] pending; // Not concurrent, accessed only in treat loop

    private final TreeSet<Integer> notCausalHosts;
    private final int[] vectorClock;
    private int broadcast = 0;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public LCBLayer(int port, List<Host> hosts, TreeSet<Integer> influencers, LinkLayer upperLayer) throws SocketException {
        this.upperLayer = upperLayer;
        treatBuffer = new LinkedBlockingQueue<>();
        pending = new PriorityQueue[hosts.size()];
        vectorClock = new int[hosts.size()];
        notCausalHosts = new TreeSet<>();
        for (int i = 0; i < hosts.size(); i++) {
            pending[i] = new PriorityQueue<>(Comparator.comparingInt(PacketInfo::getSequenceNumber));
            notCausalHosts.add(i + 1);
            vectorClock[i] = 0;
        }
        for (int in: influencers) {
            notCausalHosts.remove(in);
        }

        running.set(true);
        new Thread(this::treatLoop).start();
        this.l = new URBLayer(port, hosts, this);
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
        int oIdx = p.getOriginalSenderId() - 1;
        PriorityQueue<PacketInfo> considered = pending[oIdx];
        considered.add(p);

        PacketInfo nextElem = considered.peek();
        while (nextElem != null && nextElem.compareVectorClock(vectorClock)) {
            considered.remove();
            upperLayer.deliver(nextElem);
            nextElem = considered.peek();
            vectorClock[oIdx]++;
        }
    }

    @Override
    public void deliver(PacketInfo p) {
        treatBuffer.add(p);
    }

    @Override
    public void sendMessage(PacketInfo p) {
        int[] newVClock = Arrays.copyOf(vectorClock, vectorClock.length);
        for (int i: notCausalHosts) {
            newVClock[i - 1] = 0;
        }
        newVClock[p.getOriginalSenderId() - 1] = broadcast++;
        p.setVectorClock(newVClock);
        l.sendMessage(p);
    }

    @Override
    public void close() {
        running.set(false);
        l.close();
    }
}
