FROM gradle:9.3.0-jdk21 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN gradle :${MODULE}:bootJar -x test --no-daemon \
    && cp $(ls /workspace/${MODULE}/build/libs/*.jar | grep -v plain) /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
