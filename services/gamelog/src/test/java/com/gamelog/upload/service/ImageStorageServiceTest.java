package com.gamelog.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.NotFoundException;
import com.gamelog.upload.domain.ImageKind;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

// Guardar e devolver a imagem que alguem enviou.
//
// Aceitar arquivo de terceiro e a superficie mais perigosa que este projeto tem, e
// os testes aqui sao quase todos sobre o que deve ser RECUSADO. Tres coisas que o
// cliente controla e que nao podem ser usadas como verdade: o nome do arquivo, o
// Content-Type declarado e o tamanho anunciado.
class ImageStorageServiceTest {

    @TempDir
    Path pasta;

    private ImageStorageService service;

    // JPEG minimo valido: assinatura + enchimento pra passar do tamanho minimo.
    private byte[] jpegValido() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        saida.write(0xFF);
        saida.write(0xD8);
        saida.write(0xFF);
        saida.write(0xE0);
        for (int i = 0; i < 64; i++) {
            saida.write(0x00);
        }
        return saida.toByteArray();
    }

    private byte[] pngValido() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        for (int b : new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}) {
            saida.write(b);
        }
        for (int i = 0; i < 64; i++) {
            saida.write(0x00);
        }
        return saida.toByteArray();
    }

    @BeforeEach
    void criar() {
        service = new ImageStorageService(pasta.toString());
    }

    // ---------- gravacao ----------

    @Test
    void guardaUmJpegEDevolveAUrl() {
        String url = service.store(new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", jpegValido()));

        assertThat(url).startsWith("/api/uploads/").endsWith(".jpg");
    }

    @Test
    void oNomeGravadoNaoTemRelacaoComONomeEnviado() {
        // O nome do cliente NUNCA e usado. E o vetor classico: "../../../etc/passwd"
        // como nome de arquivo faz a gravacao sair da pasta de uploads.
        String url = service.store(new MockMultipartFile(
                "file", "../../../etc/passwd.jpg", "image/jpeg", jpegValido()));

        String nome = url.substring(url.lastIndexOf('/') + 1);

        assertThat(nome).doesNotContain("..").doesNotContain("passwd");
        // UUID (36 caracteres) + ponto + extensao.
        assertThat(nome).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");
    }

    @Test
    void gravaOArquivoDentroDaPastaConfigurada() throws IOException {
        String url = service.store(new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", jpegValido()));
        String nome = url.substring(url.lastIndexOf('/') + 1);

        assertThat(Files.exists(pasta.resolve(nome))).isTrue();
        try (var arquivos = Files.list(pasta)) {
            assertThat(arquivos).hasSize(1);
        }
    }

    @Test
    void aExtensaoVemDoConteudoENaoDoNomeEnviado() {
        // PNG enviado como "foto.jpg" e gravado como .png: quem decide e a
        // assinatura. Guardar como .jpg faria o arquivo ser servido com o
        // Content-Type errado depois.
        String url = service.store(new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", pngValido()));

        assertThat(url).endsWith(".png");
    }

    @Test
    void doisUploadsIguaisNaoSeSobrescrevem() {
        String primeira = service.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", jpegValido()));
        String segunda = service.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", jpegValido()));

        assertThat(primeira).isNotEqualTo(segunda);
    }

    // ---------- recusas ----------

    @Test
    void recusaTextoRenomeadoParaJpg() {
        MockMultipartFile falso = new MockMultipartFile(
                "file", "foto.jpg", "image/jpeg", "nao sou imagem".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.store(falso))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recusaSvgMesmoComContentTypeDeImagem() {
        MockMultipartFile svg = new MockMultipartFile(
                "file", "avatar.svg", "image/svg+xml",
                "<svg onload='alert(1)'></svg>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.store(svg))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void naoGravaNadaQuandoRecusa() throws IOException {
        try {
            service.store(new MockMultipartFile(
                    "file", "x.jpg", "image/jpeg", "lixo".getBytes(StandardCharsets.UTF_8)));
        } catch (BadRequestException esperado) {
            // esperado
        }

        // A validacao acontece ANTES de escrever. Gravar e apagar depois deixaria
        // arquivo pra tras se o processo morresse no meio.
        try (var arquivos = Files.list(pasta)) {
            assertThat(arquivos).isEmpty();
        }
    }

    @Test
    void recusaArquivoVazio() {
        assertThatThrownBy(() -> service.store(
                new MockMultipartFile("file", "vazio.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void recusaArquivoAcimaDoLimite() {
        // O limite tambem e checado aqui, e nao so na configuracao do servlet: a
        // configuracao protege o processo, esta checagem protege a regra - e e ela
        // que devolve uma mensagem que a tela sabe mostrar.
        byte[] gigante = new byte[ImageStorageService.MAX_BYTES + 1];
        System.arraycopy(jpegValido(), 0, gigante, 0, jpegValido().length);

        assertThatThrownBy(() -> service.store(
                new MockMultipartFile("file", "grande.jpg", "image/jpeg", gigante)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("5");
    }

    // ---------- leitura ----------

    @Test
    void leOArquivoGravado() {
        byte[] original = jpegValido();
        String url = service.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", original));
        String nome = url.substring(url.lastIndexOf('/') + 1);

        var lida = service.read(nome);

        assertThat(lida.kind()).isEqualTo(ImageKind.JPEG);
        assertThat(lida.bytes()).isEqualTo(original);
    }

    @Test
    void aLeituraRecusaNomeComTraversal() {
        // Segunda barreira, na saida. Mesmo com a gravacao usando UUID, a rota de
        // leitura recebe o nome pela URL - e "..%2F..%2Fapplication.properties"
        // serviria qualquer arquivo do servidor.
        for (String malicioso : new String[]{
                "../application.properties",
                "..\\application.properties",
                "/etc/passwd",
                "foo/bar.jpg",
        }) {
            assertThatThrownBy(() -> service.read(malicioso))
                    .as("deveria recusar %s", malicioso)
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Test
    void aLeituraRecusaNomeQueNaoSejaUuid() {
        // Aceitar so o formato que a gravacao produz e mais forte do que caçar
        // padroes perigosos: nao existe nome valido que nao tenha saido daqui.
        assertThatThrownBy(() -> service.read("avatar.jpg"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void arquivoInexistenteDa404ENao500() {
        assertThatThrownBy(() -> service.read("00000000-0000-0000-0000-000000000000.jpg"))
                .isInstanceOf(NotFoundException.class);
    }
}
