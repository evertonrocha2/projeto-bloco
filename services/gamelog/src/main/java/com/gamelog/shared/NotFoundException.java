package com.gamelog.shared;

// Lancada quando algo que foi pedido nao existe (jogo, usuario, etc).
// O handler global traduz isso pra um HTTP 404.
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
