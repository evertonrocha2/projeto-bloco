package com.gamelog.upload.service;

import com.gamelog.shared.BadRequestException;
import com.gamelog.shared.NotFoundException;
import com.gamelog.upload.domain.ImageKind;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Guarda e devolve as imagens que os usuarios enviam.
//
// E a superficie mais perigosa do projeto: um endpoint que aceita bytes
// arbitrarios de qualquer pessoa logada e depois os serve de volta pra qualquer
// visitante. Tres coisas que chegam no upload e que NAO podem ser tratadas como
// verdade, porque quem envia escolhe as tres:
//
//   nome do arquivo   -> ignorado; o arquivo vira um UUID
//   Content-Type      -> ignorado; o tipo sai dos bytes (ver ImageKind)
//   tamanho anunciado -> ignorado; o tamanho real e medido aqui
//
// O que NAO esta aqui, de proposito: apagar a imagem anterior quando alguem troca
// de avatar. Os arquivos velhos ficam no disco. E desperdicio conhecido e aceito -
// a alternativa exige saber de quem e cada arquivo, e cria um caminho de codigo
// que apaga arquivo, onde um erro custa o avatar de outra pessoa. Num projeto
// deste tamanho, disco e mais barato que esse risco.
@Service
public class ImageStorageService {

    // 5 MB. Avatar e capa nao precisam de mais, e o limite existe pra que um
    // upload nao consuma o disco nem a memoria do processo.
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    // Prefixo da URL publica. O mesmo caminho da rota de leitura.
    private static final String URL_PREFIX = "/api/uploads/";

    // Exatamente o que a gravacao produz: UUID em minusculas + extensao conhecida.
    //
    // Aceitar so o formato que ESTE codigo gera e mais forte do que cacar padroes
    // perigosos: nao existe nome legitimo que nao tenha saido daqui, entao qualquer
    // outra coisa e recusada sem precisar prever a forma do ataque.
    private static final Pattern NOME_VALIDO = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final Path pasta;

    public ImageStorageService(@Value("${app.uploads.dir:./uploads}") String dir) {
        this.pasta = Path.of(dir).toAbsolutePath().normalize();
    }

    // O arquivo lido, com o tipo que ele realmente e.
    public record StoredImage(byte[] bytes, ImageKind kind) {
    }

    // Grava e devolve a URL publica.
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Escolha um arquivo");
        }

        // Medido do conteudo real, nao do tamanho anunciado no cabecalho.
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("A imagem passa de 5 MB");
        }

        byte[] dados = ler(file);

        if (dados.length > MAX_BYTES) {
            throw new BadRequestException("A imagem passa de 5 MB");
        }

        // A decisao sai dos BYTES. O Content-Type declarado nao entra na conta.
        ImageKind kind = ImageKind.detect(dados)
                .orElseThrow(() -> new BadRequestException(
                        "Formato nao aceito. Envie uma imagem JPEG, PNG ou WebP."));

        // UUID, e nao o nome do cliente. Sem isto, "../../../etc/passwd.jpg" como
        // nome faz a gravacao sair da pasta de uploads - e o nome tambem seria a
        // URL publica, ou seja, o proprio atacante escolheria o endereco.
        String nome = UUID.randomUUID() + "." + kind.getExtension();

        try {
            // A validacao inteira acontece ANTES de escrever. Gravar e apagar em
            // caso de recusa deixaria arquivo pra tras se o processo morresse no
            // meio da operacao.
            Files.createDirectories(pasta);
            Files.write(pasta.resolve(nome), dados);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel gravar a imagem", e);
        }

        return URL_PREFIX + nome;
    }

    // Le um arquivo gravado, pelo nome que aparece na URL.
    public StoredImage read(String nome) {
        // Segunda barreira, agora na saida. A gravacao usa UUID, mas a LEITURA
        // recebe o nome pela URL - e sem esta checagem "../application.properties"
        // serviria a configuracao do servidor, com segredos dentro.
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) {
            throw new BadRequestException("Nome de arquivo invalido");
        }

        Path arquivo = pasta.resolve(nome).normalize();

        // Cinto e suspensorio: mesmo com o padrao acima, confirma que o caminho
        // resolvido continua dentro da pasta. Custa uma comparacao e fecha a porta
        // pra qualquer forma de traversal que o padrao deixasse passar.
        if (!arquivo.startsWith(pasta)) {
            throw new BadRequestException("Nome de arquivo invalido");
        }

        if (!Files.isRegularFile(arquivo)) {
            throw new NotFoundException("Imagem nao encontrada");
        }

        byte[] dados;
        try {
            dados = Files.readAllBytes(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler a imagem", e);
        }

        // O tipo e detectado de novo na leitura, em vez de deduzido da extensao. O
        // Content-Type da resposta passa a descrever o que o arquivo E, e nao o que
        // o nome dele diz.
        ImageKind kind = ImageKind.detect(dados)
                .orElseThrow(() -> new NotFoundException("Imagem nao encontrada"));

        return new StoredImage(dados, kind);
    }

    private byte[] ler(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Nao foi possivel ler o arquivo enviado");
        }
    }
}
