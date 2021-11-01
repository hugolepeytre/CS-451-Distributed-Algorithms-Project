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
// ./stress.py -r ../template_java/run.sh -t perfect -l ../template_java/stress -p 3 -m 2\
// TODO : Test URB while launching a minority of processes
// TODO : Test URB with stress.py
// TODO : Test URB with tc.py on VM
// TODO : Print timestamps for message broadcasts and deliveries and measure URB with 9 processes and 100 messages
// TODO : Adapt Main and Dummy to FIFO
// TODO : Test FIFO while launching a minority of processes
// TODO : Test FIFO with stress.py
// TODO : Test FIFO with tc.py on VM
// TODO : Print timestamps for message broadcasts and deliveries and measure FIFO with 9 processes and 100 messages
// TODO : Profile

public class Main {
    private static LinkLayer link;
    private static int nMessages;
    private static List<Host> hosts;

    private static int id;
    private static int port;
    private static InetAddress address;

    private static String outputPath;

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
            link = new DummyLayer(port, hosts, outputPath);
            for (int i = 1; i <= nMessages; i++) {
                String payload = "test";
                PacketInfo toSend = new PacketInfo(id, port, address,
                        0, 0, null, link.nextSeqNum(), i, payload);
                link.sendMessage(toSend);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static void populate(String[] args) {
        Parser parser = new Parser(args);
        parser.parse();
        String[] instructions = parser.instructions("fifo");

        nMessages = Integer.parseInt(instructions[0]);
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
