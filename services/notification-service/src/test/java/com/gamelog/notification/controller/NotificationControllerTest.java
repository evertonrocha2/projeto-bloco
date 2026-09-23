package com.gamelog.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gamelog.notification.domain.FeedItem;
import com.gamelog.notification.domain.Notification;
import com.gamelog.notification.domain.NotificationType;
import com.gamelog.notification.repository.FeedItemRepository;
import com.gamelog.notification.repository.NotificationRepository;
import com.gamelog.notification.service.NotificationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// O contrato HTTP que o front consome pelo gateway.
@DataJpaTest
class NotificationControllerTest {

    private static final Instant T0 = Instant.parse("2026-09-20T12:00:00Z");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FeedItemRepository feedItemRepository;

    private MockMvc mockMvc;
    private Notification paraAna;

    @BeforeEach
    void setUp() {
        NotificationService service = new NotificationService(
                notificationRepository, feedItemRepository, Clock.fixed(T0, ZoneOffset.UTC));
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new NotificationExceptionHandler())
                .build();

        paraAna = notificationRepository.save(new Notification("ana", NotificationType.REVIEW_REPLY,
                "beto respondeu", "beto", 7L, 1L, "e-1", T0));
        notificationRepository.save(new Notification("ana", NotificationType.WELCOME,
                "bem-vinda", null, null, null, "e-0", T0.minusSeconds(60)));
        notificationRepository.save(new Notification("beto", NotificationType.WELCOME,
                "bem-vindo", null, null, null, "e-2", T0));
        feedItemRepository.save(new FeedItem(7L, "ana", 1L, "Zelda", 5, T0));
    }

    @Test
    void caixaDeEntradaMaisRecentePrimeiroComContagemDeNaoLidas() throws Exception {
        mockMvc.perform(get("/api/notifications/ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].type").value("REVIEW_REPLY"))
                .andExpect(jsonPath("$.items[0].read").value(false));
    }

    @Test
    void marcarComoLida() throws Exception {
        mockMvc.perform(post("/api/notifications/ana/{id}/read", paraAna.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/notifications/ana"))
                .andExpect(jsonPath("$.unread").value(1));
    }

    @Test
    void naoDaPraMarcarANotificacaoDeOutraPessoa() throws Exception {
        mockMvc.perform(post("/api/notifications/beto/{id}/read", paraAna.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void marcarTodasSoAfetaODono() throws Exception {
        mockMvc.perform(post("/api/notifications/ana/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        mockMvc.perform(get("/api/notifications/beto"))
                .andExpect(jsonPath("$.unread").value(1));
    }

    @Test
    void feedPublico() throws Exception {
        mockMvc.perform(get("/api/notifications/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gameTitle").value("Zelda"))
                .andExpect(jsonPath("$[0].rating").value(5));
    }
}
