package cs451.Layers;

import cs451.Util.PacketInfo;

import java.net.InetAddress;

public interface LinkLayer {
    void deliver(PacketInfo p);

    void sendMessage(PacketInfo p);

    void close();

    boolean isDone();
}
