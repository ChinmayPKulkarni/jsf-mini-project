package com.borrowbox.dto;

import com.borrowbox.model.Category;
import com.borrowbox.model.ItemCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCreateRequest {

    @NotBlank(message = "Item title is required")
    private String title;

    private String description;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Condition is required")
    private ItemCondition itemCondition;

    private String imageUrl;
}

