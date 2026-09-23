# syntax=docker/dockerfile:1.7
#
# TP5: UMA receita de imagem pros seis servicos Java.
#
#   docker build --build-arg MODULE=services/gamelog -t gamelog .
#
# Os seis sao Spring Boot sobre a mesma versao de Java e o mesmo POM pai; seis
# Dockerfiles quase iguais divergiriam com o tempo (um atualiza a JRE, outro nao).
# O que muda entre eles e so o modulo Maven, e isso e um argumento de build.

############################## build ##############################
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src

# O contexto inteiro entra porque "-am" (also-make) precisa dos modulos de que o
# servico depende (ex.: platform/observability). O .dockerignore corta target/,
# node_modules e dados locais.
COPY . .

# O cache de ~/.m2 sobrevive entre builds: so a primeira baixa a internet toda.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl "${MODULE}" -am package -DskipTests

# Jar em camadas: dependencias (mudam raramente) separadas do codigo (muda a cada
# commit). No push, so a camada "application" - alguns KB - sobe de novo.
RUN mkdir -p /layers /extra \
 && cp "${MODULE}"/target/*-SNAPSHOT.jar /app.jar \
 && cd /layers && java -Djarmode=layertools -jar /app.jar extract \
 && if [ "${MODULE}" = "platform/config-server" ]; then cp -r /src/config-repo /extra/config-repo; fi

############################## runtime ##############################
# So a JRE: sem Maven, sem JDK, sem codigo-fonte. Imagem menor e menos superficie.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Nunca como root: se alguem explorar uma falha na aplicacao, cai num usuario
# sem permissao pra nada alem da propria pasta.
RUN addgroup -S gamelog && adduser -S gamelog -G gamelog \
 && mkdir -p /app/data /app/uploads && chown -R gamelog:gamelog /app

COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
# Vazio em todo servico menos no config-server, que serve os arquivos da pasta.
COPY --from=build /extra/ ./

USER gamelog

# MaxRAMPercentage: a JVM dimensiona o heap pelo limite do CONTAINER (e nao pela
# RAM da maquina). ExitOnOutOfMemoryError: sem memoria, morrer e deixar o
# orquestrador reiniciar e melhor que seguir meio vivo.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
