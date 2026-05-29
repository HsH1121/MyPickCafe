package com.example.MyPickCafe.apiController;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthTestController {

    @GetMapping("/public/ping")
    public Map<String, String> publicPing() {
        return Map.of("ok", "public");
    }

    @GetMapping("/private/ping")
    public Map<String, String> privatePing() {
        return Map.of("ok", "private");
    }
}
