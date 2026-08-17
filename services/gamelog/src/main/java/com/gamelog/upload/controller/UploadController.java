package com.gamelog.upload.controller;

import com.gamelog.upload.service.ImageStorageService;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;

// Envio e entrega de imagem de personalizacao: avatar, capa de perfil, capa de
// lista.
//
// Enviar exige login; ver e publico - a imagem aparece num perfil que qualquer um
// abre. Nao ha rota pra apagar: os arquivos antigos ficam no disco quando alguem
// troca de imagem. Decisao consciente, comentada no ImageStorageService.
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageStorageService storage;

    public UploadController(ImageStorageService storage) {
        this.storage = storage;
    }

    // Recebe o arquivo e devolve a URL onde ele passa a existir.
    //
    // A resposta e {"url": "/api/uploads/<uuid>.jpg"} - o mesmo formato de string
    // que a galeria pronta e o campo de endereco colado ja produziam. E o que faz o
    // upload nao exigir mudanca nenhuma no resto do sistema: perfil e lista
    // continuam guardando uma URL, e o ImageUrl.sanitize ja aceita esta, porque
    // comeca com barra.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", storage.store(file));
    }

    // Entrega os bytes.
    @GetMapping("/{nome}")
    public ResponseEntity<byte[]> serve(@PathVariable String nome) {
        ImageStorageService.StoredImage imagem = storage.read(nome);

        return ResponseEntity.ok()
                // O tipo vem dos BYTES do arquivo, nao da extensao do nome.
                .contentType(MediaType.parseMediaType(imagem.kind().getContentType()))
                // nosniff: proibe o navegador de ignorar o Content-Type acima e
                // adivinhar outro pelo conteudo. Sem isso, um arquivo que passasse
                // pela validacao mas contivesse marcacao poderia ser interpretado
                // como HTML - e ai seria script rodando na origem da aplicacao.
                .header("X-Content-Type-Options", "nosniff")
                // O conteudo de uma URL destas nunca muda: o nome e um UUID gerado
                // na gravacao, e trocar de imagem gera outro nome. Entao da pra
                // guardar em cache por muito tempo sem risco de servir dado velho.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(imagem.bytes());
    }
}
