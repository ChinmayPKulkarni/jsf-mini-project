package com.borrowbox.service;

import com.borrowbox.dto.BorrowRequestCreateDto;
import com.borrowbox.dto.BorrowRequestResponse;
import com.borrowbox.dto.BorrowRequestStatusUpdateDto;
import com.borrowbox.exception.BadRequestException;
import com.borrowbox.exception.ConflictException;
import com.borrowbox.exception.ResourceNotFoundException;
import com.borrowbox.model.*;
import com.borrowbox.repository.BorrowRequestRepository;
import com.borrowbox.repository.ItemRepository;
import com.borrowbox.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowRequestService {

    private final BorrowRequestRepository borrowRequestRepository;
    private final ItemRepository itemRepository;
    private final RatingRepository ratingRepository;
    private final AuthService authService;

    private static final List<RequestStatus> OCCUPIED_STATUSES = Arrays.asList(
            RequestStatus.APPROVED,
            RequestStatus.ACTIVE,
            RequestStatus.OVERDUE
    );

    @Transactional
    public BorrowRequestResponse createRequest(BorrowRequestCreateDto dto) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        LocalDate today = LocalDate.now();
        if (dto.getStartDate().isBefore(today)) {
            throw new BadRequestException("Start date cannot be in the past");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("End date cannot be earlier than start date");
        }

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + dto.getItemId()));

        if (item.getStatus() == ItemStatus.UNAVAILABLE) {
            throw new BadRequestException("This item is currently marked as unavailable for borrowing");
        }

        if (item.getOwner().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot borrow your own listed item");
        }

        // Conflict & overlap check
        List<BorrowRequest> overlaps = borrowRequestRepository.findOverlappingRequests(
                item.getId(),
                dto.getStartDate(),
                dto.getEndDate(),
                OCCUPIED_STATUSES
        );

        if (!overlaps.isEmpty()) {
            BorrowRequest existing = overlaps.get(0);
            throw new ConflictException("The item is already booked from " +
                    existing.getStartDate() + " to " + existing.getEndDate());
        }

        BorrowRequest request = BorrowRequest.builder()
                .item(item)
                .borrower(currentUser)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(RequestStatus.PENDING)
                .build();

        BorrowRequest saved = borrowRequestRepository.save(request);
        return BorrowRequestResponse.fromEntity(saved, currentUser.getId(), false);
    }

    public List<BorrowRequestResponse> getMyBorrows() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        updateOverdueStatusIfApplicable();

        List<BorrowRequest> requests = borrowRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(currentUser.getId());
        return requests.stream()
                .map(r -> {
                    boolean reviewed = ratingRepository.existsByBorrowRequestIdAndReviewerId(r.getId(), currentUser.getId());
                    return BorrowRequestResponse.fromEntity(r, currentUser.getId(), reviewed);
                })
                .collect(Collectors.toList());
    }

    public List<BorrowRequestResponse> getIncomingRequests() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        updateOverdueStatusIfApplicable();

        List<BorrowRequest> requests = borrowRequestRepository.findByItemOwnerIdOrderByCreatedAtDesc(currentUser.getId());
        return requests.stream()
                .map(r -> {
                    boolean reviewed = ratingRepository.existsByBorrowRequestIdAndReviewerId(r.getId(), currentUser.getId());
                    return BorrowRequestResponse.fromEntity(r, currentUser.getId(), reviewed);
                })
                .collect(Collectors.toList());
    }

    public List<BorrowRequestResponse> getItemRequests(Long itemId) {
        List<BorrowRequest> requests = borrowRequestRepository.findByItemIdOrderByCreatedAtDesc(itemId);
        User currentUser = authService.getCurrentUserResponse() != null ? authService.getCurrentAuthenticatedUser() : null;
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        return requests.stream()
                .map(r -> BorrowRequestResponse.fromEntity(r, currentUserId, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public BorrowRequestResponse updateRequestStatus(Long requestId, BorrowRequestStatusUpdateDto dto) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        BorrowRequest request = borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow request not found with id: " + requestId));

        Item item = request.getItem();
        boolean isOwner = item.getOwner().getId().equals(currentUser.getId());
        boolean isBorrower = request.getBorrower().getId().equals(currentUser.getId());

        String action = dto.getAction().trim().toUpperCase();

        switch (action) {
            case "APPROVE":
                if (!isOwner) {
                    throw new BadRequestException("Only the item owner can approve this request");
                }
                if (request.getStatus() != RequestStatus.PENDING) {
                    throw new BadRequestException("Only pending requests can be approved");
                }

                // Check again for conflicts
                List<BorrowRequest> overlaps = borrowRequestRepository.findOverlappingRequests(
                        item.getId(),
                        request.getStartDate(),
                        request.getEndDate(),
                        OCCUPIED_STATUSES
                );
                if (!overlaps.isEmpty()) {
                    throw new ConflictException("Cannot approve: item already booked for overlapping dates");
                }

                request.setStatus(RequestStatus.APPROVED);

                // FR-4.3: Automatically reject conflicting pending requests for the same item and dates
                List<BorrowRequest> conflictingPending = borrowRequestRepository.findConflictingPendingRequests(
                        item.getId(),
                        request.getId(),
                        request.getStartDate(),
                        request.getEndDate(),
                        RequestStatus.PENDING
                );
                for (BorrowRequest conflict : conflictingPending) {
                    conflict.setStatus(RequestStatus.REJECTED);
                    borrowRequestRepository.save(conflict);
                }
                break;

            case "REJECT":
                if (!isOwner && !isBorrower) {
                    throw new BadRequestException("Unauthorized to reject this request");
                }
                if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.APPROVED) {
                    throw new BadRequestException("Cannot reject request with status " + request.getStatus());
                }
                request.setStatus(RequestStatus.REJECTED);
                break;

            case "HANDOVER":
                if (!isOwner) {
                    throw new BadRequestException("Only the item owner can confirm handover");
                }
                if (request.getStatus() != RequestStatus.APPROVED) {
                    throw new BadRequestException("Item must be in APPROVED status before handover");
                }
                request.setStatus(RequestStatus.ACTIVE);
                item.setStatus(ItemStatus.BORROWED);
                itemRepository.save(item);
                break;

            case "RETURN":
                if (!isOwner) {
                    throw new BadRequestException("Only the item owner can confirm item return");
                }
                if (request.getStatus() != RequestStatus.ACTIVE &&
                    request.getStatus() != RequestStatus.OVERDUE &&
                    request.getStatus() != RequestStatus.APPROVED) {
                    throw new BadRequestException("Request must be ACTIVE or OVERDUE to mark as RETURNED");
                }
                request.setStatus(RequestStatus.RETURNED);

                // Restore item status to AVAILABLE
                item.setStatus(ItemStatus.AVAILABLE);
                itemRepository.save(item);
                break;

            case "CANCEL":
                if (!isBorrower) {
                    throw new BadRequestException("Only the borrower can cancel their request");
                }
                if (request.getStatus() != RequestStatus.PENDING) {
                    throw new BadRequestException("Only pending requests can be cancelled");
                }
                request.setStatus(RequestStatus.REJECTED);
                break;

            default:
                throw new BadRequestException("Invalid status action: " + action);
        }

        BorrowRequest saved = borrowRequestRepository.save(request);
        boolean reviewed = ratingRepository.existsByBorrowRequestIdAndReviewerId(saved.getId(), currentUser.getId());
        return BorrowRequestResponse.fromEntity(saved, currentUser.getId(), reviewed);
    }

    @Transactional
    public void updateOverdueStatusIfApplicable() {
        LocalDate today = LocalDate.now();
        List<BorrowRequest> overdues = borrowRequestRepository.findOverdueActiveRequests(RequestStatus.ACTIVE, today);
        for (BorrowRequest r : overdues) {
            r.setStatus(RequestStatus.OVERDUE);
            borrowRequestRepository.save(r);
        }
    }
}

