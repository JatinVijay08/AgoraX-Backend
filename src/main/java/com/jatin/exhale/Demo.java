package com.jatin.exhale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Demo {
    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }
}
