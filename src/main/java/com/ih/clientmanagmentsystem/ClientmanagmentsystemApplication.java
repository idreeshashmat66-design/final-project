package com.ih.clientmanagmentsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientmanagmentsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientmanagmentsystemApplication.class, args);
    }

}
spring.datasource.url=jdbc:mysql://localhost:3306/clientmanagmentsystem_db
spring.datasource.username=root
spring.datasource.password=aass1122

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver