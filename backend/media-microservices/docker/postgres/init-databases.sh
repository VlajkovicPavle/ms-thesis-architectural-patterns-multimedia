#!/bin/sh
set -eu

create_database_and_owner() {
  database_name="$1"
  database_user="$2"
  database_password="$3"

  psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    --set=database_name="$database_name" \
    --set=database_user="$database_user" \
    --set=database_password="$database_password" <<-'SQL'
	SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'database_user', :'database_password') \gexec
	SELECT format('CREATE DATABASE %I OWNER %I', :'database_name', :'database_user') \gexec
	SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'database_name') \gexec
	SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database_name', :'database_user') \gexec
SQL
}

create_database_and_owner "$MEDIA_DB" "$MEDIA_DB_USER" "$MEDIA_DB_PASSWORD"
create_database_and_owner \
  "$NOTIFICATION_DB" "$NOTIFICATION_DB_USER" "$NOTIFICATION_DB_PASSWORD"
