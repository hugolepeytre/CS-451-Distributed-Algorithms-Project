package cs451.Util;

import java.io.*;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PacketInfo implements Serializable {
    private final int senderId;

    private final int targetId;

    private final int originalSenderId;
    private final int sequenceNumber;

    private final boolean isAck;
    private final String payload;
    private int[] vectorClock;

    private PacketInfo(int senderId, int originalSenderId, int targetId, int sequenceNumber,
                      boolean isAck, String payload, int[] vectorClock) {
        this.senderId = senderId;
        this.originalSenderId = originalSenderId;
        this.targetId = targetId;
        this.sequenceNumber = sequenceNumber;
        this.isAck = isAck;
        this.payload = payload;
        this.vectorClock = vectorClock;
    }

    public static PacketInfo newPacket(int senderId, int targetId, int sequenceNumber, String payload, int[] vectorClock) {
        return new PacketInfo(senderId, senderId, targetId, sequenceNumber, false, payload, vectorClock);
    }

    public static PacketInfo fromPacket(byte[] data) {
        // Serialized as : sender ID, target ID, originalSenderId, seqNum
        // payloadLength, payload, boolean hasVclock, vclockLength, vectorClock
        int senderId = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
        int targetId = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).getInt();
        int originalSenderId = ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 12)).getInt();
        int sequenceNumber = ByteBuffer.wrap(Arrays.copyOfRange(data, 12, 16)).getInt();

        int payloadLength = ByteBuffer.wrap(Arrays.copyOfRange(data, 16, 20)).getInt();
        String payload = new String(Arrays.copyOfRange(data, 20, 20 + payloadLength), StandardCharsets.UTF_8);

        boolean isAck = data[20 + payloadLength] == 1;
        boolean hasVClock = data[21 + payloadLength] == 1;
        int[] vectorClock = null;
        if (hasVClock) {
            int vClockLength = ByteBuffer.wrap(Arrays.copyOfRange(data, 22, 26)).getInt();
            vectorClock = new int[vClockLength];
            for (int i = 0; i < vClockLength; i++) {
                vectorClock[i] = ByteBuffer.wrap(Arrays.copyOfRange(data, 26 + 4*i, 30 + 4*i)).getInt();
            }
        }

        return new PacketInfo(senderId, originalSenderId, targetId, sequenceNumber, isAck, payload, vectorClock);
    }

    public byte[] toPacket() {
        // Serialized as : sender ID, target ID, originalSenderId, seqNum
        // payloadLength, payload, boolean hasVclock, vclockLength, vectorClock
        byte[] payloadB = payload.getBytes(StandardCharsets.UTF_8);
        int payloadBLength = payloadB.length;
        boolean hasVClock = vectorClock != null;

        int bufLength = 4*4 + 4 + payloadBLength + 2*1;// 4 bytes per int, 4 for p_length, payload, 2 booleans
        if (hasVClock) {
            bufLength += 4 + 4*vectorClock.length; // 4 bytes per length, then vector clock
        }
        byte[] buf = new byte[bufLength];

        System.arraycopy(ByteBuffer.allocate(4).putInt(senderId).array(), 0, buf,0,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(targetId).array(), 0, buf,4,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(originalSenderId).array(), 0, buf,8,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(sequenceNumber).array(), 0, buf,12,4);

        System.arraycopy(ByteBuffer.allocate(4).putInt(payloadBLength).array(), 0, buf,16,4);
        System.arraycopy(payloadB, 0, buf,20, payloadBLength);

        byte[] hasVClockB = {(byte) (hasVClock ? 1 : 0 )};
        byte[] isAckB = {(byte) (isAck ? 1 : 0 )};
        System.arraycopy(isAckB, 0, buf,20 + payloadBLength,1);
        System.arraycopy(hasVClockB, 0, buf,21 + payloadBLength,1);
        if (hasVClock) {
            System.arraycopy(ByteBuffer.allocate(4).putInt(vectorClock.length).array(), 0, buf,22 + payloadBLength,4);
            for (int i = 0; i < vectorClock.length; i++) {
                System.arraycopy(ByteBuffer.allocate(4).putInt(vectorClock[i]).array(), 0, buf,26 + payloadBLength + 4*i,4);
            }
        }
        return buf;
    }

    public PacketInfo getACK() {
        return new PacketInfo(targetId, originalSenderId, senderId, sequenceNumber, true, payload, vectorClock);
    }

    public PacketInfo newDestination(int id) {
        return new PacketInfo(senderId, originalSenderId, id, sequenceNumber, false,  payload, vectorClock);
    }

    public PacketInfo becomeSender() {
        return new PacketInfo(targetId, originalSenderId, 0, sequenceNumber, false, payload, vectorClock);
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