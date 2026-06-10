package controller;

import dao.ClientDAO;
import dao.ClientDAOImpl;
import model.Client;
import util.JsonUtil;
import java.util.List;
import java.util.Map;

public class ClientController {

    private final ClientDAO clientDAO;

    public ClientController() {
        this.clientDAO = new ClientDAOImpl();
    }

    // Basic CRUD
    public String getAllClients() {
        return JsonUtil.toJson(clientDAO.getAllClients());
    }

    public String getClientById(int id) {
        Client client = clientDAO.getClientById(id);
        return client == null ? "{\"error\":\"Client not found\"}" : JsonUtil.toJson(client);
    }

    public String addClient(String jsonBody) {
        try {
            Client client = JsonUtil.fromJson(jsonBody, Client.class);
            if (client.getName() == null || client.getName().isBlank())
                return "{\"success\":false,\"message\":\"Name is required\"}";
            if (client.getEmail() == null || client.getEmail().isBlank())
                return "{\"success\":false,\"message\":\"Email is required\"}";
            if (!clientDAO.isEmailUnique(client.getEmail(), -1))
                return "{\"success\":false,\"message\":\"Email already exists\"}";
            if (client.getStatus() == null || client.getStatus().isBlank())
                client.setStatus("active");
            boolean result = clientDAO.addClient(client);
            return result ? "{\"success\":true,\"message\":\"Client added successfully\"}"
                    : "{\"success\":false,\"message\":\"Failed to add client\"}";
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}";
        }
    }

    public String updateClient(int id, String jsonBody) {
        try {
            if (clientDAO.getClientById(id) == null)
                return "{\"success\":false,\"message\":\"Client not found\"}";
            Client client = JsonUtil.fromJson(jsonBody, Client.class);
            if (!clientDAO.isEmailUnique(client.getEmail(), id))
                return "{\"success\":false,\"message\":\"Email already exists\"}";
            client.setId(id);
            boolean result = clientDAO.updateClient(client);
            return result ? "{\"success\":true,\"message\":\"Client updated successfully\"}"
                    : "{\"success\":false,\"message\":\"Failed to update client\"}";
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}";
        }
    }

    public String deleteClient(int id) {
        if (clientDAO.getClientById(id) == null)
            return "{\"success\":false,\"message\":\"Client not found\"}";
        boolean result = clientDAO.deleteClient(id);
        return result ? "{\"success\":true,\"message\":\"Client deleted successfully\"}"
                : "{\"success\":false,\"message\":\"Failed to delete client\"}";
    }

    public String searchClients(String keyword) {
        if (keyword == null || keyword.isBlank()) return getAllClients();
        return JsonUtil.toJson(clientDAO.searchClients(keyword));
    }

    // New endpoints
    public String getStats() {
        Map<String, Integer> stats = clientDAO.getStats();
        return JsonUtil.toJson((Client) stats);
    }

    public String getRecentClients(int limit) {
        return JsonUtil.toJson(clientDAO.getRecentClients(limit));
    }

    public String getClientsPaginated(int page, int size, String sort, String order) {
        List<Client> clients = clientDAO.getAllClients(page, size, sort, order);
        int total = clientDAO.getTotalCount();
        return "{\"data\":" + JsonUtil.toJson(clients) + ",\"total\":" + total + "}";
    }

    public String bulkDelete(String jsonBody) {
        try {
            List<Integer> ids = JsonUtil.parseIntList(jsonBody);
            boolean success = clientDAO.bulkDelete(ids);
            return success ? "{\"success\":true,\"message\":\"Deleted successfully\"}"
                    : "{\"success\":false,\"message\":\"Bulk delete failed\"}";
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"Invalid request\"}";
        }
    }

    public String bulkUpdateStatus(String jsonBody) {
        try {
            String status = JsonUtil.extractField(jsonBody, "status");
            String idsStr = JsonUtil.extractField(jsonBody, "ids");
            List<Integer> ids = JsonUtil.parseIntList(idsStr);
            boolean success = clientDAO.bulkUpdateStatus(ids, status);
            return success ? "{\"success\":true,\"message\":\"Status updated\"}"
                    : "{\"success\":false,\"message\":\"Update failed\"}";
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"Invalid request\"}";
        }
    }

    public String exportCsv() {
        return clientDAO.exportToCsv();
    }
}