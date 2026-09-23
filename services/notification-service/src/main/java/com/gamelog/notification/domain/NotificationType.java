package com.gamelog.notification.domain;

public enum NotificationType {
    WELCOME,
    // Alguem respondeu a SUA avaliacao.
    REVIEW_REPLY,
    // Alguem respondeu a SUA resposta, na avaliacao de outra pessoa.
    THREAD_REPLY,
    REVIEW_VOTE
}
