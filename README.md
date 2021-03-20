# Househod-backend

## Docker
Get postgress:
```bash
docker pull postgres
```

Init docker image
```bash
docker run --name testHousehold -d -p 5433:5432 -e POSTGRES_PASSWORD=mysecretpassword postgres
```