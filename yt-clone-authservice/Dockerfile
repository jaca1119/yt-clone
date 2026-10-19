FROM eclipse-temurin:25-jdk AS build
#FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY . .

RUN chmod +x ./gradlew

#build and cache gradle dependencies
#RUN --mount=type=cache,target=/root/.gradle \
#    ./gradlew bootJar --no-daemon

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
#FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]