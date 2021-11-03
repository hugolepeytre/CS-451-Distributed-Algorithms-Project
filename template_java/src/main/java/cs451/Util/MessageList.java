package cs451.Util;

import java.util.ArrayList;
import java.util.TreeSet;

public class MessageList {
    private final int ackLimit;
    private final ArrayList<TreeSet<Integer>> acksList;
    private final ArrayList<Boolean> delivered;

    public MessageList(int nHosts) {
        acksList = new ArrayList<>();
        delivered = new ArrayList<>();
        ackLimit = nHosts/2 + 1;
    }

    public PacketInfo addAck(PacketInfo p) {
        PacketInfo returnVal = null;
        int seqNum = p.getOriginalSequenceNumber();
        while (acksList.size() < seqNum) {
            acksList.add(null);
            delivered.add(false);
        }

        TreeSet<Integer> acks = acksList.get(seqNum - 1);
        if (acks == null) {
            acks = new TreeSet<>();
            acks.add(p.getTargetId());
            acks.add(p.getSenderId());
            acksList.set(seqNum - 1, acks);
            if (acks.size() >= ackLimit) {
                returnVal = p;
                delivered.set(seqNum - 1, true);
            }
        }
        else {
            acks.add(p.getSenderId());
            if (acks.size() >= ackLimit && !delivered.get(seqNum - 1)) {
                returnVal = p;
                delivered.set(seqNum - 1, true);
            }
        }
        return returnVal;
    }

    public boolean wasForwarded(PacketInfo p) {
        return p.getOriginalSequenceNumber() <= acksList.size()
                && acksList.get(p.getOriginalSequenceNumber() - 1) != null;
    }

    // Assumes messages are sent ordered by sequence number
    public void addMessage(PacketInfo p) {
        TreeSet<Integer> s = new TreeSet<>();
        s.add(p.getSenderId()); // Process' own senderId
        acksList.add(s);
        delivered.add(false);
    }
}
