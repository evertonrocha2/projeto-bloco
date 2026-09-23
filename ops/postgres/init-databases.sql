-- Roda so na PRIMEIRA subida do volume (docker-entrypoint-initdb.d).
-- O banco "gamelog" ja e criado por POSTGRES_DB; aqui entram os dos outros dois
-- servicos. Um servidor, tres bancos: o isolamento que importa (nenhum servico le
-- tabela de outro) se mantem, e o ambiente local nao precisa de tres Postgres.
CREATE DATABASE recommendations;
CREATE DATABASE notifications;
