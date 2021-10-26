package cs451.Util;

import java.util.ArrayList;
import java.util.TreeSet;

public class MessageList {
    private final int ackLimit;
    private final ArrayList<TreeSet<Integer>> acksList;
    private final ArrayList<PacketInfo> messages;

    public MessageList(int nHosts) {
        acksList = new ArrayList<>();
        messages = new ArrayList<>();
        ackLimit = nHosts/2 + 1;
    }

    public PacketInfo addAck(PacketInfo p) {
        PacketInfo returnVal = null;
        while (acksList.size() < p.getSequenceNumber()) {
            acksList.add(null);
            messages.add(null);
        }

        int seqNum = p.getSequenceNumber();
        TreeSet<Integer> acks = acksList.get(seqNum - 1);
        if (acks == null) {
            acks = new TreeSet<>();
            acks.add(p.getTargetId());
            acks.add(p.getSenderId());
            acks.add(p.getOriginalSenderId());
            acksList.add(seqNum - 1, acks);
            messages.add(seqNum - 1, p);
            returnVal = p;
        }
        else {
            acks.add(p.getSenderId());
        }
        return returnVal;
    }

    public boolean forwarded(PacketInfo p) {
        return messages.get(p.getSequenceNumber() - 1) == null;
    }

    // Assumes messages are sent ordered by sequence number
    public void addMessage(PacketInfo p) {
        TreeSet<Integer> s = new TreeSet<>();
        s.add(p.getSenderId()); // Process' own senderId
        acksList.add(s);
        messages.add(p);
    }
}
