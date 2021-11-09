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

    public static final int PORTS_WIDTH = 1000;
    public static final int PORTS_BEGIN = 11000;
    public static final String ACK = "ack";
    public static final long NANOSECS_IN_MS = 1_000_000;
    public static final int BUF_SIZE = 1024;
    public static final int BYTES_IN_INT = 4;

    // Magic numbers to fine-tune
    public static final int BLOCK_TIME = 10;
    public static final int RETRANSMIT_WAIT_TIME = 4000;
    public static final int MAX_RETRANSMIT_PER_HOST = 100;
}
