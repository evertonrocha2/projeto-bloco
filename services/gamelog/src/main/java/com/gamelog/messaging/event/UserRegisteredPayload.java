package com.gamelog.messaging.event;

// Payload de user.registered. So o username: email e bio sao dado pessoal, e
// nenhum consumidor atual precisa deles. Evento e contrato publico - o que entra
// aqui passa a ser visto por qualquer servico que assinar.
public record UserRegisteredPayload(String username) {
}
