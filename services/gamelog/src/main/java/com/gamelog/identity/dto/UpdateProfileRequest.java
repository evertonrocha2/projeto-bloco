package com.gamelog.identity.dto;

import jakarta.validation.constraints.Size;

// O que a tela de editar perfil manda.
//
// Os tres campos sao anulaveis porque apagar qualquer um deles e uma acao
// legitima: e assim que se remove um avatar ou se esvazia a bio. As URLs passam
// pelo ImageUrl no service, que recusa esquema perigoso antes de gravar.
public record UpdateProfileRequest(

        @Size(max = 500, message = "a bio passou de 500 caracteres")
        String bio,

        String avatarUrl,

        String bannerUrl
) {
}
