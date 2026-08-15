package com.gamelog.recommendation.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// O perfil de gosto de um usuario: quanto ele gosta de cada genero, numa escala
// em que o genero favorito vale 1.0.
//
// E a primeira metade do algoritmo, separada em classe propria por dois motivos:
// da pra testar isoladamente, e e o que o endpoint /taste-profile expoe. Mostrar
// o perfil na tela e o que torna a recomendacao explicavel - o usuario ve POR QUE
// aquele jogo apareceu, em vez de receber uma lista que parece adivinhacao.
//
// Classe pura: nada de Spring, banco ou HTTP.
public final class TasteProfile {

    private final Map<String, Double> weightByGenre;

    private TasteProfile(Map<String, Double> weightByGenre) {
        this.weightByGenre = weightByGenre;
    }

    // Monta o perfil a partir de tres sinais, do mais forte pro mais fraco:
    //   1. jogos avaliados bem       -> (nota - 2.5), entao nota 5 pesa mais que 4
    //   2. jogos marcados "gostei"   -> likedBoost
    //   3. jogos so na colecao       -> collectionWeight
    //
    // O catalogo entra como parametro porque colecao e feedback chegam so com o
    // id do jogo; e nele que se descobre o genero correspondente.
    public static TasteProfile from(GameActivity activity,
                                    List<CatalogGame> catalog,
                                    List<FeedbackEntry> feedback,
                                    ScoringWeights weights) {

        Map<Long, String> genreByGameId = new HashMap<>();
        for (CatalogGame game : catalog) {
            genreByGameId.put(game.gameId(), game.genre());
        }

        Map<String, Double> raw = new HashMap<>();

        // 1. Avaliacoes. Somente as que passam do limiar: nota baixa diz que a
        // pessoa NAO gostou, e tratar isso como afinidade recomendaria mais do
        // mesmo que ela rejeitou. O "- 2.5" faz nota 5 valer o dobro de nota 4
        // (2.5 contra 1.5), o que diferencia "gostei muito" de "gostei".
        for (RatedGame rated : activity.ratedGames()) {
            if (rated.rating() >= weights.minRating()) {
                addToEachGenre(raw, rated.genre(), rated.rating() - 2.5);
            }
        }

        // 2. Feedback positivo dado na propria tela de recomendacoes.
        for (FeedbackEntry entry : feedback) {
            if (entry.verdict() == FeedbackVerdict.LIKED) {
                addToEachGenre(raw, genreByGameId.get(entry.gameId()), weights.likedBoost());
            }
        }

        // 3. Colecao. Ter o jogo e um sinal mais fraco do que ter gostado dele.
        for (Long ownedId : activity.ownedGameIds()) {
            addToEachGenre(raw, genreByGameId.get(ownedId), weights.collectionWeight());
        }

        return new TasteProfile(normalise(raw));
    }

    // Divide tudo pelo maior peso, de modo que o genero favorito fique em 1.0.
    //
    // Sem normalizar, o perfil de quem avaliou 200 jogos teria pesos na casa das
    // centenas e o de quem avaliou tres ficaria perto de zero. Como o score final
    // soma afinidade com nota da comunidade, a parte da comunidade viraria
    // irrelevante no primeiro caso e dominante no segundo - o mesmo algoritmo se
    // comportaria de dois jeitos diferentes.
    private static Map<String, Double> normalise(Map<String, Double> raw) {
        double max = raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if (max <= 0.0) {
            return Map.of();
        }

        Map<String, Double> normalised = new HashMap<>();
        raw.forEach((genre, weight) -> normalised.put(genre, weight / max));
        return normalised;
    }

    // Distribui o mesmo peso por todos os generos do jogo. "Action, RPG" alimenta
    // Action e RPG.
    private static void addToEachGenre(Map<String, Double> target, String genreField, double weight) {
        for (String genre : splitGenres(genreField)) {
            target.merge(genre, weight, Double::sum);
        }
    }

    // No catalogo o genero e uma string unica ("Action, RPG") - e o formato que
    // vem da API da RAWG e que o monolito guarda desde o TP1. Aqui a gente
    // desempacota, tolerando espacos sobrando e campo vazio ou nulo.
    static List<String> splitGenres(String genreField) {
        if (genreField == null || genreField.isBlank()) {
            return List.of();
        }

        List<String> genres = new ArrayList<>();
        for (String part : genreField.split(",")) {
            String genre = part.trim();
            if (!genre.isEmpty()) {
                genres.add(genre);
            }
        }
        return genres;
    }

    // Quanto este perfil combina com um jogo de generos dados.
    //
    // E a MEDIA dos pesos dos generos do jogo, contando genero fora do perfil
    // como zero. Ignorar os desconhecidos em vez de zera-los faria "Action,
    // Sports" valer o mesmo que "Action" puro pra quem so gosta de Action - meio
    // acerto passaria a valer acerto cheio, e jogos de genero unico perderiam
    // espaco pra jogos que acertam por acidente.
    public double affinityFor(String genreField) {
        List<String> genres = splitGenres(genreField);
        if (genres.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (String genre : genres) {
            sum += weightByGenre.getOrDefault(genre, 0.0);
        }
        return sum / genres.size();
    }

    // Os generos de maior peso dentro de um jogo especifico - usado pra escrever
    // o "porque voce gosta de X e Y" que aparece no card.
    public List<String> strongestGenresIn(String genreField, int limit) {
        return splitGenres(genreField).stream()
                .filter(weightByGenre::containsKey)
                .sorted(Comparator.comparingDouble(
                        (String genre) -> weightByGenre.get(genre)).reversed())
                .limit(limit)
                .toList();
    }

    // Perfil vazio significa "nao sei nada sobre essa pessoa" - usuario novo, ou
    // que so avaliou jogos mal. O algoritmo trata esse caso recomendando os mais
    // bem avaliados da comunidade, em vez de devolver lista vazia.
    public boolean isEmpty() {
        return weightByGenre.isEmpty();
    }

    // Copia defensiva: o perfil e imutavel pra fora.
    public Map<String, Double> weights() {
        return Map.copyOf(weightByGenre);
    }

    // Generos ordenados do mais forte pro mais fraco. E isso que o endpoint
    // /taste-profile devolve pro grafico da tela.
    public List<Map.Entry<String, Double>> rankedGenres() {
        return weightByGenre.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        // Desempate por nome pra ordem ser estavel: sem isso, dois
                        // generos de peso igual sairiam em ordem imprevisivel e o
                        // grafico "pularia" entre requisicoes.
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> Map.<String, Double>entry(entry.getKey(), entry.getValue()))
                .toList();
    }
}
