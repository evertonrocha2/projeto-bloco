# syntax=docker/dockerfile:1.7
# TP5: o front como imagem - build do Vite servido por nginx.

FROM node:20-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
# Vazio = mesma origem. O navegador chama /api/... no proprio endereco do front, e
# o nginx repassa pro gateway. Assim a MESMA imagem roda em localhost, no cluster
# e atras de qualquer dominio, sem rebuild e sem CORS.
ENV VITE_API_URL=""
RUN npm run build

# Variante sem root do nginx (escuta na 8080 em vez da 80).
FROM nginxinc/nginx-unprivileged:1.27-alpine
# A imagem do nginx aplica envsubst nos templates ao iniciar: o endereco do
# gateway vem do ambiente (API_GATEWAY_URL), e nao fica gravado na imagem.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html
ENV API_GATEWAY_URL=http://api-gateway:8090
EXPOSE 8080
