package com.gamelog.recommendation.shared;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduz erro em JSON, no MESMO formato que o monolito usa: {"error": "..."}.
//
// Manter o formato identico entre os dois servicos nao e detalhe estetico: o
// api.js do front tem uma unica funcao que le data.error pra montar a mensagem.
// Se cada servico inventasse o proprio formato, o front precisaria saber com qual
// deles esta falando - e a divisao em servicos, que o gateway esconde, voltaria a
// aparecer na tela.
@RestControllerAdvice
public class RecommendationExceptionHandler {

    // @Valid falhou (ex: gameId ausente).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Dados invalidos");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    // JSON que o Jackson nao consegue ler - tipicamente um verdict que nao existe
    // no enum. Sem este handler o Spring devolveria 400 com um corpo de erro
    // generico, fora do formato que o front espera.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(
            HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Corpo da requisicao invalido. "
                        + "verdict deve ser LIKED ou DISMISSED."));
    }
}
