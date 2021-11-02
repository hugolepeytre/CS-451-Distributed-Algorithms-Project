package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cs451.Util.Constants.BLOCK_TIME;

public class FIFOLayer implements LinkLayer {
    private final URBLayer l;
    private final LinkLayer upperLayer;

    private final LinkedBlockingQueue<PacketInfo> treatBuffer;
    private final ArrayList<PriorityQueue<PacketInfo>> pending;
    private final ArrayList<Integer> nextToDeliver;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public FIFOLayer(int port, List<Host> hosts, LinkLayer upperLayer) throws SocketException {
        this.upperLayer = upperLayer;
        this.l = new URBLayer(port, hosts, this);
        treatBuffer = new LinkedBlockingQueue<>();
        pending = new ArrayList<>();
        nextToDeliver = new ArrayList<>();
        for (int i = 0; i < hosts.size(); i++) {
            pending.add(new PriorityQueue<>(Comparator.comparingInt(PacketInfo::getSequenceNumber)));
            nextToDeliver.add(1);
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
        int oIdx = p.getOriginalSenderId() - 1;
        PriorityQueue<PacketInfo> considered = pending.get(oIdx);
        considered.add(p);
        System.out.println("Containing " + considered.size() + " for sender " + (oIdx + 1));

        PacketInfo nextElem = considered.peek();
        int next = nextToDeliver.get(oIdx);
        System.out.println("Next are : ");
        for (int i: nextToDeliver) {
            System.out.println(i);
        }
        while (nextElem != null && nextElem.getOriginalSequenceNumber() == next) {
            considered.remove();
            upperLayer.deliver(nextElem);
            nextElem = considered.peek();
            next++;
        }
        nextToDeliver.set(oIdx, next);
    }

    @Override
    public void deliver(PacketInfo p) {
        treatBuffer.add(p);
    }

    @Override
    public void sendMessage(PacketInfo p) {
        l.sendMessage(p);
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

    @Override
    public int nextSeqNum() {
        return l.nextSeqNum();
    }
}
