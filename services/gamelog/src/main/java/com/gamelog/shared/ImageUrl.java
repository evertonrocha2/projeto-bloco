package com.gamelog.shared;

import java.util.Locale;

// Onde toda imagem escolhida pelo usuario e conferida antes de virar linha no
// banco: avatar, capa do perfil, capa de lista.
//
// A decisao de produto foi galeria pronta + campo de URL, sem upload. Isso evita
// multipart, magic bytes e armazenamento - mas nao evita o problema principal,
// que e o valor ir parar num <img src>. "javascript:alert(1)" e um src
// perfeitamente valido pro navegador, e quem escolhe a imagem nao e
// necessariamente quem vai olhar a tela: basta a lista ser publica.
//
// A regra e uma lista de PERMITIDOS, e nao de proibidos. Enumerar esquemas
// perigosos e uma corrida que se perde - sobra sempre um vbscript:, um filesystem:
// ou uma variacao de codificacao que ninguem lembrou.
public final class ImageUrl {

    // Mesmo tamanho das colunas que guardam esses valores.
    private static final int MAX_LENGTH = 500;

    private ImageUrl() {
    }

    // Devolve a URL limpa, ou null quando nao ha imagem.
    //
    // Vazio nao e erro: limpar o campo e como se remove um avatar.
    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String url = raw.trim();

        if (url.length() > MAX_LENGTH) {
            throw new BadRequestException("O endereco da imagem e longo demais");
        }

        // toLowerCase com Locale.ROOT: em turco, "I".toLowerCase() vira "ı" e a
        // comparacao com "http" falharia dependendo da maquina que roda o servidor.
        String comparavel = url.toLowerCase(Locale.ROOT);

        // Caminho na NOSSA origem: e como a galeria pronta chega, ja que as artes
        // moram em frontend/public e sao servidas da raiz.
        //
        // O "!startsWith(//)" nao e detalhe. Duas barras nao sao um caminho: o
        // navegador le "//evil.com/x.jpg" como "mesmo esquema da pagina, OUTRO
        // host". Sem essa exclusao, a regra escrita pra aceitar arte da propria
        // aplicacao aceitaria qualquer servidor do mundo - e escondida atras de um
        // valor que parece um caminho local em toda inspecao superficial.
        boolean caminhoLocal = url.startsWith("/") && !url.startsWith("//");

        boolean permitida = comparavel.startsWith("https://")
                || comparavel.startsWith("http://")
                || caminhoLocal;

        if (!permitida) {
            throw new BadRequestException(
                    "A imagem precisa ser um endereco http, https, ou um caminho comecando com /");
        }

        return url;
    }
}
