package cs451.Parsing;

import cs451.Util.Constants;

import java.net.InetAddress;
import java.util.List;

public class Parser {

    private final String[] args;
    private IdParser idParser;
    private HostsParser hostsParser;
    private OutputParser outputParser;
    private ConfigParser configParser;

    public Parser(String[] args) {
        this.args = args;
    }

    public void parse() {
        long pid = ProcessHandle.current().pid();

        idParser = new IdParser();
        hostsParser = new HostsParser();
        outputParser = new OutputParser();
        configParser = new ConfigParser();

        int argsNum = args.length;
        if (argsNum != Constants.ARG_LIMIT_CONFIG) {
            help();
        }

        if (!idParser.populate(args[Constants.ID_KEY], args[Constants.ID_VALUE])) {
            help();
        }

        if (!hostsParser.populate(args[Constants.HOSTS_KEY], args[Constants.HOSTS_VALUE])) {
            help();
        }

        if (!hostsParser.inRange(idParser.getId())) {
            help();
        }

        if (!outputParser.populate(args[Constants.OUTPUT_KEY], args[Constants.OUTPUT_VALUE])) {
            help();
        }

        if (!configParser.populate(args[Constants.CONFIG_VALUE])) {
            help();
        }
    }

    private void help() {
        System.err.println("Usage: ./run.sh --id ID --hosts HOSTS --output OUTPUT CONFIG");
        System.exit(1);
    }

    public int myId() {
        return idParser.getId();
    }

    public List<Host> hosts() {
        return hostsParser.getHosts();
    }

    public String output() {
        return outputParser.getPath();
    }

    public String config() { return configParser.getPath(); }

    public String[] instructions(String step) {
        if (step.equals("perfect")) {
            return configParser.getInstructions().trim().split(" ");
        }
        else if (step.equals("fifo")) {
            return configParser.getInstructions().trim().split(" ");
        }
        else throw new RuntimeException("Wrong config parsing instruction");
    }

    public InetAddress getAddress(int id) {
        return hostsParser.getAddress(id);
    }

    public int getPort(int id) {
        return hostsParser.getPort(id);
    }
}
