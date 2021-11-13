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
// ./stress.py -r ../template_java/run.sh -t fifo -l ../template_java/stress -p 10 -m 200
// TODO : If need to improve speed :
//  - Try 1 - Create byte buffers in other threads so UDP send is the bottleneck
//  - Try 2 - Group packets in separate thread
//  - Try 3 - Group packets in same thread (both at the same time)
//  - Profile
// TODO : Change output format before submission, check restore stress.py
// Benchmarks without grouping : no tc : 10,200 - 0:15; with tc : 10,200 - 2:30
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
                PacketInfo toSend = PacketInfo.newPacket(id, 0, i, payload, null);
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
