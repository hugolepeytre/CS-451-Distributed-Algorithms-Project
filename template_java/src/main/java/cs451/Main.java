package cs451;

import cs451.Layers.DummyLayer;
import cs451.Layers.LinkLayer;
import cs451.Parsing.Parser;
import cs451.Util.PacketInfo;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

// ./run.sh --id 1 --hosts ../config_files/hosts.txt --output ../config_files/outputs/1.txt ../config_files/configs/perfect_link.txt
// ./stress.py -r ../template_java/run.sh -t perfect -l ../template_java/stress -p 3 -m 2
// TODO : Change DummyLayer to work with new specs
// TODO : Change parsing to work with new specs
// TODO : Use only originalSeqNumber from URB upwards, find a way to assign new seq numbers
// TODO : Test all my URB code, I was tired

public class Main {
    private static LinkLayer link;
    private static int nMessages;
    private static int nHosts;
    private static boolean isReceiver;

    private static int id;
    private static int port;
    private static InetAddress address;

    private static int receiverId;
    private static int receiverPort;
    private static InetAddress receiverAddress;

    private static String outputPath;

    public static void main(String[] args) {
        long pid = ProcessHandle.current().pid();
        System.out.print("Parsing. PID : "+ pid + "\n");

        populate(args);
        createOutputFile(outputPath);
        initSignalHandlers();

        System.out.print("Broadcasting and delivering messages...\n");
        if (isReceiver) {
            runReceiver();
        }
        else {
            runSender();
        }
    }

    private static void runSender() {
        try {
            link = new DummyLayer(port, nHosts, outputPath);
            for (int i = 1; i <= nMessages; i++) {
                String payload = "test";
                PacketInfo toSend = new PacketInfo(id, port, address, receiverId, receiverPort, receiverAddress, i, payload);
                link.sendMessage(toSend);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static void runReceiver() {
        try {
            link = new DummyLayer(port, nHosts, outputPath);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static void populate(String[] args) {
        Parser parser = new Parser(args);
        parser.parse();
        String[] instructions = parser.instructions("perfect");

        nMessages = Integer.parseInt(instructions[0]);
        nHosts = parser.getNHosts();
        receiverId = Integer.parseInt(instructions[1]);
        receiverPort = parser.getPort(receiverId);
        receiverAddress = parser.getAddress(receiverId);

        id = parser.myId();
        port = parser.getPort(id);
        address = parser.getAddress(id);

        outputPath = parser.output();
        isReceiver = receiverId == id;
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
