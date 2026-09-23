package com.gamelog.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// Servico de notificacoes e feed da comunidade (TP4).
//
// Nasceu direto orientado a eventos: nao tem nenhum cliente HTTP pro monolito e o
// monolito nao sabe que ele existe. Tudo que ele sabe chega pela fila
// notification.events, assinada no exchange gamelog.events.
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
