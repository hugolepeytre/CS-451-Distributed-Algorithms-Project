package cs451.Util;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

//    public static PacketInfo fromPacket(DatagramPacket p) {
//        int l = p.getLength();
//        byte[] data = Arrays.copyOfRange(p.getData(), 0, l);
//        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
//             ObjectInputStream in = new ObjectInputStream(bis)) {
//            return (PacketInfo) in.readObject();
//        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static PacketInfo fromPacket(DatagramPacket p) {
        // Serialized as : sender ID, Port, target ID, Port, originalSenderId, seqNum
        // senderAddress, targetAddress, payloadLength, payload, boolean hasVclock, vclockLength, vectorClock
        int l = p.getLength();
        byte[] data = Arrays.copyOfRange(p.getData(), 0, l);

        int senderId = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
        int senderPort = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).getInt();
        int targetId = ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 12)).getInt();
        int targetPort = ByteBuffer.wrap(Arrays.copyOfRange(data, 12, 16)).getInt();
        int originalSenderId = ByteBuffer.wrap(Arrays.copyOfRange(data, 16, 20)).getInt();
        int sequenceNumber = ByteBuffer.wrap(Arrays.copyOfRange(data, 20, 24)).getInt();

        InetAddress senderAddress = null;
        InetAddress targetAddress = null;
        try {
            senderAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, 24, 28));
            targetAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, 28, 32));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        int payloadLength = ByteBuffer.wrap(Arrays.copyOfRange(data, 32, 36)).getInt();
        String payload = new String(Arrays.copyOfRange(data, 36, 36 + payloadLength), StandardCharsets.UTF_8);

        boolean isAck = data[36 + payloadLength] == 1;
        boolean hasVClock = data[37 + payloadLength] == 1;
        int[] vectorClock = null;
        if (hasVClock) {
            int vClockLength = ByteBuffer.wrap(Arrays.copyOfRange(data, 38, 42)).getInt();
            vectorClock = new int[vClockLength];
            for (int i = 0; i < vClockLength; i++) {
                vectorClock[i] = ByteBuffer.wrap(Arrays.copyOfRange(data, 42 + 4*i, 46 + 4*i)).getInt();
            }
        }

        return new PacketInfo(senderId, senderPort, senderAddress, originalSenderId,
                targetId, targetPort, targetAddress, sequenceNumber,
                isAck, payload, vectorClock);
    }

//    public DatagramPacket toPacket() {
//        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
//            ObjectOutputStream out = new ObjectOutputStream(bos);
//            out.writeObject(this);
//            out.flush();
//            byte[] buf = bos.toByteArray();
//            return new DatagramPacket(buf, buf.length, targetAddress, targetPort);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public DatagramPacket toPacket() {
        // Serialized as : sender ID, Port, target ID, Port, originalSenderId, seqNum
        // senderAddress, targetAddress, payloadLength, payload, boolean hasVclock, vclockLength, vectorClock
        byte[] payloadB = payload.getBytes(StandardCharsets.UTF_8);
        int payloadBLength = payloadB.length;
        boolean hasVClock = vectorClock != null;

        int bufLength = 6*4 + 2*4 + 4 + payloadBLength + 2*1;// 4 bytes per int, per address, 4 for length, payload, 2 booleans
        if (hasVClock) {
            bufLength += 4 + 4*vectorClock.length; // 4 bytes per length, then vector clock
        }
        byte buf[] = new byte[bufLength];

        System.arraycopy(ByteBuffer.allocate(4).putInt(senderId).array(), 0, buf,0,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(senderPort).array(), 0, buf,4,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(targetId).array(), 0, buf,8,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(targetPort).array(), 0, buf,12,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(originalSenderId).array(), 0, buf,16,4);
        System.arraycopy(ByteBuffer.allocate(4).putInt(sequenceNumber).array(), 0, buf,20,4);
        System.arraycopy(senderAddress.getAddress(), 0, buf,24,4);
        System.arraycopy(targetAddress.getAddress(), 0, buf,28,4);

        System.arraycopy(ByteBuffer.allocate(4).putInt(payloadBLength).array(), 0, buf,32,4);
        System.arraycopy(payloadB, 0, buf,36, payloadBLength);

        byte[] hasVClockB = {(byte) (hasVClock ? 1 : 0 )};
        byte[] isAckB = {(byte) (isAck ? 1 : 0 )};
        System.arraycopy(isAckB, 0, buf,36 + payloadBLength,1);
        System.arraycopy(hasVClockB, 0, buf,37 + payloadBLength,1);
        if (hasVClock) {
            System.arraycopy(ByteBuffer.allocate(4).putInt(vectorClock.length).array(), 0, buf,38 + payloadBLength,4);
            for (int i = 0; i < vectorClock.length; i++) {
                System.arraycopy(ByteBuffer.allocate(4).putInt(vectorClock[i]).array(), 0, buf,42 + payloadBLength + 4*i,4);
            }
        }
        return new DatagramPacket(buf, bufLength, targetAddress, targetPort);
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