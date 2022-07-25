#!/bin/sh

set -ex

[ "${CI_ENV?:is not set}" ]
[ "${TAG?:is not set}" ]

NETWORK=elvis-${CI_ENV}

DB_IMAGE=elvis-database:${TAG}
BACKEND_IMAGE=${CI_REGISTRY_IMAGE?:is not set}:${TAG}
KEYCLOAK_IMAGE=elvis-keycloak:${TAG}

DB_CONTAINER=elvis-db-${CI_ENV}
BACKEND_CONTAINER=elvis-backend-${CI_ENV}
MINIO_CONTAINER=elvis-minio-${CI_ENV}
KEYCLOAK_CONTAINER=elvis-keycloak-${CI_ENV}

docker rm -fv "$BACKEND_CONTAINER" || true
docker rm -fv "$KEYCLOAK_CONTAINER" || true

# shellcheck disable=SC2039
if [ "${CI_ENV}" == "test" ]; then
  docker rm -fv "$DB_CONTAINER" || true
  docker rm -fv "$MINIO_CONTAINER" || true

  docker run \
    -d \
    --name="$DB_CONTAINER" \
    --network="$NETWORK" \
    -e POSTGRES_USER=elvis \
    -e POSTGRES_PASSWORD=elvis \
    -e POSTGRES_DB=elvis \
    -e POSTGRES_DB=elvis \
    -e KEYCLOAK_DB_NAME=keycloak \
    -e KEYCLOAK_DB_USER=keycloak \
    -e KEYCLOAK_DB_PASSWORD=keycloak \
    "$DB_IMAGE"

  docker run \
    -d \
    --name="$MINIO_CONTAINER" \
    --network="$NETWORK" \
    -e MINIO_ACCESS_KEY=minio \
    -e MINIO_SECRET_KEY=minio123456789 \
    minio/minio \
    server --compat \
    /data

  docker run \
    -d \
    --name="$KEYCLOAK_CONTAINER" \
    --network="$NETWORK" \
    -e KEYCLOAK_USER=elvis \
    -e KEYCLOAK_PASSWORD=K4Ngy6E4 \
    -e KEYCLOAK_IMPORT=/tmp/exports/elvis-realm.json \
    -e DB_VENDOR=postgres \
    -e DB_ADDR="$DB_CONTAINER" \
    -e KEYCLOAK_FRONTEND_URL="https://elvis-test.pictura-hosting.nl/auth/" \
    -e DB_DATABASE=keycloak \
    -e DB_USER=keycloak \
    -e DB_PASSWORD=keycloak\
    -p 127.0.0.1:5050:8080/tcp \
    "$KEYCLOAK_IMAGE"

  docker run \
    -d \
    --name="$BACKEND_CONTAINER" \
    --network="$NETWORK" \
    -e DATABASE_HOST="$DB_CONTAINER" \
    -e DATABASE_NAME=elvis \
    -e DATABASE_USERNAME=elvis \
    -e DATABASE_PASSWORD=elvis \
    -e MAILER=console \
    -e MAILER_SMTP_PORT=25 \
    -e MAILER_SMTP_HOSTNAME=listserv.pictura-hosting.nl \
    -e FRONTEND_URL=https://elvis-test.pictura-hosting.nl \
    -e S3_HOST=http://"$MINIO_CONTAINER":9000 \
    -e KEYCLOAK_SERVER_URL=https://elvis-test.pictura-hosting.nl/auth \
    -e KEYCLOAK_USER=elvis \
    -e KEYCLOAK_PASSWORD=K4Ngy6E4 \
    -e APP_ROLE="IMPORT_DEV_USERS MIGRATE_USERS MIGRATE_RESOURCES MIGRATE_CONTENT USER_GROUP_ADJUSTMENT" \
    "$BACKEND_IMAGE"
fi

if [ "${CI_ENV}" == "accept" ]; then
  BACKUP_DIR=/var/backups
  POSTGRES_BACKUP_DIR="$BACKUP_DIR"/postgres
  MINIO_BACKUP_DIR="$BACKUP_DIR"/minio

  mkdir -p "$BACKUP_DIR" "$POSTGRES_BACKUP_DIR" "$MINIO_BACKUP_DIR"

  # PostgreSQL backup
  POSTGRES_DUMP_FILE_NAME=$(date +'%Y_%m_%d_%H_%M')_${CI_ENV}.sql.gz

  docker exec -t "$DB_CONTAINER" pg_dump -d elvis -c -U elvis | gzip >"$POSTGRES_DUMP_FILE_NAME"
  mv ./"$POSTGRES_DUMP_FILE_NAME" "$POSTGRES_BACKUP_DIR"/"$MINIO_DUMP_FILE_NAME"

  # MinIO backup
  MINIO_DUMP_FILE_NAME=$(date +'%Y_%m_%d_%H_%M')_${CI_ENV}_.tar.gz

  docker exec -t "$MINIO_CONTAINER" tar -zcvf ./minio_dump.tar.gz /data
  docker cp "$MINIO_CONTAINER":/minio_dump.tar.gz "$MINIO_BACKUP_DIR"/"$MINIO_DUMP_FILE_NAME"

  docker run \
    -d \
    --name="$BACKEND_CONTAINER" \
    --network="$NETWORK" \
    -e DATABASE_HOST="$DB_CONTAINER" \
    -e DATABASE_NAME=elvis \
    -e DATABASE_USERNAME=elvis \
    -e DATABASE_PASSWORD=elvis \
    -e MAILER=vertx \
    -e FRONTEND_URL=https://elvis-accept.pictura-hosting.nl \
    -e MAILER_SMTP_PORT=25 \
    -e MAILER_SMTP_HOSTNAME=listserv.pictura-hosting.nl \
    -e S3_HOST=http://"$MINIO_CONTAINER":9000 \
    -e KEYCLOAK_SERVER_URL=https://elvis-accept.pictura-hosting.nl/auth \
    -e KEYCLOAK_USER=elvis \
    -e KEYCLOAK_PASSWORD=t9CT4ye6 \
    "$BACKEND_IMAGE"

  docker run \
    -d \
    --name="$KEYCLOAK_CONTAINER" \
    --network="$NETWORK" \
    -e KEYCLOAK_USER=elvis \
    -e KEYCLOAK_PASSWORD=t9CT4ye6 \
    -e KEYCLOAK_IMPORT=/tmp/exports/elvis-realm.json \
    -e DB_VENDOR=postgres \
    -e DB_ADDR="$DB_CONTAINER" \
    -e KEYCLOAK_FRONTEND_URL="https://elvis-accept.pictura-hosting.nl/auth/" \
    -e DB_DATABASE=keycloak \
    -e DB_USER=keycloak \
    -e DB_PASSWORD=keycloak\
    -p 127.0.0.1:5051:8080/tcp \
    "$KEYCLOAK_IMAGE"

fi

docker network connect ingress "$BACKEND_CONTAINER"
docker network connect ingress elvis-keycloak-test || true
docker network connect ingress elvis-keycloak-accept || true

docker update --restart=always "$BACKEND_CONTAINER" || true
docker update --restart=always "$DB_CONTAINER" || true
docker update --restart=always "$MINIO_CONTAINER" || true
docker update --restart=always "$KEYCLOAK_CONTAINER" || true

# Restart
docker rm -f nginx-ingress || true
docker run \
  -d \
  --name=nginx-ingress \
  -v /etc/letsencrypt/:/etc/letsencrypt/ \
  -v /var/www/certbot_challenge:/var/www/certbot_challenge \
  -p 80:80 \
  -p 443:443 \
  --network=ingress nginx-ingress
