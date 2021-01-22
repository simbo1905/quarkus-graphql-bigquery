package com.github.simbo1905.bigquerygraphql;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @NoArgsConstructor @ToString
@RegisterForReflection
public class FieldMetaData {
    /**
     * Wiring typeName e.g. "Query", "Book"
     */
    String typeName;

    /**
     * Wiring fieldName e.g. "bookById", "author"
     */
    String fieldName;

    /**
     * The BigQuery query sql.
     */
    String sql;

    /**
     * The result set colums and BQ unfortunately does not supply this query result meta-data
     */
    String mapperCsv;

    /**
     * The source parameter (if query) or attribute (if entity) on the GraphQL side e.g., "authorId"
     */
    String gqlAttr;

    /**
     * The destination attribute on the SQL side e.g., "id"
     */
    String sqlParam;
}
