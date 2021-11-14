package cs451;

import cs451.Layers.DummyLayer;
import cs451.Layers.LinkLayer;
import cs451.Parsing.Host;
import cs451.Parsing.Parser;

import java.net.SocketException;
import java.util.List;

// ./run.sh --id 1 --hosts ../config_files/hosts.txt --output ../config_files/outputs/1.txt ../config_files/configs/perfect_link.txt
// ./stress.py -r ../template_java/run.sh -t perfect -l ../template_java/stress -p 2 -m 3
// ./stress.py -r ../template_java/run.sh -t fifo -l ../template_java/stress -p 10 -m 200
// TODO : Try to not flood network : Adjust resend time to host response time.
//  (In UDP, for each host wait until timer, double timer when resend, and half timer when receive)
//  Also when requesting new packets, maybe do it sequentially and delete retransmit loop

// TODO : Redo correctness tests (running fifo_validation)
// TODO : Change output format before submission, check restore stress.py

// TODO : For LCB, augment packet size
// TODO : For LCB, adjust Packet size and thus packet group size to number of hosts (payload size)

// Hyperparameters : grouping size, processes, messages, tc.py (0/1)
// Benchmarks :
//      Grouping 4 :
//          3, 10000 :
//          5, 10000
//          10, 1000
//          10, 5000
//          20, 100
//      Grouping 32 :
//          3, 10000 :
//          5, 10000
//          10, 1000
//          10, 5000
//          20, 100
//      Grouping 64 :
//          3, 10000 :
//          5, 10000 :
//          10, 1000 :
//          10, 5000 :
//          20, 100 :
//      Grouping 128 :
//          3, 10000 : 23, 6
//          5, 10000 : 77, 28
//          10, 1000 : 79, 33
//          10, 5000 : 450, 189
//          20, 100 : 48, 35
//      Grouping 512 :
//          3, 10000 : 8, 6
//          5, 10000 : 29, 20
//          10, 1000 : 34, 25
//          10, 5000 :
//          20, 100 :
//      Grouping 1024 : (Highest possible packet size for FIFO)
//          3, 10000 : 6, 4
//          5, 10000 :
//          10, 1000 : 18, 16
//          10, 5000 :
//          20, 100 :
public class Main {
    private static LinkLayer link;
    private static int nMessages;
    private static List<Host> hosts;

    private static int id;
    private static int port;

    private static String outputPath;

    private static final boolean FIFO_RUN = true;

    public static void main(String[] args) {
        long pid = ProcessHandle.current().pid();
        System.out.print("Parsing. PID : "+ pid + "\n");

        populate(args);
        initSignalHandlers();

        System.out.print("Broadcasting and delivering messages...\n");
        runSender();
    }

    private static void runSender() {
        try {
            if (FIFO_RUN)
                link = new DummyLayer(port, hosts, nMessages, id, outputPath);
            else {
                link = new DummyLayer(port, hosts, nMessages, id, hosts.get(id - 1).getInfluencers(), outputPath);
            }
        } catch (SocketException e) {
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

        outputPath = parser.output();
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
