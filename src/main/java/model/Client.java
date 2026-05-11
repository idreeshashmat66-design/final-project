package com.ih.clientmanagmentsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String issueType;

    // Default Constructor
    public Client() {
    }

    // Parameterized Constructor
    public Client(String name, String email, String issueType) {
        this.name = name;
        this.email = email;
        this.issueType = issueType;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getIssueType() {
        return issueType;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }
}