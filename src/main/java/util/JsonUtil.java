package util;


import model.Client;
import java.util.List;

/**
 * Lightweight JSON utility using Gson (add gson dependency to pom.xml).
 * Falls back to manual serialization if Gson is not available.
 */
public class JsonUtil {

    // ── Serialize ────────────────────────────────────────────────────────────

    public static String toJson(Client client) {
        if (client == null) return "null";
        return "{"
                + "\"id\":"      + client.getId()                        + ","
                + "\"name\":"    + quote(client.getName())               + ","
                + "\"email\":"   + quote(client.getEmail())              + ","
                + "\"phone\":"   + quote(client.getPhone())              + ","
                + "\"address\":" + quote(client.getAddress())            + ","
                + "\"company\":" + quote(client.getCompany())            + ","
                + "\"status\":"  + quote(client.getStatus())
                + "}";
    }

    public static String toJson(List<Client> clients) {
        if (clients == null || clients.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < clients.size(); i++) {
            sb.append(toJson(clients.get(i)));
            if (i < clients.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── Deserialize ──────────────────────────────────────────────────────────

    /**
     * Very simple JSON → Client parser (handles flat JSON only).
     * Replace with Gson/Jackson for production use.
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (clazz == Client.class) {
            Client c = new Client();
            c.setName(extractField(json, "name"));
            c.setEmail(extractField(json, "email"));
            c.setPhone(extractField(json, "phone"));
            c.setAddress(extractField(json, "address"));
            c.setCompany(extractField(json, "company"));
            c.setStatus(extractField(json, "status"));
            String idStr = extractField(json, "id");
            if (idStr != null && !idStr.isEmpty()) {
                try { c.setId(Integer.parseInt(idStr)); } catch (NumberFormatException ignored) {}
            }
            return (T) c;
        }
        throw new UnsupportedOperationException("fromJson not implemented for: " + clazz.getName());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String quote(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    // Add to JsonUtil.java
    public static List<Integer> parseIntList(String json) {
        List<Integer> list = new java.util.ArrayList<>();
        // simple parsing: assume [1,2,3]
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            String[] parts = json.substring(1, json.length()-1).split(",");
            for (String p : parts) {
                try { list.add(Integer.parseInt(p.trim())); } catch (NumberFormatException e) {}
            }
        }
        return list;
    }

    /**
     * Extracts a string or numeric field value from a flat JSON string.
     * Example: {"name":"Alice"} → extractField(json, "name") → "Alice"
     */
    public static String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + key.length());
        if (colon == -1) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;

        char first = json.charAt(start);
        if (first == '"') {
            // string value
            int end = start + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            return json.substring(start + 1, end);
        } else {
            // numeric / boolean / null value
            int end = start;
            while (end < json.length() && ",}".indexOf(json.charAt(end)) == -1) end++;
            return json.substring(start, end).trim();}

        }
    }

