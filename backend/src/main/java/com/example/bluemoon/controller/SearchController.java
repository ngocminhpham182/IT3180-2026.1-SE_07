package com.example.bluemoon.controller;

import com.example.bluemoon.dto.SearchResponse;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        SearchResponse response = searchService.search(keyword, type, limit);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }
}
