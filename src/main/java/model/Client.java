package model;

public class Client {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String company;
    private String status; // active, inactive

    public Client() {}

    public Client(int id, String name, String email, String phone, String address, String company, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.company = company;
        this.status = status;
    }

    public Client(String name, String email, String phone, String address, String company, String status) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.company = company;
        this.status = status;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Client{id=" + id + ", name='" + name + "', email='" + email +
                "', phone='" + phone + "', company='" + company + "', status='" + status + "'}";
    }
}
