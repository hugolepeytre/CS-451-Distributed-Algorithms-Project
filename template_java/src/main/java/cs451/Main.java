package cs451;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static cs451.Constants.PORTS_WIDTH;
import static cs451.Constants.PORTS_BEGIN;

// ./run.sh --id 1 --hosts ../config_files/hosts.txt --output ../config_files/outputs/1.txt ../config_files/configs/perfect_link.txt

public class Main {
    private static LinkLayer link;

    private static void handleSignal() {
        System.out.println("Immediately stopping network packet processing.");
        System.out.println("Writing output.");
        link.close();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::handleSignal));
    }

    public static void main(String[] args) {
        System.out.print("Parsing\n");
        Parser parser = new Parser(args);
        parser.parse();

        long pid = ProcessHandle.current().pid();
        System.out.print("Creating process. PID : " + pid + "\n");
        int id = parser.myId();
        String outputPath = parser.output();

        String[] instructions = parser.instructions().split(" ");
        int n = Integer.parseInt(instructions[0]);
        int receiverId = Integer.parseInt(instructions[1]);
        boolean isReceiver = receiverId == id;
        InetAddress receiverAddress = null;

        int[] portToID = new int[PORTS_WIDTH];
        for (Host host: parser.hosts()) {
            int hostId = host.getId();
            portToID[host.getPort() - PORTS_BEGIN] = host.getId();
            if (hostId == receiverId) try {
                receiverAddress = InetAddress.getByName(host.getIp());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        initSignalHandlers();
        System.out.print("Broadcasting and delivering messages...\n");
        if (isReceiver) {
            runReceiver(pid, parser, id, portToID, outputPath);
        }
        else {
            runSender(parser, id, n, receiverId, portToID, receiverAddress, outputPath);
        }


    }

    private static void runSender(Parser parser, int id, int n, int receiverId, int[] portToID, InetAddress ad, String outputPath) {
        int sendPort = parser.hosts().get(receiverId - 1).getPort();
        int port = parser.hosts().get(id - 1).getPort();
        try {
            link = new DummyLayer(port, id, portToID, outputPath);
            for (int i = 1; i <= n; i++) {
                link.sendMessage(sendPort, ad, i, "Payload lol");
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (!link.isDone()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        link.close();
    }

    private static void runReceiver(long pid, Parser parser, int id, int[] portToID, String outputPath) {
//        writePid(pid);
        int port = parser.hosts().get(id - 1).getPort();
        try {
            link = new DummyLayer(port, id, portToID, outputPath);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static void writePid(long pid) {
        try {
            File pidFile = new File("../config_files/pid.txt");
            pidFile.createNewFile();
            FileWriter f2 = new FileWriter(pidFile, false);
            f2.write(Long.toString(pid));
            f2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
