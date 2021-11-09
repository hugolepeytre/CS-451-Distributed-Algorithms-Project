package cs451;

import cs451.Layers.DummyLayer;
import cs451.Layers.LinkLayer;
import cs451.Parsing.Host;
import cs451.Parsing.Parser;
import cs451.Util.PacketInfo;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

// ./run.sh --id 1 --hosts ../config_files/hosts.txt --output ../config_files/outputs/1.txt ../config_files/configs/perfect_link.txt
// ./stress.py -r ../template_java/run.sh -t perfect -l ../template_java/stress -p 2 -m 3
// ./stress.py -r ../template_java/run.sh -t lcausal -l ../template_java/stress -p 5 -m 10
// TODO : Test FIFO/LCB with max process + messages, stress, high traffic mayhem and measure correctness + speed
// TODO : If need to improve speed :
//  - wait longer to resend to slow hosts (PerfectLink)
//  - Have a separate socket for sending
//  - Group packets. Probably in UDPLink directly
//  - profile (Improve Packet serialization ?)
//  - Garbage collect (URB, FIFO, LCB)
// TODO : Review all used data structures and make sure they make sense
// TODO : Removed original sequence number, check that everything still works (in particular, do packets resent to sender
//  by URB get treated differently from ACKs sent to them)
// TODO : Does performance testing have network delays ? At least no processes are crashed/delayed
public class Main {
    private static LinkLayer link;
    private static int nMessages;
    private static List<Host> hosts;

    private static int id;
    private static int port;
    private static InetAddress address;

    private static String outputPath;

    private static final boolean FIFO_RUN = true;

    public static void main(String[] args) {
        long pid = ProcessHandle.current().pid();
        System.out.print("Parsing. PID : "+ pid + "\n");

        populate(args);
        createOutputFile(outputPath);
        initSignalHandlers();

        System.out.print("Broadcasting and delivering messages...\n");
        runSender();
    }

    private static void runSender() {
        try {
            if (FIFO_RUN)
                link = new DummyLayer(port, hosts, outputPath);
            else {
                link = new DummyLayer(port, hosts, hosts.get(id - 1).getInfluencers(), outputPath);
            }
            for (int i = 1; i <= nMessages; i++) {
                if (!FIFO_RUN) {
                    Thread.sleep(100); // Used to test LCB
                }
                String payload = "";
                PacketInfo toSend = PacketInfo.newPacket(id, port, address,
                        0, 0, null, i, payload, null);
                link.sendMessage(toSend);
            }
        } catch (SocketException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void populate(String[] args) {
        Parser parser = new Parser(args);
        parser.parse();
        String[] instructions;
        if (FIFO_RUN)
            instructions = parser.instructions("fifo");
        else {
            instructions = parser.instructions("lcb");
            parser.populateCausality(instructions);
        }

        nMessages = Integer.parseInt(instructions[0].trim());
        hosts = parser.hosts();

        id = parser.myId();
        port = parser.getPort(id);
        address = parser.getAddress(id);

        outputPath = parser.output();
    }

    private static void createOutputFile(String outputPath) {
        try {
            File out = new File(outputPath);
            out.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleSignal() {
        System.out.println("Immediately stopping network packet processing.");
        System.out.println("Writing output.");
        link.close();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::handleSignal));
    }
}
