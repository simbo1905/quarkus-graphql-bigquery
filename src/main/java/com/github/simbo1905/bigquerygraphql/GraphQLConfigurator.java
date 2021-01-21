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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    @ConfigProperty(name = "wirings.cache.pk", defaultValue = "id")
    String name;

    @SneakyThrows
    private GraphQL setupGraphQL(){
        // first load the metadata and generate the generic DataFetchers.
        String wirings = vertx.fileSystem().readFileBlocking(wiringsFile).toString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<FieldMetaData> mappings = objectMapper.readValue(wirings, new TypeReference<>() {});
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring();
        wiring = wire(wiring, mappings);

        // if you wanted to customise things you would add more wiring here
        wiring = wiring.directiveWiring(new CachedDirective());

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

    /**
     * Logic to look for <pre>@cache(ms: 15000)</pre> directives on fields. It then creates a Guava TTL cache
     * and wraps the DataFetcher invocation in cache get/put logic.
     */
    class CachedDirective implements SchemaDirectiveWiring {
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

                // return a wrapper that uses the cache for this field
                DataFetcher cachedDataFetcher = (e) -> {
                    Object id = e.getArgument(name);
                    if( id == null )
                        return null;
                    var value = cache.getIfPresent(Objects.toString(id));
                    if( value == null ){
                        value = (CompletableFuture) uncachedDataFetcher.get(e);
                        if( value != null )
                            cache.put(Objects.toString(id), value);
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
                        // wrap in an async data fetcher so as to not block Vert.x
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
