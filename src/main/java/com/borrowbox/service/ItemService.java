package com.borrowbox.service;

import com.borrowbox.dto.ItemCreateRequest;
import com.borrowbox.dto.ItemResponse;
import com.borrowbox.dto.ItemUpdateRequest;
import com.borrowbox.exception.BadRequestException;
import com.borrowbox.exception.ConflictException;
import com.borrowbox.exception.ResourceNotFoundException;
import com.borrowbox.model.*;
import com.borrowbox.repository.BorrowRequestRepository;
import com.borrowbox.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final AuthService authService;

    @Transactional
    public ItemResponse createItem(ItemCreateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        Item item = Item.builder()
                .owner(currentUser)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .category(request.getCategory())
                .itemCondition(request.getItemCondition())
                .status(ItemStatus.AVAILABLE)
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .build();

        Item saved = itemRepository.save(item);
        return ItemResponse.fromEntity(saved);
    }

    public List<ItemResponse> searchItems(String keyword, Category category, ItemCondition condition, ItemStatus status) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<Item> items = itemRepository.searchAndFilter(trimmedKeyword, category, condition, status);
        return items.stream().map(ItemResponse::fromEntity).collect(Collectors.toList());
    }

    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return ItemResponse.fromEntity(item);
    }

    public List<ItemResponse> getMyItems() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<Item> items = itemRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId());
        return items.stream().map(ItemResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemUpdateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (!item.getOwner().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to update this item listing");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
        }
        if (request.getItemCondition() != null) {
            item.setItemCondition(request.getItemCondition());
        }
        if (request.getStatus() != null) {
            // Cannot manually set to BORROWED through edit; that happens via request approval/handover
            if (request.getStatus() == ItemStatus.BORROWED) {
                throw new BadRequestException("Item status cannot be manually set to BORROWED");
            }
            item.setStatus(request.getStatus());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl().trim());
        }

        Item updated = itemRepository.save(item);
        return ItemResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteItem(Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (!item.getOwner().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to delete this item listing");
        }

        long activeBorrows = borrowRequestRepository.countByItemIdAndStatusIn(
                id,
                List.of(RequestStatus.APPROVED, RequestStatus.ACTIVE, RequestStatus.OVERDUE)
        );
        if (activeBorrows > 0) {
            throw new ConflictException("Cannot delete item with active or approved borrow requests");
        }

        itemRepository.delete(item);
    }
}

