package com.ih.clientmanagmentsystem;

import server.AppServer;

public class ClientmanagmentsystemApplication {
    public static void main(String[] args) {
        try {
            AppServer server = new AppServer(8082);
            server.start();
            System.out.println("Client Management System started on http://localhost:8082");
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}