package server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class AppServer {

    private final int port;
    private HttpServer server;

    public AppServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API routes
        server.createContext("/api/clients", new ClientHandler());

        // Static file route (serves frontend)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Server listening on port " + port);

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            server.stop(2);
        }));
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
