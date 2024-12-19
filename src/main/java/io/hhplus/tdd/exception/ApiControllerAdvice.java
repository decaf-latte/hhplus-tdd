package io.hhplus.tdd.exception;

import io.hhplus.tdd.point.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
@Slf4j
@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }

    @ExceptionHandler(PointException.class)
    public ResponseEntity<ErrorResponse> handlePointException(PointException e) {

        //에러로그 추가
        log.error("에러 발생: 상태 코드={}, 메시지={}, 발생 원인={}",
                e.getStatus().value(),
                e.getMessage(),
                e.getCause() != null ? e.getCause().getMessage() : "없음");

        return ResponseEntity
                .status(e.getStatus()) // 상태 코드 가져오기
                .body(new ErrorResponse(String.valueOf(e.getStatus().value()), e.getMessage()));
    }
}
