package server;

import core.ConnectType;

import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) throws IOException {
        if (!args[0].matches("^[0-9]+$")) {
            System.out.println("Please input a numerical port number as an argument.");
            System.exit(-1);
        }

        int port = Integer.parseInt(args[0]);
        System.out.println("Connecting as port " + port + "...");

        String name = (args.length > 1) ? args[1] : "Server";

        // for internet use
        ScrabbleServerListener server = new ScrabbleServerListener(name,
                ConnectType.INTERNET);
        server.start(port);
    }

}
