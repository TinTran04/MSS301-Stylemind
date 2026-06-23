package com.stylemind.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.stylemind.ai", "com.stylemind.common"})
@EnableFeignClients(basePackages = "com.stylemind.ai.feign")
@EnableScheduling
public class AiAgentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAgentServiceApplication.class, args);
    }
}
