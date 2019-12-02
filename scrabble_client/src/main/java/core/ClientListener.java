package core;

import core.message.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public abstract class ClientListener extends Listener {
    protected Socket socket;

    public ClientListener(String name) {
        super(name);
    }

    public abstract void onAuthenticate() throws Exception;

    @Override
    public void reset() {
        super.reset();
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start(String ip, int port) throws Exception {
        reset();

        socket = new Socket(ip, port);
        // TODO: this is important but there has to be a better way to write this
        connections.put(socket, null);

        // perform a simple authentication check
        // TODO: Maybe use executor on other threads here?
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                onAuthenticate();
                return null;
            }
        });

        future.get();

        // heartbeat
        Thread t = new Thread(() -> run_heartbeat(socket));
        t.setName("heartbeat");
        t.start();

        // separate thread for connector
        Thread main_t = new Thread(() -> run_socket(socket, t));
        main_t.setName("client thread");
        main_t.start();

        onUserConnect(socket);
    }

    /** Send a message to the server. */
    protected void sendMessage(Message msg) throws IOException {
        super.sendMessage(msg, socket);
    }
}
