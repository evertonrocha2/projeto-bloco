package com.gamelog.upload.domain;

import java.util.Optional;

// Os formatos de imagem que a aplicacao aceita receber, identificados pelo
// CONTEUDO do arquivo.
//
// Nao pelo Content-Type declarado nem pela extensao do nome: os dois vem de quem
// enviou. "avatar.jpg", Content-Type "image/jpeg", com HTML dentro e um upload
// perfeitamente valido pro protocolo - e um script armazenado pra quem abrir o
// perfil depois.
//
// Os primeiros bytes de um arquivo binario sao uma assinatura que o formato exige
// e que nao da pra escolher. E o unico dado do upload que nao esta nas maos de
// quem enviou.
//
// A lista e de PERMITIDOS. Enumerar formatos perigosos e uma corrida que se
// perde - sobra sempre um. Enumerar os tres que a aplicacao sabe exibir e uma
// decisao fechada, e entrar nela passa a ser deliberado.
//
// Fora da lista, de proposito:
//   SVG  - e imagem de verdade, mas e XML: aceita <script> dentro e o navegador
//          EXECUTA ao renderizar. Num avatar de perfil publico, isso e script
//          rodando na sessao de quem visita.
//   GIF  - legitimo, so nao pedido. Fica fora porque entrar na lista deve ser
//          uma decisao, e nao um esquecimento.
public enum ImageKind {

    JPEG("jpg", "image/jpeg"),

    PNG("png", "image/png"),

    WEBP("webp", "image/webp");

    // Onde o WebP guarda a segunda marca, e quantos bytes sao precisos pra le-la.
    private static final int WEBP_MARK_AT = 8;
    private static final int WEBP_MIN = 12;

    private final String extension;
    private final String contentType;

    ImageKind(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    // Que formato e este arquivo. Optional vazio significa "nenhum dos aceitos",
    // e quem chama traduz isso em recusa.
    public static Optional<ImageKind> detect(byte[] dados) {
        if (dados == null) {
            return Optional.empty();
        }

        // Cada assinatura tem o SEU proprio tamanho minimo, conferido dentro do
        // comeca(). Uma guarda global de 12 bytes - o que o WebP precisa - faria um
        // JPEG, que se identifica em 3, ser recusado por um motivo que nao e o dele.
        // Nao muda o resultado pra arquivo real, mas embaralha os dois casos de
        // "curto demais", e um deles e a defesa contra estouro de indice.

        // FF D8 FF - todo JPEG comeca assim, seja JFIF ou Exif.
        if (comeca(dados, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }

        // 89 "PNG" CR LF SUB LF. Os oito bytes juntos sao desenhados pra detectar
        // corrupcao de transferencia, entao conferimos os oito.
        if (comeca(dados, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }

        // "RIFF" no inicio e "WEBP" no byte 8. Os quatro do meio sao o tamanho do
        // arquivo.
        //
        // As DUAS marcas importam: WAV e AVI tambem sao conteineres RIFF e comecam
        // igual. Conferir so o inicio aceitaria um audio como imagem, que seria
        // gravado, servido com Content-Type de imagem, e apontado por um <img> que
        // nunca renderiza.
        if (dados.length >= WEBP_MIN
                && comeca(dados, 'R', 'I', 'F', 'F')
                && dados[WEBP_MARK_AT] == 'W'
                && dados[WEBP_MARK_AT + 1] == 'E'
                && dados[WEBP_MARK_AT + 2] == 'B'
                && dados[WEBP_MARK_AT + 3] == 'P') {
            return Optional.of(WEBP);
        }

        return Optional.empty();
    }

    // Compara os primeiros bytes com a assinatura. Arquivo mais curto que a
    // assinatura nao casa - e a guarda que impede estouro de indice.
    private static boolean comeca(byte[] dados, int... assinatura) {
        if (dados.length < assinatura.length) {
            return false;
        }

        for (int i = 0; i < assinatura.length; i++) {
            if (dados[i] != (byte) assinatura[i]) {
                return false;
            }
        }
        return true;
    }

    // A extensao gravada em disco vem daqui, do tipo DETECTADO - nunca do nome que
    // o cliente mandou.
    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }
}
