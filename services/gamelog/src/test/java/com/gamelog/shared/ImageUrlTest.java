package com.gamelog.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

// Toda imagem que o usuario escolhe - avatar, capa do perfil, capa de lista -
// entra no sistema como URL, seja colada por ele ou vinda da galeria pronta.
//
// O valor vai parar num <img src>. Sem checagem, "javascript:alert(1)" e um src
// perfeitamente valido pro navegador, e a pessoa que escolhe a imagem nao e
// necessariamente a mesma que vai olhar o perfil - basta a lista ser publica.
class ImageUrlTest {

    @Test
    void aceitaUrlHttps() {
        assertThat(ImageUrl.sanitize("https://cdn.exemplo.com/capa.jpg"))
                .isEqualTo("https://cdn.exemplo.com/capa.jpg");
    }

    @Test
    void aceitaUrlHttp() {
        // Nao e ideal, mas capa de jogo vem de CDN antiga que as vezes so serve
        // http, e recusar transformaria um problema de transporte num impedimento
        // de produto.
        assertThat(ImageUrl.sanitize("http://cdn.exemplo.com/capa.jpg"))
                .isEqualTo("http://cdn.exemplo.com/capa.jpg");
    }

    @Test
    void aceitaCaminhoRelativo() {
        // E como a galeria pronta e referenciada: as artes moram em
        // frontend/public e chegam como /hero.jpg.
        assertThat(ImageUrl.sanitize("/backgrounds/hero.jpg")).isEqualTo("/backgrounds/hero.jpg");
    }

    @Test
    void tiraEspacoDasPontas() {
        assertThat(ImageUrl.sanitize("  https://exemplo.com/a.jpg  "))
                .isEqualTo("https://exemplo.com/a.jpg");
    }

    @Test
    void tratsVazioComoAusencia() {
        // Limpar o campo e uma acao legitima: e assim que se remove um avatar.
        assertThat(ImageUrl.sanitize(null)).isNull();
        assertThat(ImageUrl.sanitize("")).isNull();
        assertThat(ImageUrl.sanitize("   ")).isNull();
    }

    @Test
    void recusaJavascript() {
        assertThatThrownBy(() -> ImageUrl.sanitize("javascript:alert(1)"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recusaJavascriptDisfarcadoDeMaiuscula() {
        // A comparacao tem que ser insensivel a caixa, senao "JavaScript:" passa.
        assertThatThrownBy(() -> ImageUrl.sanitize("JaVaScRiPt:alert(1)"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recusaDataUri() {
        // data: carrega o conteudo inteiro na propria URL - inclusive um SVG com
        // script dentro, que o navegador executa ao renderizar.
        assertThatThrownBy(() -> ImageUrl.sanitize("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recusaCaminhoQueNaoComecaComBarra() {
        // "exemplo.com/a.jpg" sem esquema seria resolvido como caminho relativo a
        // pagina atual e daria 404 - erro silencioso e confuso de diagnosticar.
        assertThatThrownBy(() -> ImageUrl.sanitize("exemplo.com/capa.jpg"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recusaUrlLongaDemaisPraColuna() {
        // A coluna tem 500. Sem esta checagem o banco truncaria - ou explodiria -
        // com uma mensagem que nao ajuda ninguem.
        String gigante = "https://exemplo.com/" + "a".repeat(600) + ".jpg";

        assertThatThrownBy(() -> ImageUrl.sanitize(gigante))
                .isInstanceOf(BadRequestException.class);
    }
}
