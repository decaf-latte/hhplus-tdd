package io.hhplus.tdd.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PointException extends RuntimeException {

    private final HttpStatus status;

    public PointException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

}