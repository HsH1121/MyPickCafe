package com.example.MyPickCafe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PythonTagResponse {
    private String       sentiment;

    @JsonProperty("FACILITY")
    private List<String> facilityTags;

    @JsonProperty("MENU")
    private List<String> menuTags;

    @JsonProperty("PURPOSE")
    private List<String> purposeTags;

    @JsonProperty("MOOD")
    private List<String> moodTags;
}
