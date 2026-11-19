package com.borrowbox.dto;

import com.borrowbox.model.Category;
import com.borrowbox.model.ItemCondition;
import com.borrowbox.model.ItemStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemUpdateRequest {

    private String title;
    private String description;
    private Category category;
    private ItemCondition itemCondition;
    private ItemStatus status;
    private String imageUrl;
}

