package cs451.Util;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class PacketInfo implements Serializable {
    private final int senderId;
    private final int senderPort;
    private final InetAddress senderAddress;

    private final int originalSenderId;
    private final int originalSenderPort;
    private final InetAddress originalSenderAddress;

    private final int targetId;
    private final int targetPort;
    private final InetAddress targetAddress;

    private final int sequenceNumber;
    private final int originalSequenceNumber;

    private final boolean isAck;
    private final String payload;
    private int[] vectorClock;

    // TODO : Only construct byte array once, then reuse
    // TODO : Try constructing byte array by hand and check time gained
    public PacketInfo(int senderId, int senderPort, InetAddress senderAddress,
                      int originalSenderId, int originalSenderPort, InetAddress originalSenderAddress,
                      int targetId, int targetPort, InetAddress targetAddress,
                      int sequenceNumber, int originalSequenceNumber, boolean isAck, String payload,
                      int[] vectorClock) {
        this.senderId = senderId;
        this.senderPort = senderPort;
        this.senderAddress = senderAddress;
        this.originalSenderId = originalSenderId;
        this.originalSenderPort = originalSenderPort;
        this.originalSenderAddress = originalSenderAddress;
        this.targetId = targetId;
        this.targetPort = targetPort;
        this.targetAddress = targetAddress;
        this.sequenceNumber = sequenceNumber;
        this.originalSequenceNumber = originalSequenceNumber;
        this.isAck = isAck;
        this.payload = payload;
        this.vectorClock = vectorClock;
    }

    public PacketInfo(int senderId, int senderPort, InetAddress senderAddress,
                      int targetId, int targetPort, InetAddress targetAddress,
                      int sequenceNumber, int originalSequenceNumber, String payload,
                      int[] vectorClock) {
        this.senderId = senderId;
        this.senderPort = senderPort;
        this.senderAddress = senderAddress;
        this.originalSenderId = senderId;
        this.originalSenderPort = senderPort;
        this.originalSenderAddress = senderAddress;
        this.targetId = targetId;
        this.targetPort = targetPort;
        this.targetAddress = targetAddress;
        this.sequenceNumber = sequenceNumber;
        this.originalSequenceNumber = originalSequenceNumber;
        this.isAck = false;
        this.payload = payload;
        this.vectorClock = vectorClock;
    }

    public static PacketInfo fromPacket(DatagramPacket p) {
        int l = p.getLength();
        byte[] data = Arrays.copyOfRange(p.getData(), 0, l);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return (PacketInfo) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public DatagramPacket toPacket() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            byte[] buf = bos.toByteArray();
            return new DatagramPacket(buf, buf.length, targetAddress, targetPort);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PacketInfo getACK() {
        return new PacketInfo(targetId, targetPort, targetAddress,
                              originalSenderId, originalSenderPort, originalSenderAddress,
                              senderId, senderPort, senderAddress,
                              sequenceNumber, originalSequenceNumber, true, payload, vectorClock);
    }

    public PacketInfo newDestination(int id, int port, InetAddress address) {
        return new PacketInfo(senderId, senderPort, senderAddress,
                originalSenderId, originalSenderPort, originalSenderAddress,
                id, port, address,
                sequenceNumber, originalSequenceNumber, false,  payload, vectorClock);
    }

    public PacketInfo becomeSender(int newSeqNumber) {
        return new PacketInfo(targetId, targetPort, targetAddress,
                originalSenderId, originalSenderPort, originalSenderAddress,
                0, 0, null,
                newSeqNumber, originalSequenceNumber, false, payload, vectorClock);
    }

    public int getSenderId() {
        return senderId;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public InetAddress getSenderAddress() {
        return senderAddress;
    }

    public int getOriginalSenderId() {
        return originalSenderId;
    }

    public int getOriginalSenderPort() {
        return originalSenderPort;
    }

    public InetAddress getOriginalSenderAddress() {
        return originalSenderAddress;
    }

    public int getTargetId() {
        return targetId;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public InetAddress getTargetAddress() {
        return targetAddress;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getOriginalSequenceNumber() {
        return originalSequenceNumber;
    }

    public boolean isAck() {
        return isAck;
    }

    public String getPayload() {
        return payload;
    }

    public boolean compareVectorClock(int[] otherVectorClock) {
        for (int i = 0; i < vectorClock.length; i++) {
            if (otherVectorClock[i] < vectorClock[i])
                return false;
        }
        return true;
    }

    public void setVectorClock(int[] newVClock) {
        vectorClock = newVClock;
    }
}