package cs451.Parsing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigParser {

    private String path;
    private String instructions;

    public boolean populate(String value) {
        File file = new File(value);
        path = file.getPath();
        Path filepath = Paths.get(path);
        try {
            instructions = Files.readString(filepath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getPath() {
        return path;
    }

    public String getInstructions() { return instructions; }

}
