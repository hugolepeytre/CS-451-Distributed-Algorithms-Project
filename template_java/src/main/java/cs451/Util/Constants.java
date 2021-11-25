package cs451.Util;

public class Constants {
    public static final int ARG_LIMIT_CONFIG = 7;

    // indexes for id
    public static final int ID_KEY = 0;
    public static final int ID_VALUE = 1;

    // indexes for hosts
    public static final int HOSTS_KEY = 2;
    public static final int HOSTS_VALUE = 3;

    // indexes for output
    public static final int OUTPUT_KEY = 4;
    public static final int OUTPUT_VALUE = 5;

    // indexes for config
    public static final int CONFIG_VALUE = 6;

    public static final int MAX_UDP_PAYLOAD_SIZE = 60_000;
    public static int PACKET_GROUP_SIZE = 1024;
    public static int MAX_PACKET_SIZE = 50; // 26 bytes constant size, 24 bytes for payload
    public static int BUF_SIZE = PACKET_GROUP_SIZE*MAX_PACKET_SIZE + 4;

    // Magic numbers to fine-tune
    public static final int BLOCK_TIME = 10;
    public static final int BASE_RESET_MILLIS = 10;
    public static final int BROADCAST_BATCH_SIZE = 1000;
    public static final int RETRANSMIT_PER_HOST = 5*PACKET_GROUP_SIZE;

    public static void setGroupSize(int nHosts) {
        MAX_PACKET_SIZE = 50 + 4*nHosts;
        PACKET_GROUP_SIZE = MAX_UDP_PAYLOAD_SIZE/MAX_PACKET_SIZE;
        BUF_SIZE = PACKET_GROUP_SIZE*MAX_PACKET_SIZE + 4;
    }
}
