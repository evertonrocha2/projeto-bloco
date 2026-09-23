package com.gamelog.notification.controller;

import com.gamelog.notification.service.NotificationNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

    // 404 tambem quando a notificacao existe mas e de outra pessoa: dizer "existe,
    // mas nao e sua" revelaria ids alheios.
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotificationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
