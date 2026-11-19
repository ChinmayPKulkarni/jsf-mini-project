package com.borrowbox.repository;

import com.borrowbox.model.BorrowRequest;
import com.borrowbox.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    List<BorrowRequest> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    List<BorrowRequest> findByItemOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<BorrowRequest> findByItemIdOrderByCreatedAtDesc(Long itemId);

    // Checks if there are any active/approved requests overlapping with the given date range for an item
    @Query("SELECT r FROM BorrowRequest r WHERE r.item.id = :itemId " +
           "AND r.status IN :statuses " +
           "AND r.startDate <= :endDate " +
           "AND r.endDate >= :startDate")
    List<BorrowRequest> findOverlappingRequests(
            @Param("itemId") Long itemId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<RequestStatus> statuses
    );

    // Find conflicting pending requests (excluding a specific request) for auto-rejection when approving
    @Query("SELECT r FROM BorrowRequest r WHERE r.item.id = :itemId " +
           "AND r.id <> :excludeRequestId " +
           "AND r.status = :pendingStatus " +
           "AND r.startDate <= :endDate " +
           "AND r.endDate >= :startDate")
    List<BorrowRequest> findConflictingPendingRequests(
            @Param("itemId") Long itemId,
            @Param("excludeRequestId") Long excludeRequestId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("pendingStatus") RequestStatus pendingStatus
    );

    // Find all ACTIVE requests whose end_date has passed today
    @Query("SELECT r FROM BorrowRequest r WHERE r.status = :activeStatus AND r.endDate < :today")
    List<BorrowRequest> findOverdueActiveRequests(
            @Param("activeStatus") RequestStatus activeStatus,
            @Param("today") LocalDate today
    );

    // Count active or approved requests for an item to prevent deletion
    @Query("SELECT COUNT(r) FROM BorrowRequest r WHERE r.item.id = :itemId AND r.status IN :statuses")
    long countByItemIdAndStatusIn(
            @Param("itemId") Long itemId,
            @Param("statuses") Collection<RequestStatus> statuses
    );
}

