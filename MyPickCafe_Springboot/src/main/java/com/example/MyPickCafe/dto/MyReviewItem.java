// src/main/java/com/example/MyPickCafe/dto/MyReviewItem.java
package com.example.MyPickCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
public class MyReviewItem {
    private Long reviewId;
    private Long cafeId;
    private String cafeName;
    private String cafePhotoUrl;
    private String reviewContent;
    private LocalDateTime reviewDate;
    private int reviewGood;
    private int reviewBad;

    public String getReviewDateFmt() {
        return (reviewDate == null) ? "" :
                reviewDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getExcerpt() {
        if (reviewContent == null) {
            return "";
        }
        String str = reviewContent.strip();
        return (str.length() > 120) ? str.substring(0, 80) + "..." : str;
    }
}
