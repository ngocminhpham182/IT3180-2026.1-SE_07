package com.example.bluemoon.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class RequestOutdatedException extends RuntimeException {
    private final List<String> fields;

    public RequestOutdatedException(List<String> fields, String message) {
        super(message);
        this.fields = fields != null ? fields : Collections.emptyList();
    }
}
