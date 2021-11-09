package cs451.Util;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class PacketInfo implements Serializable {
    private final int senderId;
    private final int senderPort;
    private final InetAddress senderAddress;

    private final int targetId;
    private final int targetPort;
    private final InetAddress targetAddress;

    private final int originalSenderId;
    private final int sequenceNumber;

    private final boolean isAck;
    private final String payload;
    private int[] vectorClock;

    public PacketInfo(int senderId, int senderPort, InetAddress senderAddress,
                      int originalSenderId,
                      int targetId, int targetPort, InetAddress targetAddress,
                      int sequenceNumber, boolean isAck, String payload,
                      int[] vectorClock) {
        this.senderId = senderId;
        this.senderPort = senderPort;
        this.senderAddress = senderAddress;
        this.originalSenderId = originalSenderId;
        this.targetId = targetId;
        this.targetPort = targetPort;
        this.targetAddress = targetAddress;
        this.sequenceNumber = sequenceNumber;
        this.isAck = isAck;
        this.payload = payload;
        this.vectorClock = vectorClock;
    }

    public static PacketInfo newPacket(int senderId, int senderPort, InetAddress senderAddress,
                      int targetId, int targetPort, InetAddress targetAddress,
                      int sequenceNumber, String payload,
                      int[] vectorClock) {
        return new PacketInfo(senderId, senderPort, senderAddress, senderId, targetId, targetPort, targetAddress,
                sequenceNumber, false, payload, vectorClock);
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
                              originalSenderId,
                              senderId, senderPort, senderAddress,
                              sequenceNumber, true, payload, vectorClock);
    }

    public PacketInfo newDestination(int id, int port, InetAddress address) {
        return new PacketInfo(senderId, senderPort, senderAddress,
                originalSenderId,
                id, port, address,
                sequenceNumber, false,  payload, vectorClock);
    }

    public PacketInfo becomeSender() {
        return new PacketInfo(targetId, targetPort, targetAddress,
                originalSenderId,
                0, 0, null,
                sequenceNumber, false, payload, vectorClock);
    }

    public int getSenderId() {
        return senderId;
    }

    public int getOriginalSenderId() {
        return originalSenderId;
    }

    public int getTargetId() {
        return targetId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
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