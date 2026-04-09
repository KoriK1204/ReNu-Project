import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Path projectRoot = Paths.get("..").toAbsolutePath().normalize();

        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/products", new ProductsHandler());
        server.createContext("/api/cart", new CartHandler());
        server.createContext("/", new StaticFileHandler(projectRoot));

        server.setExecutor(null);
        System.out.println("Java backend running on http://localhost:" + port);
        server.start();
    }

    private static void addCorsHeaders(Headers headers) {
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readRequestBody(exchange.getRequestBody());
            Map<String, String> form = parseJson(body);
            String username = form.getOrDefault("username", "");
            String password = form.getOrDefault("password", "");

            if (username.equals("admin") && password.equals("password")) {
                String response = "{\"success\":true,\"token\":\"demo-token\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                String response = "{\"success\":false,\"message\":\"Invalid credentials\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(401, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.getResponseBody().close();
        }
    }

    private static class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String response = "["
                    + "{\"id\":1,\"name\":\"Sample Product A\",\"price\":29.99},"
                    + "{\"id\":2,\"name\":\"Sample Product B\",\"price\":49.99},"
                    + "{\"id\":3,\"name\":\"Sample Product C\",\"price\":19.99}"
                    + "]";
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }

    private static class CartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String response = "{\"success\":true,\"message\":\"Cart updated\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        private final Path root;

        StaticFileHandler(Path root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/") || requestPath.isEmpty()) {
                requestPath = "/index.html";
            }
            Path filePath = root.resolve(requestPath.substring(1)).normalize();
            if (!filePath.startsWith(root) || Files.isDirectory(filePath) || !Files.exists(filePath)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            exchange.getResponseHeaders().add("Content-Type", mimeType);
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private static String readRequestBody(InputStream inputStream) throws IOException {
        byte[] data = inputStream.readAllBytes();
        return new String(data, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
            for (String item : json.split(",")) {
                String[] pair = item.split(":", 2);
                if (pair.length == 2) {
                    String key = pair[0].trim().replaceAll("^\"|\"$", "");
                    String value = pair[1].trim().replaceAll("^\"|\"$", "");
                    map.put(key, value);
                }
            }
        }
        return map;
    }
}
