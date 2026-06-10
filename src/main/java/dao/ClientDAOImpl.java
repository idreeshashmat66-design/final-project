package dao;

import model.Client;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientDAOImpl implements ClientDAO {

    @Override
    public List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clients.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching clients: " + e.getMessage());
        }
        return clients;
    }

    @Override
    public Client getClientById(int id) {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching client by ID: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean addClient(Client client) {
        String sql = "INSERT INTO clients (name, email, phone, address, company, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, client.getName());
            ps.setString(2, client.getEmail());
            ps.setString(3, client.getPhone());
            ps.setString(4, client.getAddress());
            ps.setString(5, client.getCompany());
            ps.setString(6, client.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding client: " + e.getMessage());
            return false;
        }
    }

    public interface ClientDAO {
        List<Client> getAllClients();
        Client getClientById(int id);
        boolean addClient(Client client);
        boolean updateClient(Client client);
        boolean deleteClient(int id);
        List<Client> searchClients(String keyword);

        // New methods
        List<Client> getAllClients(int page, int size, String sortBy, String sortDir);
        int getTotalCount();
        Map<String, Integer> getStats();
        boolean bulkDelete(List<Integer> ids);
        boolean bulkUpdateStatus(List<Integer> ids, String status);
        boolean isEmailUnique(String email, int excludeId);
        List<Client> getRecentClients(int limit);
        String exportToCsv();
    }

    @Override
    public boolean updateClient(Client client) {
        String sql = "UPDATE clients SET name=?, email=?, phone=?, address=?, company=?, status=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, client.getName());
            ps.setString(2, client.getEmail());
            ps.setString(3, client.getPhone());
            ps.setString(4, client.getAddress());
            ps.setString(5, client.getCompany());
            ps.setString(6, client.getStatus());
            ps.setInt(7, client.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating client: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteClient(int id) {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting client: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Client> searchClients(String keyword) {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients WHERE name LIKE ? OR email LIKE ? OR company LIKE ?";
        String like = "%" + keyword + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching clients: " + e.getMessage());
        }
        return clients;
    }

    // ==================== NEW METHODS ====================

    @Override
    public List<Client> getAllClients(int page, int size, String sortBy, String sortDir) {
        List<Client> clients = new ArrayList<>();
        String sortColumn = "name";
        if (sortBy != null && (sortBy.equals("id") || sortBy.equals("name") || sortBy.equals("email") || sortBy.equals("status"))) {
            sortColumn = sortBy;
        }
        String order = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "DESC" : "ASC";
        String sql = "SELECT * FROM clients ORDER BY " + sortColumn + " " + order + " LIMIT ? OFFSET ?";
        int offset = page * size;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clients.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching clients paginated: " + e.getMessage());
        }
        return clients;
    }

    @Override
    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM clients";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting clients: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new java.util.HashMap<>();
        String sql = "SELECT status, COUNT(*) FROM clients GROUP BY status";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int total = 0, active = 0, inactive = 0;
            while (rs.next()) {
                String status = rs.getString(1);
                int count = rs.getInt(2);
                total += count;
                if ("active".equals(status)) active = count;
                else if ("inactive".equals(status)) inactive = count;
            }
            stats.put("total", total);
            stats.put("active", active);
            stats.put("inactive", inactive);
        } catch (SQLException e) {
            System.err.println("Error getting stats: " + e.getMessage());
        }
        return stats;
    }

    @Override
    public boolean bulkDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return false;
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int id : ids) {
                    ps.setInt(1, id);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                conn.commit();
                return results.length == ids.size();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Bulk delete error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean bulkUpdateStatus(List<Integer> ids, String status) {
        if (ids == null || ids.isEmpty()) return false;
        String sql = "UPDATE clients SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int id : ids) {
                    ps.setString(1, status);
                    ps.setInt(2, id);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                conn.commit();
                return results.length == ids.size();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Bulk status update error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEmailUnique(String email, int excludeId) {
        String sql = "SELECT COUNT(*) FROM clients WHERE email = ? AND id != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            System.err.println("Email uniqueness check error: " + e.getMessage());
        }
        return true;
    }

    @Override
    public List<Client> getRecentClients(int limit) {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clients.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recent clients: " + e.getMessage());
        }
        return clients;
    }

    @Override
    public String exportToCsv() {
        List<Client> clients = getAllClients();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Name,Email,Phone,Address,Company,Status\n");
        for (Client c : clients) {
            sb.append(c.getId()).append(",")
                    .append(escapeCsv(c.getName())).append(",")
                    .append(escapeCsv(c.getEmail())).append(",")
                    .append(escapeCsv(c.getPhone())).append(",")
                    .append(escapeCsv(c.getAddress())).append(",")
                    .append(escapeCsv(c.getCompany())).append(",")
                    .append(c.getStatus()).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private Client mapRow(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("company"),
                rs.getString("status")
        );
    }
}