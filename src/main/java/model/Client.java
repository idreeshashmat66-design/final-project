package model;

public class Client {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String address;

    public Client(int id, String name, String email, String phone, String address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }
    public Client(String name, String email, String phone, String address) {
        this(0, name, email, phone, address);
    }

    // Getters
    public int getId()          { return id; }
    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }
    public String getAddress()  { return address; }

    public void setName(String name)
    { this.name = name; }
    public void setEmail(String email)
    { this.email = email; }
    public void setPhone(String phone)
    { this.phone = phone; }
    public void setAddress(String address)
    { this.address = address; }

    @Override
    public String toString() {
        return "Client[" + id + "] " + name + " | " + email;
    }
}
