package com.borrowbox.controller;

import com.borrowbox.dto.RatingCreateDto;
import com.borrowbox.dto.RatingResponse;
import com.borrowbox.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/api/ratings")
    public ResponseEntity<RatingResponse> submitRating(@Valid @RequestBody RatingCreateDto dto) {
        RatingResponse response = ratingService.submitRating(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/users/{userId}/ratings")
    public ResponseEntity<List<RatingResponse>> getRatingsForUser(@PathVariable Long userId) {
        List<RatingResponse> list = ratingService.getRatingsForUser(userId);
        return ResponseEntity.ok(list);
    }
}

