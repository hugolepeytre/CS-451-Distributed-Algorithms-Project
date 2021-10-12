package cs451;

import java.io.File;
import java.net.SocketException;

public class Main {
    private static PerfectLink link;

    private static void handleSignal() {
        System.out.println("Immediately stopping network packet processing.");
        System.out.println("Writing output.");
        link.writeOutput();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        // Print stuff
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");

        System.out.println("Creating process\n");

        int id = parser.myId();
        String[] instructions = parser.instructions().split(" ");
        int receiver = Integer.parseInt(instructions[0]);
        int n = Integer.parseInt(instructions[1]);
        int[] portToId = new int[1000];
        for (Host host: parser.hosts()) {
            portToId[host.getPort() - 11000] = host.getId();
        }
        boolean isReceiver = receiver == id;

        if (isReceiver) {
            deletePreviousOutputs();
            int port = parser.hosts().get(id - 1).getPort();
            try {
                link = new PerfectLinkReceiver(port, id, portToId);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        else {
            int sendPort = parser.hosts().get(receiver - 1).getPort();
            int port = parser.hosts().get(id - 1).getPort();
            try {
                link = new PerfectLinkSender(sendPort, port, id, n);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        initSignalHandlers();

        System.out.println("Broadcasting and delivering messages...\n");
        link.run();
    }

    private static void deletePreviousOutputs() {
        File dir = new File("../config_files/outputs");
        for(File file: dir.listFiles())
            if (!file.isDirectory())
                file.delete();
    }
}
