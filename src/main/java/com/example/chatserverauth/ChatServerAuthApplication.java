package com.example.chatserverauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ChatServerAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServerAuthApplication.class, args);
    }

}
