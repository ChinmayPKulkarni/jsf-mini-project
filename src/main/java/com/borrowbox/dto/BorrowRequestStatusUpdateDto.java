package com.borrowbox.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowRequestStatusUpdateDto {

    /**
     * Supported actions:
     * - APPROVE
     * - REJECT
     * - HANDOVER (marks request as ACTIVE and item as BORROWED)
     * - RETURN (marks request as RETURNED and item as AVAILABLE)
     */
    @NotBlank(message = "Action is required (APPROVE, REJECT, HANDOVER, RETURN)")
    private String action;
}

