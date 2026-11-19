package com.borrowbox.repository;

import com.borrowbox.model.Category;
import com.borrowbox.model.Item;
import com.borrowbox.model.ItemCondition;
import com.borrowbox.model.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT i FROM Item i WHERE " +
           "(:keyword IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:category IS NULL OR i.category = :category) " +
           "AND (:condition IS NULL OR i.itemCondition = :condition) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.createdAt DESC")
    List<Item> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("condition") ItemCondition condition,
            @Param("status") ItemStatus status
    );
}

