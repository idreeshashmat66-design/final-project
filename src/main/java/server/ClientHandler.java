package server;

import controller.ClientController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements HttpHandler {
    private final ClientController controller = new ClientController();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        String method = exchange.getRequestMethod();
        if (method.equalsIgnoreCase("OPTIONS")) {
            sendResponse(exchange, 204, "");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        System.out.println("Request: " + method + " " + path); // debug

        // ---------- SPECIAL ROUTES (must be before generic CRUD) ----------
        // GET /api/stats
        if (method.equalsIgnoreCase("GET") && path.equals("/api/stats")) {
            sendJson(exchange, 200, controller.getStats());
            return;
        }
        // GET /api/clients/recent?limit=5
        if (method.equalsIgnoreCase("GET") && path.equals("/api/clients/recent")) {
            int limit = 5;
            if (query != null && query.startsWith("limit=")) {
                try { limit = Integer.parseInt(query.substring(6)); } catch (NumberFormatException e) {}
            }
            sendJson(exchange, 200, controller.getRecentClients(limit));
            return;
        }
        // GET /api/clients/export/csv
        if (method.equalsIgnoreCase("GET") && path.equals("/api/clients/export/csv")) {
            String csv = controller.exportCsv();
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=clients.csv");
            sendResponse(exchange, 200, csv);
            return;
        }
        // GET /api/clients/paginated?page=0&size=10&sort=name&order=asc
        if (method.equalsIgnoreCase("GET") && path.equals("/api/clients/paginated")) {
            int page = 0, size = 10;
            String sort = "name", order = "asc";
            if (query != null) {
                for (String pair : query.split("&")) {
                    String[] kv = pair.split("=");
                    if (kv.length == 2) {
                        switch (kv[0]) {
                            case "page": page = Integer.parseInt(kv[1]); break;
                            case "size": size = Integer.parseInt(kv[1]); break;
                            case "sort": sort = kv[1]; break;
                            case "order": order = kv[1]; break;
                        }
                    }
                }
            }
            sendJson(exchange, 200, controller.getClientsPaginated(page, size, sort, order));
            return;
        }
        // POST /api/clients/bulk/delete
        if (method.equalsIgnoreCase("POST") && path.equals("/api/clients/bulk/delete")) {
            String body = readBody(exchange);
            sendJson(exchange, 200, controller.bulkDelete(body));
            return;
        }
        // PUT /api/clients/bulk/status
        if (method.equalsIgnoreCase("PUT") && path.equals("/api/clients/bulk/status")) {
            String body = readBody(exchange);
            sendJson(exchange, 200, controller.bulkUpdateStatus(body));
            return;
        }

        // ---------- STANDARD CRUD ROUTES ----------
        // /api/clients or /api/clients/{id}
        if (path.startsWith("/api/clients")) {
            String[] parts = path.split("/");
            boolean hasId = parts.length >= 4 && parts[3].matches("\\d+");
            int id = hasId ? Integer.parseInt(parts[3]) : -1;

            String response;
            int statusCode = 200;

            try {
                switch (method.toUpperCase()) {
                    case "GET":
                        if (hasId) {
                            response = controller.getClientById(id);
                        } else if (query != null && query.startsWith("q=")) {
                            String keyword = query.substring(2);
                            response = controller.searchClients(keyword);
                        } else {
                            response = controller.getAllClients();
                        }
                        break;
                    case "POST":
                        response = controller.addClient(readBody(exchange));
                        statusCode = 201;
                        break;
                    case "PUT":
                        if (!hasId) {
                            statusCode = 400;
                            response = "{\"error\":\"ID required\"}";
                        } else {
                            response = controller.updateClient(id, readBody(exchange));
                        }
                        break;
                    case "DELETE":
                        if (!hasId) {
                            statusCode = 400;
                            response = "{\"error\":\"ID required\"}";
                        } else {
                            response = controller.deleteClient(id);
                        }
                        break;
                    default:
                        statusCode = 405;
                        response = "{\"error\":\"Method not allowed\"}";
                }
            } catch (Exception e) {
                statusCode = 500;
                response = "{\"error\":\"Internal server error\"}";
                e.printStackTrace();
            }
            sendJson(exchange, statusCode, response);
            return;
        }

        // ---------- STATIC FILES (HTML, CSS, JS) ----------
        serveStaticFile(exchange, path);
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        sendResponse(exchange, statusCode, json);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
        String staticDir = "src/main/resources/static/frontend";
        if (path.equals("/")) path = "/index.html";
        java.nio.file.Path filePath = java.nio.file.Paths.get(staticDir + path);
        if (java.nio.file.Files.exists(filePath) && !java.nio.file.Files.isDirectory(filePath)) {
            byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
            String mime = getMimeType(path);
            exchange.getResponseHeaders().set("Content-Type", mime);
            sendResponse(exchange, 200, new String(bytes, StandardCharsets.UTF_8));
        } else {
            String notFound = "<h1>404 Not Found</h1>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            sendResponse(exchange, 404, notFound);
        }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        return "application/octet-stream";
    }
}