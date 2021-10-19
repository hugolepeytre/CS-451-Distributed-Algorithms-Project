package cs451;

import java.net.InetAddress;
import java.net.SocketException;

public class DummyLayer extends LinkLayer{
    // Dummy layer for submission 1, only logs
    private PerfectLink l;

    public DummyLayer(int receivePort, int id, int[] portToID) throws SocketException {
        l = new PerfectLink(receivePort, id, portToID, this);
    }

    @Override
    public void deliver(PacketInfo p, int sender) {
        // TODO
    }

    @Override
    public void sendMessage(int port, InetAddress address, int packetNumber, String message) {
        // TODO
    }

    @Override
    public void close() {
        // TODO
    }
}
