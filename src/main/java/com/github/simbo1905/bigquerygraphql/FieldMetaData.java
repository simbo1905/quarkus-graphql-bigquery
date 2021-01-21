package com.github.simbo1905.bigquerygraphql;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

// TODO turn this back into a Lombok type
@RegisterForReflection
public class FieldMetaData {

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getMapperCsv() {
        return mapperCsv;
    }

    public void setMapperCsv(String mapperCsv) {
        this.mapperCsv = mapperCsv;
    }

    public String getGqlAttr() {
        return gqlAttr;
    }

    public void setGqlAttr(String gqlAttr) {
        this.gqlAttr = gqlAttr;
    }

    public String getSqlParam() {
        return sqlParam;
    }

    public void setSqlParam(String sqlParam) {
        this.sqlParam = sqlParam;
    }

    public FieldMetaData() {
    }

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

    @Override
    public String toString() {
        return "QueryAndMapperCsv{" +
                "typeName='" + typeName + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", sql='" + sql + '\'' +
                ", mapperCsv='" + mapperCsv + '\'' +
                ", gqlAttr='" + gqlAttr + '\'' +
                ", sqlParam='" + sqlParam + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldMetaData that = (FieldMetaData) o;
        return Objects.equals(typeName, that.typeName) && Objects.equals(fieldName, that.fieldName) && Objects.equals(sql, that.sql) && Objects.equals(mapperCsv, that.mapperCsv) && Objects.equals(gqlAttr, that.gqlAttr) && Objects.equals(sqlParam, that.sqlParam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, fieldName, sql, mapperCsv, gqlAttr, sqlParam);
    }
}
