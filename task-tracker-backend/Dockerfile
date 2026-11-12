FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew
COPY src src
RUN ./gradlew --no-daemon bootJar
RUN cp build/libs/*-SNAPSHOT.jar app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
