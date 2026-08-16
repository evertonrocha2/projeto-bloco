package com.gamelog.shared.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Chamado pelo Envers toda vez que uma revisao nova vai ser gravada.
// A gente aproveita pra carimbar o username de quem esta logado (vem do token
// JWT, via SecurityContext). Mudancas feitas fora de uma requisicao autenticada
// (ex: o DataSeeder no startup) ficam registradas como "sistema".
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevision revision = (AuditRevision) revisionEntity;
        revision.setUsername(currentUsername());
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "sistema";
        }
        return auth.getName();
    }
}
