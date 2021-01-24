# Quarkus QraphQL over BigQuery

This is a port port of [bigquery-graphql](https://github.com/simbo1905/bigquery-graphql) onto Quarkus with GraalVM 
Native Image support. See below for how to compile and run the native image using Docker. See
[bigquery-graphql](https://github.com/simbo1905/bigquery-graphql) for how to set up the BigQuery schema, data and 
security. 

This project uses Quarkus, the Supersonic Subatomic Java Framework.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

# GraphQL API

Open up localhost:8080/graphql-ui/ and query with:

```graphql
{
  bookById(id:"book-1"){
    id
    name
    pageCount
    author {
      firstName
      lastName
    }
  }
}
```

Then we get back:

```json
{
  "data": {
    "book1": {
      "id": "book-1",
      "name": "Harry Potter and the Philosopher's Stone",
      "pageCount": 223,
      "author": {
        "firstName": "Joanne",
        "lastName": "Rowling"
      }
    }
  }
}
```

## Creating a native executable

```shell
# Before building the docker image run:
mvn package -Pnative -Dquarkus.native.container-build=true

# Then, build the image with:
docker build -f src/main/docker/Dockerfile.native -t simonmassey/quarkus-graphql .

# Then run the container using:
docker run -it --volume $(pwd):/home/project -e GOOGLE_APPLICATION_CREDENTIALS=/home/project/bigquery-sa.json -p 8080:8080 simonmassey/quarkus-graphql
```
