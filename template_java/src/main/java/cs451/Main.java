package cs451;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static cs451.Constants.PORTS_WIDTH;
import static cs451.Constants.PORTS_BEGIN;

// TODO : Test using stress
// TODO : Benchmark ?
// TODO : Check Inet Address parsing
// TODO : Concurrency
// Question : Does my perfect link have to be able to listen and send in parallel ?
// Question : Should I log two messages with the same payload if they are meant to be different ?
// Question : Inet Address parsing ?
// Question : Should I be able to listen for messages and send in parallel ? What about concurrency, deadlocks... ?
public class Main {
    private static PerfectLink link;

    private static void handleSignal() {
        System.out.println("Immediately stopping network packet processing.");
        System.out.println("Writing output.");
        link.writeOutput();
        link.close();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::handleSignal));
    }

    public static void main(String[] args) {
        System.out.println("Parsing\n");
        Parser parser = new Parser(args);
        parser.parse();

        System.out.println("Creating process\n");
        long pid = ProcessHandle.current().pid();
        int id = parser.myId();
        String[] instructions = parser.instructions().split(" ");
        int receiver = Integer.parseInt(instructions[0]);
        int n = Integer.parseInt(instructions[1]);
        boolean isReceiver = receiver == id;
        int[] portToID = new int[PORTS_WIDTH];
        for (Host host: parser.hosts()) {
            portToID[host.getPort() - PORTS_BEGIN] = host.getId();
        }

        initSignalHandlers();
        System.out.println("Broadcasting and delivering messages...\n");
        if (isReceiver) {
            runReceiver(pid, parser, id, portToID);
        }
        else {
            runSender(parser, id, n, receiver, portToID);
        }


    }

    private static void runSender(Parser parser, int id, int n, int receiverId, int[] portToID) {
        int sendPort = parser.hosts().get(receiverId - 1).getPort();
        int port = parser.hosts().get(id - 1).getPort();
        try {
            link = new PerfectLink(port, id, portToID);
            for (int i = 1; i <= n; i++) {
                link.sendMessage(Integer.toString(i), sendPort);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        link.close();
    }

    private static void runReceiver(long pid, Parser parser, int id, int[] portToID) {
        deletePreviousOutputs();
        writePid(pid);
        int port = parser.hosts().get(id - 1).getPort();

        try {
            link = new PerfectLink(port, id, portToID);
            while (true) {
                link.listen(1000);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        link.close();
    }

    private static void deletePreviousOutputs() {
        File dir = new File("../config_files/outputs");
        for(File file: dir.listFiles())
            if (!file.isDirectory())
                file.delete();
        File pidFile = new File("../config_files/pid.txt");
        pidFile.delete();
    }

    private static void writePid(long pid) {
        Path path = Paths.get("../config_files/pid.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,StandardOpenOption.CREATE)) {
            writer.write(Long.toString(pid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
