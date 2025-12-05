# --- ETAPA 1: Compilación (Usando una imagen de Maven) ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copiamos el pom.xml y el código fuente
COPY ServerUbicua-master/pom.xml .
COPY ServerUbicua-master/src ./src

# 2. Compilamos el proyecto (esto genera el archivo .war en /app/target/)
RUN mvn clean package -DskipTests

# --- ETAPA 2: Ejecución (Usando la imagen de Tomcat) ---
FROM tomcat:11.0-jdk21-openjdk

# 1. Limpiamos las apps por defecto de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# 2. COPIAMOS el .war generado (usando *.war para evitar errores de nombre)
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

# 3. Configuraciones extra
RUN mkdir -p /usr/local/tomcat/logs
ENV CATALINA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080
CMD ["catalina.sh", "run"]