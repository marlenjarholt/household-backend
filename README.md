# Househod-backend

## Setup
### Docker
Get postgress:
```bash
docker pull postgres
```

Init docker image
```bash
docker run --name testHousehold -d -p 5433:5432 -e POSTGRES_PASSWORD=mysecretpassword postgres
```

### Database
This command will give you all the containers in your docker
```bash
docker container ls
```

Find the CONTAINER ID to the NAME testHouseHold and open database with 
```bash
docker exec -it <CONTAINER ID> bash
```

Log in to the database
```bash
psql -U postgres
```

Create database:
```roomsql
CREATE DATABASE household;
```

