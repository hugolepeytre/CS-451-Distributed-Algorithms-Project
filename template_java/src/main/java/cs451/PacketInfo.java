package cs451;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

public class PacketInfo {
    private static final int INT_LENGTH_BYTES = 4;
    private final int packetNumber;
    private final InetAddress address;
    private final int port;
    private final String message;
    private final byte[] payload;

    public InetAddress getAddress() {
        return address;
    }

    public int getPacketNumber() {
        return packetNumber;
    }

    public int getPort() {
        return port;
    }

    public String getMessage() {
        return message;
    }

    public DatagramPacket getSendPacket() {
        return new DatagramPacket(payload, payload.length, address, port);
    }

    public PacketInfo(int port, InetAddress address, int packetNumber, String message) {
        this.packetNumber = packetNumber;
        this.address = address;
        this.port = port;
        this.message = message;

        byte[] string = message.getBytes();
        byte[] seqNum = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(packetNumber).array();
        byte[] pl = new byte[INT_LENGTH_BYTES + string.length];
        System.arraycopy(string, 0, pl, INT_LENGTH_BYTES, string.length);
        System.arraycopy(seqNum, 0, pl, 0, INT_LENGTH_BYTES);
        this.payload = pl;
    }

    public PacketInfo(int port, InetAddress address, byte[] payload) {
        this.port = port;
        this.address = address;
        message = new String(payload, INT_LENGTH_BYTES, payload.length - INT_LENGTH_BYTES);
        packetNumber = ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, INT_LENGTH_BYTES))
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        this.payload = payload;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final PacketInfo other = (PacketInfo) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }

        if (this.packetNumber != other.packetNumber) {
            return false;
        }

        if (this.port != other.port) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(packetNumber, port, address);
    }
}
