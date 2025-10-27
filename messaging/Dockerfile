# Etapa 1: Build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .

# Descargar dependencias (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app


# Instalar herramientas de red 
RUN apk add --no-cache curl wget

# Copiar el JAR compilado desde la etapa de build
COPY --from=build /app/target/messaging-api-*.jar /app/messaging.jar

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app

USER spring:spring

# Exponer el puerto de la aplicación
EXPOSE 8089

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8089/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/messaging.jar"]