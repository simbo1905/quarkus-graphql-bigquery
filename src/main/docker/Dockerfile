## Stage 1 : build with maven builder image with native capabilities
FROM quay.io/quarkus/centos-quarkus-maven:20.3.1-java11 AS build
COPY pom.xml /usr/src/app/
RUN mvn -f /usr/src/app/pom.xml -B de.qaware.maven:go-offline-maven-plugin:1.2.5:resolve-dependencies
COPY src /usr/src/app/src
USER root
RUN chown -R quarkus /usr/src/app
USER quarkus
RUN mvn -f /usr/src/app/pom.xml -Pnative clean package

### Final image based on scratch containing only the binary
FROM scratch
COPY --chown=1000 --from=build /dist /work
# it is possible to add timezone, certificat and new user/group
# COPY --from=xxx /usr/share/zoneinfo /usr/share/zoneinfo
# COPY --from=xxx /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
# COPY --from=xxx /etc/passwd /etc/passwd
# COPY --from=xxx /etc/group /etc/group
EXPOSE 8080
USER 1000
WORKDIR /work/
CMD ["./application", "-Djava.io.tmpdir=/work/tmp"]
