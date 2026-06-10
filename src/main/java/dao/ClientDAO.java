package dao;

import model.Client;
import java.util.List;
import java.util.Map;

public interface ClientDAO {
    List<Client> getAllClients();
    Client getClientById(int id);
    boolean addClient(Client client);
    boolean updateClient(Client client);
    boolean deleteClient(int id);
    List<Client> searchClients(String keyword);

    // NEW METHODS
    List<Client> getAllClients(int page, int size, String sortBy, String sortDir);
    int getTotalCount();
    Map<String, Integer> getStats();
    boolean bulkDelete(List<Integer> ids);
    boolean bulkUpdateStatus(List<Integer> ids, String status);
    boolean isEmailUnique(String email, int excludeId);
    List<Client> getRecentClients(int limit);
    String exportToCsv();
}
