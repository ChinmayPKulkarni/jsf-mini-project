package com.borrowbox.dto;

import com.borrowbox.model.Rating;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponse {

    private Long id;
    private Long borrowRequestId;

    private Long reviewerId;
    private String reviewerName;
    private String reviewerUsn;

    private Long revieweeId;
    private String revieweeName;

    private Integer score;
    private String comment;
    private LocalDateTime createdAt;

    public static RatingResponse fromEntity(Rating rating) {
        if (rating == null) return null;
        return RatingResponse.builder()
                .id(rating.getId())
                .borrowRequestId(rating.getBorrowRequest().getId())
                .reviewerId(rating.getReviewer().getId())
                .reviewerName(rating.getReviewer().getFullName())
                .reviewerUsn(rating.getReviewer().getUsn())
                .revieweeId(rating.getReviewee().getId())
                .revieweeName(rating.getReviewee().getFullName())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}

