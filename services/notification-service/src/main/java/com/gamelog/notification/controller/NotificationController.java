package com.gamelog.notification.controller;

import com.gamelog.notification.dto.FeedItemResponse;
import com.gamelog.notification.dto.NotificationResponse;
import com.gamelog.notification.dto.NotificationsResponse;
import com.gamelog.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// API das notificacoes, exposta pelo gateway em /api/notifications/**.
//
// Mesmo modelo do recommendation-service: o servico nao valida JWT, e o gateway
// barra qualquer rota daqui sem token (exceto o feed publico). As limitacoes
// dessa escolha estao documentadas no AuthenticationFilter do gateway.
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Feed publico: ultimas avaliacoes publicadas na comunidade. Declarado antes
    // de /{username} so por legibilidade - o Spring ja prefere o caminho literal.
    @GetMapping("/feed")
    public List<FeedItemResponse> feed(@RequestParam(defaultValue = "20") int limit) {
        return notificationService.feed(limit);
    }

    @GetMapping("/{username}")
    public NotificationsResponse inbox(@PathVariable String username,
                                       @RequestParam(defaultValue = "20") int limit) {
        return notificationService.inbox(username, limit);
    }

    @PostMapping("/{username}/{id}/read")
    public NotificationResponse markRead(@PathVariable String username, @PathVariable Long id) {
        return notificationService.markRead(username, id);
    }

    @PostMapping("/{username}/read-all")
    public Map<String, Integer> markAllRead(@PathVariable String username) {
        return Map.of("updated", notificationService.markAllRead(username));
    }
}
