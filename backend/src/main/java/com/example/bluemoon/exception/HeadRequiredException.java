package com.example.bluemoon.exception;

import java.util.List;

public class HeadRequiredException extends RuntimeException {

    private final List<CandidateResponse> candidates;

    public HeadRequiredException(List<CandidateResponse> candidates) {
        super("Người này là chủ hộ. Hãy chọn chủ hộ mới trước khi ghi nhận.");
        this.candidates = candidates;
    }

    public List<CandidateResponse> getCandidates() {
        return candidates;
    }

    public record CandidateResponse(
            Long id,
            String fullName,
            String relationshipToHead
    ) {}
}
