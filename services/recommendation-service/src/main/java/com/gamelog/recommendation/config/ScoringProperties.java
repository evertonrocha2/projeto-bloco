package com.gamelog.recommendation.config;

import com.gamelog.recommendation.domain.ScoringWeights;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Traz os pesos do algoritmo da configuracao pro codigo.
//
// === Onde a configuracao distribuida aparece de verdade ===
//
// Estes valores vem de config-repo/recommendation-service.yml, servido pelo
// Config Server. Beans anotados com @ConfigurationProperties sao RE-VINCULADOS
// automaticamente pelo Spring Cloud quando chega um EnvironmentChangeEvent - ou
// seja, quando alguem chama POST /actuator/refresh.
//
// Na pratica: editar o .yml no config-repo e chamar /actuator/refresh muda como
// as recomendacoes sao calculadas, com o servico no ar, sem recompilar e sem
// reiniciar. E por isso que o RecommendationService chama toWeights() a cada
// requisicao em vez de guardar os pesos numa variavel - guardar impediria a nova
// configuracao de valer.
//
// Classe mutavel com setters porque e assim que o Spring faz o binding relaxado
// (min-rating -> setMinRating). Os valores iniciais aqui sao os mesmos do
// application.yml local, que por sua vez e a reserva pro caso de o Config Server
// nao responder.
@ConfigurationProperties(prefix = "recommendation.scoring")
public class ScoringProperties {

    private int minRating = 3;
    private double collectionWeight = 0.5;
    private double likedBoost = 1.5;
    private double genreWeight = 3.0;
    private double communityWeight = 2.0;
    private int maxResults = 8;

    // Converte a configuracao no valor puro que o algoritmo consome. E o ponto
    // exato onde o mundo do Spring termina e o dominio comeca.
    public ScoringWeights toWeights() {
        return new ScoringWeights(
                minRating, collectionWeight, likedBoost, genreWeight, communityWeight, maxResults);
    }

    public int getMinRating() {
        return minRating;
    }

    public void setMinRating(int minRating) {
        this.minRating = minRating;
    }

    public double getCollectionWeight() {
        return collectionWeight;
    }

    public void setCollectionWeight(double collectionWeight) {
        this.collectionWeight = collectionWeight;
    }

    public double getLikedBoost() {
        return likedBoost;
    }

    public void setLikedBoost(double likedBoost) {
        this.likedBoost = likedBoost;
    }

    public double getGenreWeight() {
        return genreWeight;
    }

    public void setGenreWeight(double genreWeight) {
        this.genreWeight = genreWeight;
    }

    public double getCommunityWeight() {
        return communityWeight;
    }

    public void setCommunityWeight(double communityWeight) {
        this.communityWeight = communityWeight;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
