package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewForm {

    private Long cafeId;
    private String reviewContent;
    private Integer waitingTime;
    private String companionType;
    private Integer taste;
    private String sentiment;
    private List<String> likedTagCodes;
    private LocalDateTime reviewDate;

    public Review toEntity(Cafe cafe, Member member) {
        return Review.builder()
                .cafe(cafe)
                .member(member)
                .content(reviewContent)
                .good(0) // int 기본값 보장
                .bad(0)  // int 기본값 보장
                .waitingTime(waitingTime)
                .companionType(companionType)
                .taste(taste)
                .sentiment(sentiment) // Enum이면 컨트롤러에서 변환해서 넘겨줄 것
                .likedTagCsv(likedTagCodes != null ? String.join(",", likedTagCodes) : null)
                .createdAt(reviewDate) // null이면 @PrePersist에서 세팅되도록 둠
                .build();
    }

}