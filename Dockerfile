# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Dar permisos de ejecución al wrapper de maven
RUN chmod +x ./mvnw
# Compilar el proyecto sin correr las pruebas (para que sea más rápido)
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copiar el archivo jar generado en el paso anterior
COPY --from=build /app/target/inncontrol-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
