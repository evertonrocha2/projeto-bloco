package com.gamelog.recommendation.client;

import com.gamelog.recommendation.client.dto.CatalogGamePayload;
import com.gamelog.recommendation.client.dto.GameActivityPayload;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Cliente HTTP declarativo do monolito (Spring Cloud OpenFeign).
//
// A gente escreve a INTERFACE; o Feign gera a implementacao que monta a
// requisicao, serializa, chama e desserializa a resposta. Comparado a fazer isso
// na mao com RestClient, o codigo de chamada remota fica parecido com uma chamada
// de metodo local, e o que sobra visivel e o contrato.
//
// name = "gamelog" e a peca central: NAO e um endereco, e o nome com que o
// monolito se registrou no Eureka (o spring.application.name dele). Na hora da
// chamada, o Spring Cloud LoadBalancer pergunta ao Eureka onde "gamelog" esta e
// distribui entre as instancias disponiveis.
//
// Por que isso importa: com "http://localhost:8080" fixo no codigo, subir o
// monolito em outra porta, outra maquina ou em duas instancias exigiria recompilar
// este servico. Pelo nome, nada muda aqui.
@FeignClient(name = "gamelog")
public interface GameLogClient {

    // O endpoint criado no monolito pra este consumidor (modulo integration).
    @GetMapping("/api/users/{username}/game-activity")
    GameActivityPayload getGameActivity(@PathVariable("username") String username);

    // O catalogo, que ja existia e traz genero e nota media prontos.
    @GetMapping("/api/games")
    List<CatalogGamePayload> listGames();
}
