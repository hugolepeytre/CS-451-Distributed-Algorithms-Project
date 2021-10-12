package cs451;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

abstract class PerfectLink {
    private ArrayList<String> log;
    private int id;
    private String output_path;

    public PerfectLink(int id) {
        log = new ArrayList<>();
        this.id = id;
        output_path = "../config_files/outputs/" + id + ".txt";
    }

    public void writeOutput() {
        Path path = Paths.get(output_path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND,StandardOpenOption.CREATE)) {
            for (String line : log) {
                writer.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log = new ArrayList<>();
    }

    protected void addToLog(String l) {
        log.add(l);
    }

    abstract public void run();
}
