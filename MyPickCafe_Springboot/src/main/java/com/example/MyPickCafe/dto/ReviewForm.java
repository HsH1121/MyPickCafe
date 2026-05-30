package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewForm {

    private Long cafeId;
    private String reviewContent;
    private String sentiment;
    private LocalDateTime reviewDate;

    public Review toEntity(Cafe cafe, Member member) {
        return Review.builder()
                .cafe(cafe)
                .member(member)
                .content(reviewContent)
                .good(0)
                .bad(0)
                .sentiment(sentiment)
                .createdAt(reviewDate)
                .build();
    }

}