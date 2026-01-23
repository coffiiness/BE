package com.team.voteland.core.api.controller;

import com.team.voteland.support.error.CoreException;
import com.team.voteland.support.error.ErrorType;
import com.team.voteland.core.support.response.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerAdvice {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        log.info("ValidationException : {}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ErrorType.VALIDATION_ERROR),
                ErrorType.VALIDATION_ERROR.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.info("IllegalArgumentException : {}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ErrorType.UNAUTHORIZED), ErrorType.UNAUTHORIZED.getStatus());
    }

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<?>> handleCoreException(CoreException e) {
        switch (e.getErrorType().getLogLevel()) {
            case ERROR -> log.error("CoreException : {}", e.getMessage(), e);
            case WARN -> log.warn("CoreException : {}", e.getMessage(), e);
            default -> log.info("CoreException : {}", e.getMessage(), e);
        }
        return new ResponseEntity<>(ApiResponse.error(e.getErrorType(), e.getData()), e.getErrorType().getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Exception : {}", e.getMessage(), e);
        return new ResponseEntity<>(ApiResponse.error(ErrorType.DEFAULT_ERROR), ErrorType.DEFAULT_ERROR.getStatus());
    }

}
