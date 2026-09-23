package com.gamelog.observability;

import io.micrometer.observation.Observation;

// As regras em si, separadas da configuracao pra poderem ser testadas sem subir
// um contexto Spring. Cada metodo devolve true pra "manter a observation".
//
// Os metodos servlet e reativo citam classes que so existem numa das pilhas. Isso
// e seguro porque a JVM so resolve a classe quando o metodo executa, e a
// auto-configuracao so registra o metodo da pilha presente.
final class TracingNoiseFilters {

    static final String ACTUATOR_PREFIX = "/actuator";

    private TracingNoiseFilters() {
    }

    static boolean notServletActuatorRequest(String name, Observation.Context context) {
        if (context instanceof org.springframework.http.server.observation.ServerRequestObservationContext server) {
            return !server.getCarrier().getRequestURI().startsWith(ACTUATOR_PREFIX);
        }
        return true;
    }

    static boolean notReactiveActuatorRequest(String name, Observation.Context context) {
        if (context instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext server) {
            return !server.getCarrier().getPath().value().startsWith(ACTUATOR_PREFIX);
        }
        return true;
    }

    static boolean notEurekaClientCall(String name, Observation.Context context) {
        if (context instanceof org.springframework.http.client.observation.ClientRequestObservationContext client
                && client.getCarrier() != null) {
            return !client.getCarrier().getURI().getPath().contains("/eureka/");
        }
        return true;
    }
}
