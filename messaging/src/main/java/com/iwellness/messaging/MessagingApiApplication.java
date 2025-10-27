package com.iwellness.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MessagingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingApiApplication.class, args);
    }
}
