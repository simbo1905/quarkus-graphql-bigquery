# Quarkus GraphQL Java Native and BigQuery

This is a port port of [bigquery-graphql](https://github.com/simbo1905/bigquery-graphql) onto Quarkus with GraalVM 
Native Image support. See below for how to compile and run the native image using Docker. 

This project uses Quarkus, the Supersonic Subatomic Java Framework.

## BigQuery Setup

On the Google Cloud console:

1. Create a dataset named `demo_graphql_java` see [here](https://cloud.google.com/bigquery/docs/datasets).
2. Run `create_tables.sql` using the BigQuery console.

First create the tables in a dataset `demo_graphql_java` as described above.

Create a service account `bigquery-graphql` then grant it the bigquery user role:

```sh
gcloud projects add-iam-policy-binding ${YOUR_PROJECT} \
  --member="serviceAccount:bigquery-graphql@${YOUR_PROJECT}.iam.gserviceaccount.com" \
  --role="roles/bigquery.user"
```

Grant the service account read to the two tables using these instructions [bigquery/docs/table-access-controls](https://cloud.google.com/bigquery/docs/table-access-controls#bq).

In *my* case I did something like this. YMMV you need to change the identifiers to match your project/sa:

```sh
$ cat policy.json
{
"bindings": [
 {
   "members": [
     "serviceAccount:bigquery-graphql@capable-conduit-300818.iam.gserviceaccount.com"
   ],
   "role": "roles/bigquery.dataViewer"
 }
]
}
$ bq set-iam-policy capable-conduit-300818:demo_graphql_java.book policy.json
$ bq set-iam-policy capable-conduit-300818:demo_graphql_java.author policy.json
```

On the console create a JSON keyfile for the service account and save it in the current directory.
Save the file name as "bigquery-sa.json".


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/bigquery-sa.json 
mvn quarkus:dev
```

# GraphQL API

Open up http://localhost:8080/graphql-ui/ and query with:

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
