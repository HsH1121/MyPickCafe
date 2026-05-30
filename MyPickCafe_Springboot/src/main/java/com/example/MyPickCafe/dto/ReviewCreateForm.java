// ReviewCreateForm.java
package com.example.MyPickCafe.dto;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReviewCreateForm {
    private Long cafeId;
    private String reviewContent;
    private MultipartFile[] photos;
    private String sentiment;
}
