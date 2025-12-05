FROM tomcat:11.0-jdk21-openjdk

# Copiar el WAR
COPY ServerUbicua-master/target/ServerExampleUbicomp.war /tmp/ROOT.war

# Extraer el WAR en el directorio ROOT
RUN cd /usr/local/tomcat/webapps && \
    mkdir -p ROOT && \
    cd ROOT && \
    jar -xf /tmp/ROOT.war && \
    rm /tmp/ROOT.war

# Descargar y copiar el driver MariaDB a lib de Tomcat
RUN cd /usr/local/tomcat/lib && \
    apt-get update && apt-get install -y wget && \
    wget https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.1.4/mariadb-java-client-3.1.4.jar && \
    rm -rf /var/lib/apt/lists/*

# Crear directorio de logs
RUN mkdir -p /usr/local/tomcat/logs

# Variables de memoria
ENV CATALINA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

CMD ["catalina.sh", "run"]