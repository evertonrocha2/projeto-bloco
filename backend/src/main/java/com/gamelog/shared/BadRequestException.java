package com.gamelog.shared;

// Lancada quando o pedido em si esta errado: dado invalido, regra de negocio
// violada (ex: nota fora do intervalo, usuario ja cadastrado). Vira HTTP 400.
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
