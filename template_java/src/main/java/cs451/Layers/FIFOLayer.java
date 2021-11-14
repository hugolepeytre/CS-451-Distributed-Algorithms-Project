package cs451.Layers;

import cs451.Parsing.Host;
import cs451.Util.PacketInfo;

import java.net.SocketException;
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
    private final PriorityQueue<PacketInfo>[] pending;
    private final int[] nextToDeliver;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public FIFOLayer(int port, List<Host> hosts, LinkLayer upperLayer) throws SocketException {
        this.upperLayer = upperLayer;
        treatBuffer = new LinkedBlockingQueue<>();
        pending = new PriorityQueue[hosts.size()];
        nextToDeliver = new int[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            pending[i] = new PriorityQueue<>(Comparator.comparingInt(PacketInfo::getSequenceNumber));
            nextToDeliver[i] = 1;
        }

        running.set(true);
        this.l = new URBLayer(port, hosts, this);
        new Thread(this::treatLoop).start();
    }

    private void treatLoop() {
        while (running.get()) {
            try {
                PacketInfo next = treatBuffer.poll(BLOCK_TIME, TimeUnit.MILLISECONDS);
                if (next != null) {
                    System.out.println("recivède : " + next.getSequenceNumber() + "|" + next.getSenderId() + "|" + next.getOriginalSenderId() + "|");
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
        int next = nextToDeliver[oIdx];
        while (nextElem != null && nextElem.getSequenceNumber() == next) {
            considered.remove();
            upperLayer.deliver(nextElem);
            nextElem = considered.peek();
            next++;
        }
        nextToDeliver[oIdx] = next;
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
}
