-- One PostgreSQL server for local development, one database per service: no service can read another's tables.
CREATE DATABASE identity;
CREATE DATABASE career;
CREATE DATABASE job;
