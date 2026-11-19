package com.borrowbox.dto;

import com.borrowbox.model.BorrowRequest;
import com.borrowbox.model.Category;
import com.borrowbox.model.ItemCondition;
import com.borrowbox.model.RequestStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowRequestResponse {

    private Long id;

    // Item details
    private Long itemId;
    private String itemTitle;
    private Category itemCategory;
    private ItemCondition itemCondition;
    private String itemImageUrl;

    // Owner details
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;

    // Borrower details
    private Long borrowerId;
    private String borrowerName;
    private String borrowerEmail;
    private String borrowerPhone;
    private String borrowerUsn;

    // Date range & status
    private LocalDate startDate;
    private LocalDate endDate;
    private RequestStatus status;
    private boolean overdue;
    private boolean reviewedByMe;

    private LocalDateTime createdAt;

    public static BorrowRequestResponse fromEntity(BorrowRequest request, Long currentUserId, boolean hasReviewed) {
        if (request == null) return null;

        boolean isOverdue = (request.getStatus() == RequestStatus.ACTIVE || request.getStatus() == RequestStatus.OVERDUE)
                && request.getEndDate().isBefore(LocalDate.now());

        RequestStatus computedStatus = isOverdue ? RequestStatus.OVERDUE : request.getStatus();

        return BorrowRequestResponse.builder()
                .id(request.getId())
                .itemId(request.getItem().getId())
                .itemTitle(request.getItem().getTitle())
                .itemCategory(request.getItem().getCategory())
                .itemCondition(request.getItem().getItemCondition())
                .itemImageUrl(request.getItem().getImageUrl())
                .ownerId(request.getItem().getOwner().getId())
                .ownerName(request.getItem().getOwner().getFullName())
                .ownerEmail(request.getItem().getOwner().getEmail())
                .ownerPhone(request.getItem().getOwner().getPhone())
                .borrowerId(request.getBorrower().getId())
                .borrowerName(request.getBorrower().getFullName())
                .borrowerEmail(request.getBorrower().getEmail())
                .borrowerPhone(request.getBorrower().getPhone())
                .borrowerUsn(request.getBorrower().getUsn())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(computedStatus)
                .overdue(isOverdue)
                .reviewedByMe(hasReviewed)
                .createdAt(request.getCreatedAt())
                .build();
    }
}

