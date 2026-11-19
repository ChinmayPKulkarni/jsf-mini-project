package com.borrowbox.dto;

import com.borrowbox.model.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String usn;
    private String fullName;
    private String email;
    private String phone;
    private Double averageRating;
    private Integer ratingCount;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .usn(user.getUsn())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .averageRating(user.getAverageRating() != null ? Math.round(user.getAverageRating() * 10.0) / 10.0 : 0.0)
                .ratingCount(user.getRatingCount() != null ? user.getRatingCount() : 0)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

