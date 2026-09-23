package com.gamelog.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.Observation;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TracingNoiseFiltersTest {

    @Test
    void servletDescartaActuatorEMantemRequisicaoDeNegocio() {
        assertThat(TracingNoiseFilters.notServletActuatorRequest("http.server.requests",
                servlet("/actuator/prometheus"))).isFalse();
        assertThat(TracingNoiseFilters.notServletActuatorRequest("http.server.requests",
                servlet("/actuator/health/readiness"))).isFalse();
        assertThat(TracingNoiseFilters.notServletActuatorRequest("http.server.requests",
                servlet("/api/games/7/reviews"))).isTrue();
    }

    @Test
    void reativoDescartaActuatorEMantemRequisicaoDeNegocio() {
        assertThat(TracingNoiseFilters.notReactiveActuatorRequest("http.server.requests",
                reactive("/actuator/prometheus"))).isFalse();
        assertThat(TracingNoiseFilters.notReactiveActuatorRequest("http.server.requests",
                reactive("/api/recommendations/ana"))).isTrue();
    }

    @Test
    void descartaHeartbeatDoEurekaEMantemOutrasChamadasHttp() {
        assertThat(TracingNoiseFilters.notEurekaClientCall("http.client.requests",
                client("http://discovery-server:8761/eureka/apps/GAMELOG"))).isFalse();
        assertThat(TracingNoiseFilters.notEurekaClientCall("http.client.requests",
                client("https://api.rawg.io/api/games"))).isTrue();
    }

    @Test
    void outrosTiposDeObservationPassamIntactos() {
        Observation.Context rabbit = new Observation.Context();
        assertThat(TracingNoiseFilters.notServletActuatorRequest("spring.rabbit.listener", rabbit)).isTrue();
        assertThat(TracingNoiseFilters.notReactiveActuatorRequest("spring.rabbit.listener", rabbit)).isTrue();
        assertThat(TracingNoiseFilters.notEurekaClientCall("spring.rabbit.listener", rabbit)).isTrue();
    }

    private static ServerRequestObservationContext servlet(String uri) {
        return new ServerRequestObservationContext(new MockHttpServletRequest("GET", uri),
                new MockHttpServletResponse());
    }

    private static org.springframework.http.server.reactive.observation.ServerRequestObservationContext reactive(
            String path) {
        return new org.springframework.http.server.reactive.observation.ServerRequestObservationContext(
                MockServerHttpRequest.get(path).build(), new MockServerHttpResponse(), java.util.Map.of());
    }

    private static ClientRequestObservationContext client(String url) {
        return new ClientRequestObservationContext(new MockClientHttpRequest(HttpMethod.GET, URI.create(url)));
    }
}
