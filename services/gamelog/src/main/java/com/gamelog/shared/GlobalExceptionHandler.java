package com.gamelog.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

// Centraliza o tratamento de erros de toda a API. Em vez de cada controller
// ter try/catch, qualquer excecao lancada nos services cai aqui e e convertida
// num JSON padronizado com o status HTTP certo.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    // Corpo que o Jackson nao consegue ler - tipicamente um valor que nao existe
    // no enum, como {"status": "banana"}.
    //
    // Sem este handler a excecao ficava sem tratamento e o Spring encaminhava a
    // requisicao pro /error, que exige autenticacao: o cliente recebia 403. Ou
    // seja, um corpo malformado se apresentava como problema de PERMISSAO, e quem
    // estivesse integrando iria procurar o erro no token em vez de no payload.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(
            HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Corpo da requisicao invalido ou com valor nao aceito."));
    }

    // Arquivo maior que spring.servlet.multipart.max-file-size.
    //
    // Este limite corta a requisicao ANTES de o corpo chegar ao controller, entao a
    // checagem do ImageStorageService nunca roda e a mensagem dela nunca aparece.
    // Sem este handler a excecao sobe sem tratamento e o cliente recebe 500 - ou
    // seja, "sua imagem e grande demais" se apresenta como falha do servidor, e
    // quem estiver enviando vai tentar de novo achando que foi instabilidade.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "A imagem passa de 5 MB"));
    }

    // Disparada quando uma anotacao de validacao (@NotBlank, @Min...) falha.
    // Pegamos a primeira mensagem de erro pra devolver algo legivel.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Dados invalidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }
}
