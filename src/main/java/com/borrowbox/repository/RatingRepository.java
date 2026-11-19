package com.borrowbox.repository;

import com.borrowbox.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    List<Rating> findByBorrowRequestId(Long borrowRequestId);

    boolean existsByBorrowRequestIdAndReviewerId(Long borrowRequestId, Long reviewerId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.reviewee.id = :userId")
    Optional<Double> calculateAverageRatingForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.reviewee.id = :userId")
    long countRatingsForUser(@Param("userId") Long userId);
}

