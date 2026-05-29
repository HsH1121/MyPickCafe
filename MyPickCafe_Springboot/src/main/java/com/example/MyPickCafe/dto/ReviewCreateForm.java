// ReviewCreateForm.java
package com.example.MyPickCafe.dto;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class ReviewCreateForm {
    private Long cafeId;
    private String reviewContent;

    // 추가 메타(옵션) - 엔티티 변경 없이 태그로 저장 가능
    private Integer waitingTime;       // 분
    private String companionType;      // SOLO/FRIEND/FAMILY/DATE 등
    private Integer taste;             // 1~5
    private List<String> likedTagCodes; // "맛있어요","친절해요" ...

    private MultipartFile[] photos;    // 기존 이름 그대로 'photos'

    private String sentiment;
}
