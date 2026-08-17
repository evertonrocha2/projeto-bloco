package com.gamelog.collection.domain;

// Em que pe um jogo esta na colecao de alguem.
//
// Antes isto era uma String livre, validada so com @NotBlank: o back-end aceitava
// {"status": "banana"} sem reclamar, e as quatro opcoes viviam duplicadas em dois
// arquivos do front-end. Como enum, um valor invalido e recusado com 400 antes de
// chegar ao service, e existe UMA lista de valores possiveis no sistema inteiro.
//
// PLATINADO e o valor novo. Ele nao e "zerado com enfeite": platinar significa ter
// feito tudo o que o jogo oferece, e quem coleciona trata as duas coisas como
// conquistas diferentes - por isso lista separada, e nao um sinalizador em cima de
// ZERADO.
public enum CollectionStatus {

    // A wishlist. E o status de quem ainda nao comecou.
    QUERO_JOGAR("Quero jogar"),

    JOGANDO("Jogando"),

    // Chegou ao fim da historia principal.
    ZERADO("Zerado"),

    // Completou tudo. Nos consoles da Sony vira o trofeu de platina, e o nome
    // pegou entre jogadores mesmo fora do PlayStation.
    PLATINADO("Platinado"),

    // Parou no meio e nao pretende voltar.
    LARGADO("Largado");

    private final String label;

    CollectionStatus(String label) {
        this.label = label;
    }

    // Como o status aparece na tela. Guardamos o NOME do enum no banco
    // (QUERO_JOGAR) e exibimos o rotulo - assim o texto pode ser reescrito, ou
    // traduzido, sem migrar dado nenhum.
    public String getLabel() {
        return label;
    }
}
