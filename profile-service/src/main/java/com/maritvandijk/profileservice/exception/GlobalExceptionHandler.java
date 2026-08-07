package com.maritvandijk.profileservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<ProblemDetail> handleOrderServiceException(OrderServiceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage()));
    }

    @ExceptionHandler(RecommendationServiceException.class)
    public ResponseEntity<ProblemDetail> handleRecommendationServiceException(RecommendationServiceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something unexpected happened"));
    }
}
