package com.github.simbo1905.bigquerygraphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@ApplicationScoped
public class GraphQLConfigurator {
    @Inject
    Vertx vertx;

    @Inject
    BigQueryRunner bigQueryRunner;

    public void init(@Observes Router router) {
        GraphQLHandler graphQLHandler = GraphQLHandler.create(setupGraphQL());
        router.route("/graphql").handler(graphQLHandler);
    }

    @ConfigProperty(name = "wirings.json", defaultValue = "wirings.json")
    String wiringsFile;

    @ConfigProperty(name = "schema.graphqls", defaultValue = "schema.graphqls")
    String schemaFile;

    @SneakyThrows
    private GraphQL setupGraphQL(){
        // first load the metadata and generate the generic DataFetchers.
        String wirings = vertx.fileSystem().readFileBlocking(wiringsFile).toString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<FieldMetaData> mappings = objectMapper.readValue(wirings, new TypeReference<>() {});
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring();
        wiring = wire(wiring, mappings);

        // if you wanted to customise things you would add more wiring here

        // then load the schema
        String schema = vertx.fileSystem().readFileBlocking(schemaFile).toString();
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
        SchemaGenerator schemaGenerator = new SchemaGenerator();

        // return the GraphQL server.
        RuntimeWiring runtimeWiring = wiring.build();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        return new GraphQL.Builder(graphQLSchema).build();
    }

    @SneakyThrows
    protected RuntimeWiring.Builder wire(RuntimeWiring.Builder wiring, List<FieldMetaData> mappings) {
        for (FieldMetaData fieldMetaData : mappings) {
            log.info("wiring: {}", fieldMetaData.toString());
            wiring = wiring
                    .type(fieldMetaData.typeName, builder -> {
                        // a blocking data fetcher
                        DataFetcher blockingDataFetcher = bigQueryRunner.queryForOne(
                                fieldMetaData.sql,
                                fieldMetaData.mapperCsv,
                                fieldMetaData.gqlAttr,
                                fieldMetaData.sqlParam);
                        // wrap in an asyc data fetcher so as to not block Vert.x
                        return builder.dataFetcher(fieldMetaData.fieldName,
                                (de) -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return blockingDataFetcher.get(de);
                            } catch (Exception e) {
                                log.error("Exception {} with query: {}", e.getMessage(), mappings.toString());
                                return null;
                            }
                        }));
                    });
        }
        return wiring;
    }
}
