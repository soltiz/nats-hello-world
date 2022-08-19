package io.nats.benchmark;

// Author: C. Van Frachem
// Derivated from java-nats-examples/hello-world

import java.util.Arrays;

public class NatsTester {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide a subcommand as first parameter of NatsTester.");
            System.exit(1);
        }
        String arg = args[0];
        String[] modifiedArgs = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (arg) {
                case "init":
                    StreamInit.main(modifiedArgs);
                    break;
                case "reader":
                    StreamReader.main(modifiedArgs);
                    break;
                case "writer":
                    StreamWriter.main(modifiedArgs);
                    break;
                default:
                    System.err.println("Unknown subcommand: '" + arg + "'");
                    System.exit(2);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(99);
        }
        System.exit(0);
    }
}
