package com.example.demo;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public List<ToolCallback> mcpTools(KiteTradingService kiteTradingService) {
        List<ToolCallback> callbacks = new ArrayList<>();
        callbacks.addAll(Arrays.asList(ToolCallbacks.from(kiteTradingService)));
        return callbacks;
    }
}