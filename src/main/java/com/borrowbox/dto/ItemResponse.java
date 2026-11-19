package com.borrowbox.dto;

import com.borrowbox.model.Category;
import com.borrowbox.model.Item;
import com.borrowbox.model.ItemCondition;
import com.borrowbox.model.ItemStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemResponse {

    private Long id;
    private String title;
    private String description;
    private Category category;
    private ItemCondition itemCondition;
    private ItemStatus status;
    private String imageUrl;
    private LocalDateTime createdAt;

    // Owner details
    private Long ownerId;
    private String ownerName;
    private String ownerUsn;
    private Double ownerAverageRating;
    private Integer ownerRatingCount;

    public static ItemResponse fromEntity(Item item) {
        if (item == null) return null;
        return ItemResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .category(item.getCategory())
                .itemCondition(item.getItemCondition())
                .status(item.getStatus())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .ownerId(item.getOwner() != null ? item.getOwner().getId() : null)
                .ownerName(item.getOwner() != null ? item.getOwner().getFullName() : null)
                .ownerUsn(item.getOwner() != null ? item.getOwner().getUsn() : null)
                .ownerAverageRating(item.getOwner() != null && item.getOwner().getAverageRating() != null 
                        ? Math.round(item.getOwner().getAverageRating() * 10.0) / 10.0 : 0.0)
                .ownerRatingCount(item.getOwner() != null && item.getOwner().getRatingCount() != null
                        ? item.getOwner().getRatingCount() : 0)
                .build();
    }
}

