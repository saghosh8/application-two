package com.example.applicationtwo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class StatusController {

    @GetMapping("/api/v1/status")
    public Map<String, String> status() {
        return Map.of("application", "application-two", "status", "running");
    }
}
