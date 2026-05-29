package com.example.MyPickCafe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PythonTagResponse {
    private String       sentiment;    // "GOOD" | "BAD" | null
    private List<String> facilityTags; // e.g. ["WIFI", "PLUG"]
    private List<String> menuTags;     // e.g. ["AMERICANO", "CAKE"]
    private List<String> purposeTags;  // e.g. ["STUDY"]
    private List<String> moodTags;     // e.g. ["MODERN"]
}
