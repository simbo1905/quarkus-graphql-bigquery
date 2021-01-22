package com.github.simbo1905.bigquerygraphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import graphql.GraphQL;
import graphql.schema.*;
import graphql.schema.idl.*;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @ConfigProperty(name = "wirings.file", defaultValue = "wirings.json")
    String wiringsFile;

    @ConfigProperty(name = "schema.graphqls", defaultValue = "schema.graphqls")
    String schemaFile;

    @SneakyThrows
    private GraphQL setupGraphQL(){
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring();

        // wire in caching
        wiring = wiring.directiveWiring(new FieldQueryCache());

        // load the metadata and generate the generic DataFetchers.
        String wirings = vertx.fileSystem().readFileBlocking(wiringsFile).toString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<FieldMetaData> mappings = objectMapper.readValue(wirings, new TypeReference<>() {});

        // wire in metadata driven data fetchers
        wiring = wire(wiring, mappings);

        // if you wanted to customise things you would add more wiring here

        // load the schema
        String schema = vertx.fileSystem().readFileBlocking(schemaFile).toString();
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
        SchemaGenerator schemaGenerator = new SchemaGenerator();

        // return the GraphQL server.
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, wiring.build());
        return new GraphQL.Builder(graphQLSchema).build();
    }

    /**
     * Logic to look for <pre>@cache(ms: 15000)</pre> directives on fields. It creates a Guava TTL cache
     * for each field and wraps the DataFetcher invocation in cache get/put logic.
     */
    class FieldQueryCache implements SchemaDirectiveWiring {
        @Override
        public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
            GraphQLFieldDefinition field = environment.getElement();

            val directive = field.getDirective("cache");
            if( directive != null) {
                // create a TTL cache with the timeout set via the directive
                int ms = (int) directive.getArgument("ms").getValue();
                final Cache<String, CompletableFuture> cache = CacheBuilder.newBuilder()
                        .expireAfterWrite(ms, TimeUnit.MILLISECONDS)
                        .build();

                // get the original fetcher
                GraphQLFieldsContainer parentType = environment.getFieldsContainer();
                final DataFetcher uncachedDataFetcher = environment.getCodeRegistry().getDataFetcher(parentType, field);

                // return a wrapper that uses the query cache for this field
                DataFetcher cachedDataFetcher = (env) -> {
                    // sort the arguments map and turn it into a string that can be used as a cache key
                    // https://stackoverflow.com/a/40649809/329496
                    val argMapSortedByKey = env.getArguments().entrySet().stream()
                            .sorted(Map.Entry.<String,Object>comparingByKey().reversed())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
                    val argsKey = argMapSortedByKey.entrySet().stream()
                            .map(e->e.getKey()+"|"+e.getValue().toString())
                            .collect(Collectors.joining(","));
                    // check the query cache
                    var value = cache.getIfPresent(argsKey);
                    if( value == null ){
                        // on cache miss fetch a fresh value
                        value = (CompletableFuture) uncachedDataFetcher.get(env);
                        if( value != null ) {
                            // update the cache
                            cache.put(argsKey, value);
                        }
                    }
                    return value;
                };

                // export the wrapper to the framework
                FieldCoordinates coordinates = FieldCoordinates.coordinates(parentType, field);
                environment.getCodeRegistry().dataFetcher(coordinates, cachedDataFetcher);
            }

            return field;
        }
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
                        // wrap in an async data fetcher
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
