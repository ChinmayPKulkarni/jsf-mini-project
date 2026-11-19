package com.borrowbox.controller;

import com.borrowbox.dto.ItemCreateRequest;
import com.borrowbox.dto.ItemResponse;
import com.borrowbox.dto.ItemUpdateRequest;
import com.borrowbox.model.Category;
import com.borrowbox.model.ItemCondition;
import com.borrowbox.model.ItemStatus;
import com.borrowbox.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) ItemCondition condition,
            @RequestParam(required = false) ItemStatus status
    ) {
        List<ItemResponse> items = itemService.searchItems(keyword, category, condition, status);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        ItemResponse item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ItemResponse>> getMyItems() {
        List<ItemResponse> items = itemService.getMyItems();
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemCreateRequest request) {
        ItemResponse response = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long id,
            @RequestBody ItemUpdateRequest request
    ) {
        ItemResponse response = itemService.updateItem(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Item deleted successfully"));
    }
}

