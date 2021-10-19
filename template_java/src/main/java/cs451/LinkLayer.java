package cs451;

import java.net.InetAddress;

public abstract class LinkLayer {
    public abstract void deliver(PacketInfo p, int sender);

    public abstract void sendMessage(int port, InetAddress address, int packetNumber, String message);

    public abstract void close();
}
