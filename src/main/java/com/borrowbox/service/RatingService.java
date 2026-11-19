package com.borrowbox.service;

import com.borrowbox.dto.RatingCreateDto;
import com.borrowbox.dto.RatingResponse;
import com.borrowbox.exception.BadRequestException;
import com.borrowbox.exception.ConflictException;
import com.borrowbox.exception.ResourceNotFoundException;
import com.borrowbox.model.BorrowRequest;
import com.borrowbox.model.Rating;
import com.borrowbox.model.RequestStatus;
import com.borrowbox.model.User;
import com.borrowbox.repository.BorrowRequestRepository;
import com.borrowbox.repository.RatingRepository;
import com.borrowbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional
    public RatingResponse submitRating(RatingCreateDto dto) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        BorrowRequest borrowRequest = borrowRequestRepository.findById(dto.getBorrowRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrow request not found with id: " + dto.getBorrowRequestId()));

        if (borrowRequest.getStatus() != RequestStatus.RETURNED) {
            throw new BadRequestException("Ratings can only be submitted after an item has been returned (status: RETURNED)");
        }

        boolean isBorrower = borrowRequest.getBorrower().getId().equals(currentUser.getId());
        boolean isOwner = borrowRequest.getItem().getOwner().getId().equals(currentUser.getId());

        if (!isBorrower && !isOwner) {
            throw new BadRequestException("You are not a participant in this exchange");
        }

        if (ratingRepository.existsByBorrowRequestIdAndReviewerId(dto.getBorrowRequestId(), currentUser.getId())) {
            throw new ConflictException("You have already submitted a rating for this exchange");
        }

        User reviewee = isBorrower ? borrowRequest.getItem().getOwner() : borrowRequest.getBorrower();

        Rating rating = Rating.builder()
                .borrowRequest(borrowRequest)
                .reviewer(currentUser)
                .reviewee(reviewee)
                .score(dto.getScore())
                .comment(dto.getComment() != null ? dto.getComment().trim() : null)
                .build();

        Rating saved = ratingRepository.save(rating);

        // Recalculate reviewee average rating and count
        Double avg = ratingRepository.calculateAverageRatingForUser(reviewee.getId()).orElse(0.0);
        long count = ratingRepository.countRatingsForUser(reviewee.getId());
        reviewee.setAverageRating(avg);
        reviewee.setRatingCount((int) count);
        userRepository.save(reviewee);

        return RatingResponse.fromEntity(saved);
    }

    public List<RatingResponse> getRatingsForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        List<Rating> ratings = ratingRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);
        return ratings.stream().map(RatingResponse::fromEntity).collect(Collectors.toList());
    }
}

