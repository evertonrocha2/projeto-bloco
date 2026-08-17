package com.gamelog.upload.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// Que tipo de imagem e este arquivo, olhando o CONTEUDO dele.
//
// Nao o Content-Type declarado, nem a extensao do nome: os dois vem de quem
// enviou, e quem envia pode mentir. "avatar.jpg" com Content-Type "image/jpeg" e
// um HTML dentro e um upload valido do ponto de vista do protocolo - e um XSS
// armazenado do ponto de vista de quem abrir o perfil.
//
// A regra e lista de PERMITIDOS. Enumerar formatos perigosos e uma corrida que se
// perde; enumerar os tres que a aplicacao sabe exibir e uma decisao fechada.
class ImageKindTest {

    private Optional<ImageKind> detectar(int... bytes) {
        byte[] dados = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            dados[i] = (byte) bytes[i];
        }
        return ImageKind.detect(dados);
    }

    @Test
    void reconheceJpeg() {
        assertThat(detectar(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10)).contains(ImageKind.JPEG);
    }

    @Test
    void reconhecePng() {
        assertThat(detectar(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
                .contains(ImageKind.PNG);
    }

    @Test
    void reconheceWebp() {
        // "RIFF" nos bytes 0-3 e "WEBP" nos bytes 8-11. Os quatro do meio sao o
        // tamanho do arquivo e podem ser qualquer coisa.
        assertThat(detectar(
                'R', 'I', 'F', 'F',
                0x1A, 0x2B, 0x00, 0x00,
                'W', 'E', 'B', 'P'))
                .contains(ImageKind.WEBP);
    }

    @Test
    void naoConfundeWavComWebp() {
        // WAV e AVI TAMBEM comecam com "RIFF". Conferir so o inicio aceitaria um
        // audio como imagem - e ele seria servido com Content-Type de imagem,
        // apontado por um <img> que nunca renderiza.
        assertThat(detectar(
                'R', 'I', 'F', 'F',
                0x1A, 0x2B, 0x00, 0x00,
                'W', 'A', 'V', 'E'))
                .isEmpty();
    }

    @Test
    void recusaTextoDisfarcadoDeImagem() {
        // O caso mais comum: renomear qualquer coisa pra .jpg.
        assertThat(ImageKind.detect("nao sou uma imagem".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
    }

    @Test
    void recusaSvg() {
        // SVG e imagem de verdade, e por isso mesmo esta fora: e XML, aceita
        // <script> dentro, e o navegador EXECUTA ao renderizar. Um avatar SVG num
        // perfil publico seria script rodando na sessao de quem visita.
        assertThat(ImageKind.detect("<svg xmlns='http://www.w3.org/2000/svg'>".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
    }

    @Test
    void recusaHtml() {
        assertThat(ImageKind.detect("<!doctype html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
    }

    @Test
    void recusaGifQueEImagemMasNaoEstaNaLista() {
        // GIF e imagem legitima. Fica fora porque a lista e de permitidos, e nao de
        // proibidos: entrar na lista e uma decisao, nao um esquecimento.
        assertThat(detectar('G', 'I', 'F', '8', '9', 'a')).isEmpty();
    }

    @Test
    void recusaArquivoCurtoDemaisPraTerAssinatura() {
        // Sem esta guarda a leitura dos bytes 8-11 do WebP estouraria o array.
        assertThat(detectar(0xFF, 0xD8)).isEmpty();
        assertThat(ImageKind.detect(new byte[0])).isEmpty();
        assertThat(ImageKind.detect(null)).isEmpty();
    }

    @Test
    void cadaTipoTemExtensaoEContentTypeProprios() {
        // A extensao gravada em disco vem DAQUI, do tipo detectado - nunca do nome
        // que o cliente mandou.
        assertThat(ImageKind.JPEG.getExtension()).isEqualTo("jpg");
        assertThat(ImageKind.JPEG.getContentType()).isEqualTo("image/jpeg");
        assertThat(ImageKind.PNG.getExtension()).isEqualTo("png");
        assertThat(ImageKind.WEBP.getContentType()).isEqualTo("image/webp");
    }
}
