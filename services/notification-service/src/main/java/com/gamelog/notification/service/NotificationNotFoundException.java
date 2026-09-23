package com.gamelog.notification.service;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long id) {
        super("Notificacao " + id + " nao encontrada");
    }
}
