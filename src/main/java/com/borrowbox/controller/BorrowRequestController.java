package com.borrowbox.controller;

import com.borrowbox.dto.BorrowRequestCreateDto;
import com.borrowbox.dto.BorrowRequestResponse;
import com.borrowbox.dto.BorrowRequestStatusUpdateDto;
import com.borrowbox.service.BorrowRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;

    @PostMapping
    public ResponseEntity<BorrowRequestResponse> createRequest(@Valid @RequestBody BorrowRequestCreateDto dto) {
        BorrowRequestResponse response = borrowRequestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-borrows")
    public ResponseEntity<List<BorrowRequestResponse>> getMyBorrows() {
        List<BorrowRequestResponse> list = borrowRequestService.getMyBorrows();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<BorrowRequestResponse>> getIncomingRequests() {
        List<BorrowRequestResponse> list = borrowRequestService.getIncomingRequests();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<BorrowRequestResponse>> getItemRequests(@PathVariable Long itemId) {
        List<BorrowRequestResponse> list = borrowRequestService.getItemRequests(itemId);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BorrowRequestResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BorrowRequestStatusUpdateDto dto
    ) {
        BorrowRequestResponse response = borrowRequestService.updateRequestStatus(id, dto);
        return ResponseEntity.ok(response);
    }
}

