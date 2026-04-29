import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class Main {

    static Path projectRoot;

    static Connection getConnection() throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new SQLException("MySQL driver not found", e); }
        Properties cfg = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) { cfg.load(in); }
        catch (IOException e) { throw new SQLException("config.properties not found — copy config.properties.example and fill in your credentials", e); }
        return DriverManager.getConnection(cfg.getProperty("db.url"), cfg.getProperty("db.user"), cfg.getProperty("db.pass"));
    }

    // ── Database initialisation (run on startup) ─────────────────────────────
    static void initDatabase() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // Add specs column
            ResultSet c1 = st.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_name='products' AND column_name='specs' AND table_schema=DATABASE()");
            c1.next(); if (c1.getInt(1) == 0) st.executeUpdate("ALTER TABLE products ADD COLUMN specs TEXT DEFAULT NULL");

            // Add image_urls column
            ResultSet c2 = st.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_name='products' AND column_name='image_urls' AND table_schema=DATABASE()");
            c2.next(); if (c2.getInt(1) == 0) st.executeUpdate("ALTER TABLE products ADD COLUMN image_urls TEXT DEFAULT NULL");

            // Seed specs for all products (only when specs IS NULL, so admin edits are never overwritten)
            String[][] specSeeds = {
                {"iPhone 14 Pro Max - Gold",
                 "{\"chip\":\"A16 Bionic\",\"display\":\"6.7-inch Super Retina XDR ProMotion 120Hz\",\"rearCamera\":\"48MP Main + 12MP Ultra Wide + 12MP 3x Telephoto\",\"frontCamera\":\"12MP TrueDepth\",\"battery\":\"Up to 29 hours video playback\",\"os\":\"iOS 18\",\"has5g\":\"Yes\",\"batteryHealth\":\"91%\",\"cosmetic\":\"Minor surface scratches, screen clear\",\"unlocked\":\"Yes — all carriers\",\"accessories\":\"USB-C cable included\"}"},
                {"iPhone 16 Pro — Neutral Titanium",
                 "{\"chip\":\"A18 Pro\",\"display\":\"6.3-inch Super Retina XDR ProMotion 120Hz\",\"rearCamera\":\"48MP Main + 48MP Ultra Wide + 12MP 5x Telephoto\",\"frontCamera\":\"12MP TrueDepth\",\"battery\":\"Up to 27 hours video playback\",\"os\":\"iOS 18\",\"has5g\":\"Yes\",\"batteryHealth\":\"97%\",\"cosmetic\":\"No visible scratches or marks\",\"unlocked\":\"Yes — all carriers\",\"accessories\":\"USB-C cable included\"}"},
                {"iPhone 15 Pro — Black Titanium",
                 "{\"chip\":\"A17 Pro\",\"display\":\"6.1-inch Super Retina XDR ProMotion 120Hz\",\"rearCamera\":\"48MP Main + 12MP Ultra Wide + 12MP 3x Telephoto\",\"frontCamera\":\"12MP TrueDepth\",\"battery\":\"Up to 23 hours video playback\",\"os\":\"iOS 17\",\"has5g\":\"Yes\",\"batteryHealth\":\"89%\",\"cosmetic\":\"Light scratches on frame, screen clear\",\"unlocked\":\"Yes — all carriers\",\"accessories\":\"USB-C cable included\"}"},
                {"iPhone 17 — Blue",
                 "{\"chip\":\"A19\",\"display\":\"6.3-inch Super Retina XDR ProMotion 120Hz\",\"rearCamera\":\"48MP Fusion + 12MP Ultra Wide\",\"frontCamera\":\"24MP TrueDepth\",\"battery\":\"Up to 26 hours video playback\",\"os\":\"iOS 18\",\"has5g\":\"Yes\",\"batteryHealth\":\"95%\",\"cosmetic\":\"Minor surface marks on glass back\",\"unlocked\":\"Yes — all carriers\",\"accessories\":\"USB-C cable included\"}"},
                {"MacBook Pro M4 — Space Black 14\"",
                 "{\"chip\":\"Apple M4 Pro\",\"display\":\"14.2-inch Liquid Retina XDR ProMotion 120Hz\",\"rearCamera\":\"\",\"frontCamera\":\"12MP Center Stage\",\"battery\":\"Up to 24 hours\",\"os\":\"macOS Sequoia\",\"has5g\":\"No\",\"batteryHealth\":\"96%\",\"cosmetic\":\"No scratches, pristine condition\",\"unlocked\":\"\",\"accessories\":\"96W USB-C Power Adapter included\"}"},
                {"MacBook Air M4 — Midnight 13\"",
                 "{\"chip\":\"Apple M4\",\"display\":\"13.6-inch Liquid Retina\",\"rearCamera\":\"\",\"frontCamera\":\"12MP Center Stage\",\"battery\":\"Up to 18 hours\",\"os\":\"macOS Sequoia\",\"has5g\":\"No\",\"batteryHealth\":\"91%\",\"cosmetic\":\"Light surface scratches on bottom, screen perfect\",\"unlocked\":\"\",\"accessories\":\"30W USB-C Power Adapter included\"}"},
                {"MacBook Pro M3 — Silver 14\"",
                 "{\"chip\":\"Apple M3 Pro\",\"display\":\"14.2-inch Liquid Retina XDR ProMotion 120Hz\",\"rearCamera\":\"\",\"frontCamera\":\"12MP Center Stage\",\"battery\":\"Up to 18 hours\",\"os\":\"macOS Sonoma\",\"has5g\":\"No\",\"batteryHealth\":\"94%\",\"cosmetic\":\"No visible marks, like new condition\",\"unlocked\":\"\",\"accessories\":\"70W USB-C Power Adapter included\"}"},
                {"Apple Watch Ultra 3 — Natural",
                 "{\"chip\":\"S10\",\"display\":\"49mm Always-On Retina LTPO OLED\",\"rearCamera\":\"\",\"frontCamera\":\"\",\"battery\":\"Up to 36 hours\",\"os\":\"watchOS 11\",\"has5g\":\"No\",\"batteryHealth\":\"95%\",\"cosmetic\":\"No scratches, pristine titanium case\",\"unlocked\":\"Yes — all carriers LTE\",\"accessories\":\"Alpine Loop band + USB-C magnetic cable\"}"},
                {"Apple Watch SE 2nd Gen — Midnight",
                 "{\"chip\":\"S8\",\"display\":\"44mm Retina LTPO2 OLED\",\"rearCamera\":\"\",\"frontCamera\":\"\",\"battery\":\"Up to 18 hours\",\"os\":\"watchOS 11\",\"has5g\":\"No\",\"batteryHealth\":\"88%\",\"cosmetic\":\"Minor scuff on case, band in good condition\",\"unlocked\":\"GPS only\",\"accessories\":\"Magnetic charging cable included\"}"},
                {"Apple Watch S11 — Midnight",
                 "{\"chip\":\"S11\",\"display\":\"46mm Always-On Retina LTPO OLED\",\"rearCamera\":\"\",\"frontCamera\":\"\",\"battery\":\"Up to 20 hours\",\"os\":\"watchOS 12\",\"has5g\":\"No\",\"batteryHealth\":\"92%\",\"cosmetic\":\"No visible scratches, like new\",\"unlocked\":\"Yes — GPS + Cellular, all carriers\",\"accessories\":\"Braided Solo Loop + USB-C magnetic cable\"}"},
            };
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE products SET specs = ? WHERE name = ? AND specs IS NULL")) {
                for (String[] s : specSeeds) { ps.setString(1, s[1]); ps.setString(2, s[0]); ps.executeUpdate(); }
            }
            System.out.println("Database initialised.");
        } catch (SQLException e) {
            System.err.println("initDatabase warning: " + e.getMessage());
        }
    }

    // ── Server entry point ───────────────────────────────────────────────────
    public static void main(String[] args) throws IOException {
        int  port = 8080;
        projectRoot = Paths.get("..").toAbsolutePath().normalize();
        initDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/login",          new LoginHandler());
        server.createContext("/api/products",       new ProductsHandler());
        server.createContext("/api/orders",         new OrdersHandler());
        server.createContext("/api/users",          new UsersHandler());
        server.createContext("/api/restock-orders", new RestockHandler());
        server.createContext("/api/stats",          new StatsHandler());
        server.createContext("/api/contact-messages", new ContactMessagesHandler());
        server.createContext("/api/upload-image",    new UploadImageHandler());
        server.createContext("/",                   new StaticFileHandler(projectRoot));
        server.setExecutor(null);
        System.out.println("ReNu Tech server running at http://localhost:" + port);
        server.start();
    }

    // ── Shared helpers ───────────────────────────────────────────────────────
    static void cors(Headers h) {
        h.add("Access-Control-Allow-Origin",  "*");
        h.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
    }

    static void send(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static Map<String, String> queryParams(HttpExchange ex) {
        Map<String, String> map = new LinkedHashMap<>();
        String q = ex.getRequestURI().getQuery();
        if (q == null || q.isEmpty()) return map;
        for (String pair : q.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return map;
    }

    // Flat JSON parser — correctly handles escaped quotes and special characters in values
    static Map<String, String> parseJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return map;
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return map;
        json = json.substring(1, json.length() - 1).trim();

        int i = 0, len = json.length();
        while (i < len) {
            // Skip commas and whitespace between pairs
            while (i < len && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) i++;
            if (i >= len || json.charAt(i) != '"') break;

            // Read key
            i++; // opening quote
            StringBuilder key = new StringBuilder();
            while (i < len && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\' && i + 1 < len) i++; // skip backslash
                key.append(json.charAt(i++));
            }
            i++; // closing quote

            // Skip colon
            while (i < len && (json.charAt(i) == ':' || Character.isWhitespace(json.charAt(i)))) i++;

            // Read value
            String value;
            if (i < len && json.charAt(i) == '"') {
                i++; // opening quote
                StringBuilder val = new StringBuilder();
                while (i < len) {
                    char c = json.charAt(i);
                    if (c == '\\' && i + 1 < len) {
                        char next = json.charAt(i + 1);
                        if      (next == '"')  { val.append('"');  i += 2; }
                        else if (next == '\\') { val.append('\\'); i += 2; }
                        else if (next == 'n')  { val.append('\n'); i += 2; }
                        else                   { val.append(next); i += 2; }
                    } else if (c == '"') {
                        i++; break; // closing quote
                    } else {
                        val.append(c); i++;
                    }
                }
                value = val.toString();
            } else {
                // Number, boolean, or null
                StringBuilder val = new StringBuilder();
                while (i < len && json.charAt(i) != ',' && json.charAt(i) != '}')
                    val.append(json.charAt(i++));
                value = val.toString().trim();
            }
            map.put(key.toString(), value);
        }
        return map;
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    static List<String> splitJsonArray(String json) {
        List<String> items = new ArrayList<>();
        if (json == null) return items;
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return items;
        json = json.substring(1, json.length() - 1).trim();
        int depth = 0, start = 0;
        boolean inStr = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inStr) { i++; continue; }
            if (c == '"') { inStr = !inStr; continue; }
            if (inStr) continue;
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0) {
                    items.add(json.substring(start, i + 1).trim());
                    start = i + 2;
                    i++;
                }
            }
        }
        return items;
    }

    // ── POST /api/login ──────────────────────────────────────────────────────
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            if (!ex.getRequestMethod().equalsIgnoreCase("POST"))   { ex.sendResponseHeaders(405, -1); return; }

            Map<String, String> body = parseJson(readBody(ex));
            String email    = body.getOrDefault("email",    "");
            String password = body.getOrDefault("password", "");

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, name, role, status FROM users WHERE email = ? AND password = ?")) {
                ps.setString(1, email);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if ("suspended".equals(rs.getString("status"))) {
                        send(ex, 403, "{\"success\":false,\"message\":\"Account suspended\"}");
                        return;
                    }
                    send(ex, 200, String.format(
                            "{\"success\":true,\"id\":%d,\"name\":\"%s\",\"role\":\"%s\"}",
                            rs.getInt("id"), esc(rs.getString("name")), esc(rs.getString("role"))));
                } else {
                    send(ex, 401, "{\"success\":false,\"message\":\"Invalid email or password\"}");
                }
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ── /api/products  (GET / POST / PUT / DELETE) ───────────────────────────
    static class ProductsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            switch (ex.getRequestMethod().toUpperCase()) {
                case "GET"    -> get(ex);
                case "POST"   -> post(ex);
                case "PUT"    -> put(ex);
                case "DELETE" -> delete(ex);
                default       -> ex.sendResponseHeaders(405, -1);
            }
        }

        void get(HttpExchange ex) throws IOException {
            Map<String, String> p  = queryParams(ex);
            String idParam   = p.get("id");
            String category  = p.get("category");
            String search    = p.get("search");

            StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
            if (idParam   != null && !idParam.isEmpty())   sql.append(" AND id = ?");
            if (category  != null && !category.isEmpty())  sql.append(" AND category = ?");
            if (search    != null && !search.isEmpty())    sql.append(" AND name LIKE ?");
            sql.append(" ORDER BY id");

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int i = 1;
                if (idParam  != null && !idParam.isEmpty())  ps.setInt(i++, Integer.parseInt(idParam));
                if (category != null && !category.isEmpty()) ps.setString(i++, category);
                if (search   != null && !search.isEmpty())   ps.setString(i++, "%" + search + "%");
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    String imgUrl     = rs.getString("image_url");
                    String specsRaw   = rs.getString("specs");
                    String imgUrlsRaw = rs.getString("image_urls");
                    String specsJson  = (specsRaw   != null && !specsRaw.isEmpty())   ? specsRaw   : "null";
                    String imgUrlsJson= (imgUrlsRaw != null && !imgUrlsRaw.isEmpty()) ? imgUrlsRaw : "null";
                    sb.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"category\":\"%s\",\"condition\":\"%s\",\"costPrice\":%.2f,\"price\":%.2f,\"stock\":%d,\"description\":\"%s\",\"imageUrl\":\"%s\",\"imageUrls\":%s,\"specs\":%s}",
                            rs.getInt("id"),
                            esc(rs.getString("name")),
                            esc(rs.getString("category")),
                            esc(rs.getString("condition_grade")),
                            rs.getDouble("cost_price"),
                            rs.getDouble("price"),
                            rs.getInt("stock"),
                            esc(rs.getString("description")),
                            imgUrl != null ? esc(imgUrl) : "",
                            imgUrlsJson,
                            specsJson));
                    first = false;
                }
                send(ex, 200, sb.append("]").toString());
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void post(HttpExchange ex) throws IOException {
            Map<String, String> b = parseJson(readBody(ex));
            if (!b.containsKey("name") || !b.containsKey("category") || !b.containsKey("condition")) {
                send(ex, 400, "{\"error\":\"name, category, and condition are required\"}"); return;
            }
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO products (name, category, condition_grade, cost_price, price, stock, description, image_url, specs, image_urls) VALUES (?,?,?,?,?,?,?,?,?,?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, b.get("name"));
                ps.setString(2, b.get("category"));
                ps.setString(3, b.get("condition"));
                ps.setDouble(4, Double.parseDouble(b.getOrDefault("costPrice", "0")));
                ps.setDouble(5, Double.parseDouble(b.getOrDefault("price", "0")));
                ps.setInt(6,    Integer.parseInt(b.getOrDefault("stock", "0")));
                ps.setString(7, b.getOrDefault("description", ""));
                String insertImg     = b.getOrDefault("imageUrl", "");
                String insertSpecs   = b.getOrDefault("specs", "");
                String insertImgUrls = b.getOrDefault("imageUrls", "");
                ps.setString(8,  insertImg.isEmpty()     ? null : insertImg);
                ps.setString(9,  insertSpecs.isEmpty()   ? null : insertSpecs);
                ps.setString(10, insertImgUrls.isEmpty() ? null : insertImgUrls);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                send(ex, 201, "{\"success\":true,\"id\":" + (keys.next() ? keys.getInt(1) : -1) + "}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void put(HttpExchange ex) throws IOException {
            String id = queryParams(ex).get("id");
            if (id == null) { send(ex, 400, "{\"error\":\"id is required\"}"); return; }
            Map<String, String> b = parseJson(readBody(ex));
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE products SET name=?, category=?, condition_grade=?, cost_price=?, price=?, stock=?, description=?, image_url=?, specs=?, image_urls=? WHERE id=?")) {
                ps.setString(1, b.getOrDefault("name", ""));
                ps.setString(2, b.getOrDefault("category", ""));
                ps.setString(3, b.getOrDefault("condition", ""));
                ps.setDouble(4, Double.parseDouble(b.getOrDefault("costPrice", "0")));
                ps.setDouble(5, Double.parseDouble(b.getOrDefault("price", "0")));
                ps.setInt(6,    Integer.parseInt(b.getOrDefault("stock", "0")));
                ps.setString(7, b.getOrDefault("description", ""));
                String updateImg     = b.getOrDefault("imageUrl", "");
                String updateSpecs   = b.getOrDefault("specs", "");
                String updateImgUrls = b.getOrDefault("imageUrls", "");
                ps.setString(8,  updateImg.isEmpty()     ? null : updateImg);
                ps.setString(9,  updateSpecs.isEmpty()   ? null : updateSpecs);
                ps.setString(10, updateImgUrls.isEmpty() ? null : updateImgUrls);
                ps.setInt(11, Integer.parseInt(id));
                send(ex, 200, "{\"success\":" + (ps.executeUpdate() > 0) + "}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void delete(HttpExchange ex) throws IOException {
            String id = queryParams(ex).get("id");
            if (id == null) { send(ex, 400, "{\"error\":\"id is required\"}"); return; }
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id=?")) {
                ps.setInt(1, Integer.parseInt(id));
                send(ex, 200, "{\"success\":" + (ps.executeUpdate() > 0) + "}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ── /api/orders  (GET / POST / PUT) ─────────────────────────────────────
    static class OrdersHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            switch (ex.getRequestMethod().toUpperCase()) {
                case "GET"  -> get(ex);
                case "POST" -> post(ex);
                case "PUT"  -> put(ex);
                default     -> ex.sendResponseHeaders(405, -1);
            }
        }

        void get(HttpExchange ex) throws IOException {
            Map<String, String> p = queryParams(ex);
            String status = p.get("status");
            String search = p.get("search");
            String email  = p.get("email");

            StringBuilder sql = new StringBuilder(
                "SELECT o.id, o.order_ref, o.customer_name, o.customer_email, o.address, " +
                "o.total, o.status, o.tracking_number, o.order_date, " +
                "GROUP_CONCAT(oi.product_name SEPARATOR ', ') AS products " +
                "FROM orders o LEFT JOIN order_items oi ON o.id = oi.order_id WHERE 1=1");
            if (status != null && !status.isEmpty()) sql.append(" AND o.status = ?");
            if (search != null && !search.isEmpty())
                sql.append(" AND (o.order_ref LIKE ? OR o.customer_name LIKE ?)");
            if (email  != null && !email.isEmpty())  sql.append(" AND o.customer_email = ?");
            sql.append(" GROUP BY o.id ORDER BY o.order_date DESC");

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int i = 1;
                if (status != null && !status.isEmpty()) ps.setString(i++, status);
                if (search != null && !search.isEmpty()) {
                    ps.setString(i++, "%" + search + "%");
                    ps.setString(i++, "%" + search + "%");
                }
                if (email  != null && !email.isEmpty())  ps.setString(i++, email);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append(String.format(
                            "{\"id\":%d,\"ref\":\"%s\",\"customer\":\"%s\",\"email\":\"%s\"," +
                            "\"address\":\"%s\",\"total\":%.2f,\"status\":\"%s\"," +
                            "\"tracking\":\"%s\",\"date\":\"%s\",\"products\":\"%s\"}",
                            rs.getInt("id"),
                            esc(rs.getString("order_ref")),
                            esc(rs.getString("customer_name")),
                            esc(rs.getString("customer_email")),
                            esc(rs.getString("address")),
                            rs.getDouble("total"),
                            esc(rs.getString("status")),
                            esc(rs.getString("tracking_number")),
                            esc(String.valueOf(rs.getTimestamp("order_date"))),
                            esc(rs.getString("products"))));
                    first = false;
                }
                send(ex, 200, sb.append("]").toString());
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void post(HttpExchange ex) throws IOException {
            Map<String, String> b = parseJson(readBody(ex));
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                // Generate order reference
                String ref;
                try (PreparedStatement ct = conn.prepareStatement("SELECT COUNT(*) FROM orders")) {
                    ResultSet cr = ct.executeQuery();
                    ref = "#RT-" + (1041 + (cr.next() ? cr.getInt(1) : 0));
                }
                // Insert order
                int orderId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO orders (order_ref, customer_name, customer_email, address, total, status) VALUES (?,?,?,?,?,'pending')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, ref);
                    ps.setString(2, b.getOrDefault("customer_name", ""));
                    ps.setString(3, b.getOrDefault("customer_email", ""));
                    ps.setString(4, b.getOrDefault("address", ""));
                    ps.setDouble(5, Double.parseDouble(b.getOrDefault("total", "0")));
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    orderId = keys.next() ? keys.getInt(1) : -1;
                }
                // Insert all order items from items_json array
                String itemsJson = b.getOrDefault("items_json", "");
                List<String> itemList = splitJsonArray(itemsJson);
                if (!itemList.isEmpty() && orderId > 0) {
                    try (PreparedStatement pi = conn.prepareStatement(
                            "INSERT INTO order_items (order_id, product_id, product_name, quantity, price) VALUES (?,?,?,?,?)")) {
                        for (String itemStr : itemList) {
                            Map<String, String> item = parseJson(itemStr);
                            String pName = item.getOrDefault("product_name", "");
                            String pId   = item.get("product_id");
                            int    qty   = Integer.parseInt(item.getOrDefault("qty", "1"));
                            double price = Double.parseDouble(item.getOrDefault("price", "0"));
                            pi.setInt(1, orderId);
                            if (pId != null && !pId.isEmpty()) pi.setInt(2, Integer.parseInt(pId));
                            else                               pi.setNull(2, Types.INTEGER);
                            pi.setString(3, pName);
                            pi.setInt(4, qty);
                            pi.setDouble(5, price);
                            pi.addBatch();
                        }
                        pi.executeBatch();
                    }
                }
                conn.commit();
                send(ex, 201, "{\"success\":true,\"ref\":\"" + ref + "\",\"id\":" + orderId + "}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void put(HttpExchange ex) throws IOException {
            String id = queryParams(ex).get("id");
            if (id == null) { send(ex, 400, "{\"error\":\"id is required\"}"); return; }
            Map<String, String> b = parseJson(readBody(ex));
            String newStatus = b.getOrDefault("status", "pending");
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                // Get current status before updating
                String prevStatus = "pending";
                try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM orders WHERE id=?")) {
                    ps.setInt(1, Integer.parseInt(id));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) prevStatus = rs.getString("status");
                }

                // Update order status
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE orders SET status=?, tracking_number=? WHERE id=?")) {
                    ps.setString(1, newStatus);
                    ps.setString(2, b.getOrDefault("tracking", ""));
                    ps.setInt(3, Integer.parseInt(id));
                    ps.executeUpdate();
                }

                // Adjust stock based on status transition
                if ("pending".equals(prevStatus) && "shipped".equals(newStatus)) {
                    // Decrement by product_id where available
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE products p JOIN order_items oi ON p.id = oi.product_id " +
                            "SET p.stock = GREATEST(0, p.stock - oi.quantity) " +
                            "WHERE oi.order_id = ?")) {
                        ps.setInt(1, Integer.parseInt(id));
                        ps.executeUpdate();
                    }
                    // Fallback: decrement by product name for items with no product_id
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE products p JOIN order_items oi ON p.name = oi.product_name " +
                            "SET p.stock = GREATEST(0, p.stock - oi.quantity) " +
                            "WHERE oi.order_id = ? AND oi.product_id IS NULL")) {
                        ps.setInt(1, Integer.parseInt(id));
                        ps.executeUpdate();
                    }
                } else if ("pending".equals(prevStatus) && "cancelled".equals(newStatus)) {
                    // Order never shipped — no stock was decremented, nothing to restore
                } else if ("shipped".equals(prevStatus) && "cancelled".equals(newStatus)) {
                    // Restore by product_id where available
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE products p JOIN order_items oi ON p.id = oi.product_id " +
                            "SET p.stock = p.stock + oi.quantity " +
                            "WHERE oi.order_id = ?")) {
                        ps.setInt(1, Integer.parseInt(id));
                        ps.executeUpdate();
                    }
                    // Fallback: restore by name for items with no product_id
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE products p JOIN order_items oi ON p.name = oi.product_name " +
                            "SET p.stock = p.stock + oi.quantity " +
                            "WHERE oi.order_id = ? AND oi.product_id IS NULL")) {
                        ps.setInt(1, Integer.parseInt(id));
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                send(ex, 200, "{\"success\":true}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ── /api/users  (GET / POST / PUT) ──────────────────────────────────────
    static class UsersHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            switch (ex.getRequestMethod().toUpperCase()) {
                case "GET"    -> get(ex);
                case "POST"   -> post(ex);
                case "PUT"    -> put(ex);
                case "DELETE" -> delete(ex);
                default       -> ex.sendResponseHeaders(405, -1);
            }
        }

        void get(HttpExchange ex) throws IOException {
            Map<String, String> p = queryParams(ex);
            String role   = p.get("role");
            String status = p.get("status");
            String search = p.get("search");

            StringBuilder sql = new StringBuilder(
                    "SELECT id, name, email, password, role, status, created_at FROM users WHERE 1=1");
            if (role   != null && !role.isEmpty())   sql.append(" AND role = ?");
            if (status != null && !status.isEmpty()) sql.append(" AND status = ?");
            if (search != null && !search.isEmpty())
                sql.append(" AND (name LIKE ? OR email LIKE ?)");
            sql.append(" ORDER BY created_at DESC");

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int i = 1;
                if (role   != null && !role.isEmpty())   ps.setString(i++, role);
                if (status != null && !status.isEmpty()) ps.setString(i++, status);
                if (search != null && !search.isEmpty()) {
                    ps.setString(i++, "%" + search + "%");
                    ps.setString(i++, "%" + search + "%");
                }
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\",\"status\":\"%s\",\"joined\":\"%s\"}",
                            rs.getInt("id"),
                            esc(rs.getString("name")),
                            esc(rs.getString("email")),
                            esc(rs.getString("password")),
                            esc(rs.getString("role")),
                            esc(rs.getString("status")),
                            esc(String.valueOf(rs.getTimestamp("created_at")))));
                    first = false;
                }
                send(ex, 200, sb.append("]").toString());
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void post(HttpExchange ex) throws IOException {
            Map<String, String> b = parseJson(readBody(ex));
            if (!b.containsKey("name") || !b.containsKey("email")) {
                send(ex, 400, "{\"error\":\"name and email are required\"}"); return;
            }
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO users (name, email, password, role) VALUES (?,?,?,?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, b.get("name"));
                ps.setString(2, b.get("email"));
                ps.setString(3, b.getOrDefault("password", "changeme"));
                ps.setString(4, b.getOrDefault("role", "staff"));
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                send(ex, 201, "{\"success\":true,\"id\":" + (keys.next() ? keys.getInt(1) : -1) + "}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void put(HttpExchange ex) throws IOException {
            String id = queryParams(ex).get("id");
            if (id == null) { send(ex, 400, "{\"error\":\"id is required\"}"); return; }
            Map<String, String> b = parseJson(readBody(ex));
            // Accept either a status change or a role change
            String field = b.containsKey("status") ? "status" : "role";
            String value = b.getOrDefault(field, "active");
            try (Connection conn = getConnection()) {
                // Prevent suspending admin accounts
                if ("status".equals(field) && "suspended".equals(value)) {
                    try (PreparedStatement check = conn.prepareStatement(
                            "SELECT role FROM users WHERE id = ?")) {
                        check.setInt(1, Integer.parseInt(id));
                        ResultSet rs = check.executeQuery();
                        if (rs.next() && "admin".equals(rs.getString("role"))) {
                            send(ex, 403, "{\"success\":false,\"message\":\"Admin accounts cannot be suspended\"}");
                            return;
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET " + field + " = ? WHERE id = ?")) {
                    ps.setString(1, value);
                    ps.setInt(2, Integer.parseInt(id));
                    send(ex, 200, "{\"success\":" + (ps.executeUpdate() > 0) + "}");
                }
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void delete(HttpExchange ex) throws IOException {
            String id = queryParams(ex).get("id");
            if (id == null) { send(ex, 400, "{\"error\":\"id is required\"}"); return; }
            try (Connection conn = getConnection()) {
                // Only allow deleting customer accounts
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT role FROM users WHERE id = ?")) {
                    check.setInt(1, Integer.parseInt(id));
                    ResultSet rs = check.executeQuery();
                    if (!rs.next()) { send(ex, 404, "{\"error\":\"User not found\"}"); return; }
                    if (!"customer".equals(rs.getString("role"))) {
                        send(ex, 403, "{\"error\":\"Only customer accounts can be deleted\"}"); return;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM users WHERE id = ? AND role = 'customer'")) {
                    ps.setInt(1, Integer.parseInt(id));
                    send(ex, 200, "{\"success\":" + (ps.executeUpdate() > 0) + "}");
                }
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ── /api/restock-orders  (GET / POST / PUT) ──────────────────────────────
    static class RestockHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            switch (ex.getRequestMethod().toUpperCase()) {
                case "GET"  -> get(ex);
                case "POST" -> post(ex);
                case "PUT"  -> put(ex);
                default     -> ex.sendResponseHeaders(405, -1);
            }
        }

        void get(HttpExchange ex) throws IOException {
            String status = queryParams(ex).get("status");
            StringBuilder sql = new StringBuilder("SELECT * FROM restock_orders WHERE 1=1");
            if (status != null && !status.isEmpty()) sql.append(" AND status = ?");
            sql.append(" ORDER BY created_at DESC");

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                if (status != null && !status.isEmpty()) ps.setString(1, status);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append(String.format(
                            "{\"id\":%d,\"ref\":\"%s\",\"supplier\":\"%s\",\"product\":\"%s\"," +
                            "\"qty\":%d,\"unitCost\":%.2f,\"expected\":\"%s\",\"status\":\"%s\",\"notes\":\"%s\"}",
                            rs.getInt("id"),
                            esc(rs.getString("po_ref")),
                            esc(rs.getString("supplier")),
                            esc(rs.getString("product_name")),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_cost"),
                            esc(String.valueOf(rs.getDate("expected_date"))),
                            esc(rs.getString("status")),
                            esc(rs.getString("notes"))));
                    first = false;
                }
                send(ex, 200, sb.append("]").toString());
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void post(HttpExchange ex) throws IOException {
            Map<String, String> b = parseJson(readBody(ex));
            try (Connection conn = getConnection()) {
                // Generate PO reference
                String ref;
                try (PreparedStatement ct = conn.prepareStatement("SELECT COUNT(*) FROM restock_orders")) {
                    ResultSet cr = ct.executeQuery();
                    ref = String.format("#PO-%03d", (cr.next() ? cr.getInt(1) : 0) + 1);
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO restock_orders (po_ref, supplier, product_name, product_id, quantity, unit_cost, expected_date, notes) VALUES (?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, ref);
                    ps.setString(2, b.getOrDefault("supplier", ""));
                    ps.setString(3, b.getOrDefault("product", ""));
                    String pid = b.get("productId");
                    if (pid != null && !pid.isEmpty() && !pid.equals("0")) ps.setInt(4, Integer.parseInt(pid));
                    else ps.setNull(4, Types.INTEGER);
                    ps.setInt(5,    Integer.parseInt(b.getOrDefault("qty", "1")));
                    ps.setDouble(6, Double.parseDouble(b.getOrDefault("unitCost", "0")));
                    ps.setString(7, b.getOrDefault("expected", null));
                    ps.setString(8, b.getOrDefault("notes", ""));
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    send(ex, 201, "{\"success\":true,\"ref\":\"" + ref + "\",\"id\":" + (keys.next() ? keys.getInt(1) : -1) + "}");
                }
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }

        void put(HttpExchange ex) throws IOException {
            String id = queryParams(ex).get("id");
            if (id == null) { send(ex, 400, "{\"error\":\"id is required\"}"); return; }
            Map<String, String> b = parseJson(readBody(ex));
            String newStatus = b.getOrDefault("status", "ordered");
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                // Update the restock order status
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE restock_orders SET status = ? WHERE id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, Integer.parseInt(id));
                    ps.executeUpdate();
                }
                // If marked received, add the restock quantity to the matching product's stock.
                // Use product_id when available (reliable); fall back to name match for old orders.
                if ("received".equals(newStatus)) {
                    try (PreparedStatement upd = conn.prepareStatement(
                            "UPDATE products p JOIN restock_orders r " +
                            "ON (r.product_id IS NOT NULL AND p.id = r.product_id) " +
                            "OR (r.product_id IS NULL AND p.name = r.product_name) " +
                            "SET p.stock = p.stock + r.quantity WHERE r.id = ?")) {
                        upd.setInt(1, Integer.parseInt(id));
                        upd.executeUpdate();
                    }
                }
                conn.commit();
                send(ex, 200, "{\"success\":true}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ── /api/contact (POST) ──────────────────────────────────────
static class ContactHandler implements HttpHandler {
    public void handle(HttpExchange ex) throws IOException {
        cors(ex.getResponseHeaders());

        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
            ex.sendResponseHeaders(405, -1);
            return;
        }

        Map<String, String> body = parseJson(readBody(ex));

        String firstName = body.getOrDefault("firstName", "");
        String lastName  = body.getOrDefault("lastName", "");
        String email     = body.getOrDefault("email", "");
        String subject   = body.getOrDefault("subject", "");
        String message   = body.getOrDefault("message", "");

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO contact_messages (first_name, last_name, email, subject, message) VALUES (?, ?, ?, ?, ?)"
             )) {

            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, subject);
            ps.setString(5, message);

            ps.executeUpdate();

            send(ex, 200, "{\"success\":true}");

        } catch (SQLException e) {
            send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }
}

    // ── /api/stats  (GET) ────────────────────────────────────────────────────
    static class StatsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            if (!ex.getRequestMethod().equalsIgnoreCase("GET"))    { ex.sendResponseHeaders(405, -1); return; }
            try (Connection conn = getConnection()) {
                int year = java.time.Year.now().getValue();
                double[] monthly = new double[12];
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT MONTH(order_date) AS m, SUM(total) AS rev FROM orders " +
                        "WHERE YEAR(order_date)=? AND status IN ('shipped','delivered') GROUP BY m")) {
                    ps.setInt(1, year);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) monthly[rs.getInt("m") - 1] = rs.getDouble("rev");
                }
                // Seed all known categories at 0 so every category appears even with no sales
                java.util.LinkedHashMap<String, Double> catMap = new java.util.LinkedHashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT DISTINCT category FROM products ORDER BY category")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) catMap.put(rs.getString("category"), 0.0);
                }
                // Add shipped/delivered totals matched by product_id
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT p.category, SUM(oi.quantity * oi.price) AS total " +
                        "FROM order_items oi JOIN products p ON oi.product_id = p.id " +
                        "JOIN orders o ON oi.order_id = o.id " +
                        "WHERE o.status IN ('shipped','delivered') GROUP BY p.category")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) catMap.merge(rs.getString("category"), rs.getDouble("total"), Double::sum);
                }
                // Also match by name for items where product_id was NULL
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT p.category, SUM(oi.quantity * oi.price) AS total " +
                        "FROM order_items oi JOIN products p ON p.name = oi.product_name " +
                        "JOIN orders o ON oi.order_id = o.id " +
                        "WHERE o.status IN ('shipped','delivered') AND oi.product_id IS NULL GROUP BY p.category")) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) catMap.merge(rs.getString("category"), rs.getDouble("total"), Double::sum);
                }
                java.util.List<java.util.Map.Entry<String,Double>> catEntries = new java.util.ArrayList<>(catMap.entrySet());
                catEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                StringBuilder catSb = new StringBuilder("[");
                boolean first = true;
                for (java.util.Map.Entry<String,Double> entry : catEntries) {
                    if (!first) catSb.append(",");
                    catSb.append(String.format("{\"category\":\"%s\",\"total\":%.2f}", esc(entry.getKey()), entry.getValue()));
                    first = false;
                }
                catSb.append("]");
                String[] labels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
                StringBuilder mSb = new StringBuilder("[");
                for (int i = 0; i < 12; i++) {
                    if (i > 0) mSb.append(",");
                    mSb.append(String.format("{\"l\":\"%s\",\"v\":%.2f}", labels[i], monthly[i]));
                }
                mSb.append("]");
                send(ex, 200, "{\"monthly\":" + mSb + ",\"byCategory\":" + catSb + "}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

// ── /api/contact-messages (GET, PUT, DELETE) ─────────────────
static class ContactMessagesHandler implements HttpHandler {
    public void handle(HttpExchange ex) throws IOException {
        cors(ex.getResponseHeaders());
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1); return;
        }

        String method = ex.getRequestMethod().toUpperCase();

        // GET — return all messages ordered newest first
        if (method.equals("GET")) {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, first_name, last_name, email, subject, message, is_read, submitted_at " +
                     "FROM contact_messages ORDER BY submitted_at DESC")) {
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{")
                      .append("\"id\":").append(rs.getInt("id")).append(",")
                      .append("\"first_name\":\"").append(esc(rs.getString("first_name"))).append("\",")
                      .append("\"last_name\":\"").append(esc(rs.getString("last_name"))).append("\",")
                      .append("\"email\":\"").append(esc(rs.getString("email"))).append("\",")
                      .append("\"subject\":\"").append(esc(rs.getString("subject"))).append("\",")
                      .append("\"message\":\"").append(esc(rs.getString("message"))).append("\",")
                      .append("\"is_read\":").append(rs.getInt("is_read")).append(",")
                      .append("\"submitted_at\":\"").append(rs.getTimestamp("submitted_at")).append("\"")
                      .append("}");
                }
                sb.append("]");
                send(ex, 200, sb.toString());
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
            return;
        }

        // PUT — mark message read/unread  (?id=X)
        if (method.equals("PUT")) {
            String query = ex.getRequestURI().getQuery();
            int id = parseId(query);
            if (id < 1) { send(ex, 400, "{\"error\":\"Missing id\"}"); return; }
            Map<String, String> body = parseJson(readBody(ex));
            int isRead = Integer.parseInt(body.getOrDefault("is_read", "1"));
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE contact_messages SET is_read=? WHERE id=?")) {
                ps.setInt(1, isRead);
                ps.setInt(2, id);
                ps.executeUpdate();
                send(ex, 200, "{\"success\":true}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
            return;
        }

        // DELETE — remove a message (?id=X)
        if (method.equals("DELETE")) {
            String query = ex.getRequestURI().getQuery();
            int id = parseId(query);
            if (id < 1) { send(ex, 400, "{\"error\":\"Missing id\"}"); return; }
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM contact_messages WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                send(ex, 200, "{\"success\":true}");
            } catch (SQLException e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
            return;
        }

        ex.sendResponseHeaders(405, -1);
    }

    private int parseId(String query) {
        if (query == null) return -1;
        for (String p : query.split("&")) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals("id")) {
                try { return Integer.parseInt(kv[1]); } catch (NumberFormatException e) { return -1; }
            }
        }
        return -1;
    }
}
    
    // ── Image upload ─────────────────────────────────────────────────────────
    static class UploadImageHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { send(ex, 405, "{\"error\":\"Method not allowed\"}"); return; }
            Map<String, String> b = parseJson(readBody(ex));
            String filename = b.get("filename");
            String data     = b.get("data");
            if (filename == null || filename.isBlank() || data == null || data.isBlank()) {
                send(ex, 400, "{\"error\":\"filename and data are required\"}"); return;
            }
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "-");
            Path dest = projectRoot.resolve("public/images/" + filename).normalize();
            if (!dest.startsWith(projectRoot.resolve("public/images"))) {
                send(ex, 400, "{\"error\":\"Invalid filename\"}"); return;
            }
            try {
                Files.write(dest, Base64.getDecoder().decode(data));
                send(ex, 200, "{\"path\":\"public/images/" + esc(filename) + "\"}");
            } catch (Exception e) {
                send(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            }
        }
    }

    // ── Static file server ───────────────────────────────────────────────────
    static class StaticFileHandler implements HttpHandler {
        private final Path root;
        StaticFileHandler(Path root) { this.root = root; }

        public void handle(HttpExchange ex) throws IOException {
            cors(ex.getResponseHeaders());
            if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { ex.sendResponseHeaders(405, -1); return; }
            String reqPath = ex.getRequestURI().getPath();
            if (reqPath.equals("/") || reqPath.isEmpty()) reqPath = "/index.html";
            Path filePath = root.resolve(reqPath.substring(1)).normalize();
            if (!filePath.startsWith(root) || Files.isDirectory(filePath) || !Files.exists(filePath)) {
                ex.sendResponseHeaders(404, -1); return;
            }
            String mime = Files.probeContentType(filePath);
            if (mime == null) mime = "application/octet-stream";
            ex.getResponseHeaders().add("Content-Type", mime);
            byte[] bytes = Files.readAllBytes(filePath);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }
    }
}
