# ==================== Etap 1: Build ====================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Kopiuj pliki Maven
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Pobierz zaleznosci (cache warstwy Dockera)
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

# Kopiuj zrodla
COPY src src

# Buduj JAR z profilem produkcyjnym (Vaadin frontend bundle)
RUN ./mvnw clean package -Pproduction -DskipTests -B

# ==================== Etap 2: Runtime ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Kopiuj TYLKO fat JAR (nie .jar.original)
COPY --from=build /app/target/FlashCards-0.0.1-SNAPSHOT.jar app.jar

# Render.com domyslnie ustawia PORT=10000
ENV PORT=10000

# Optymalizacja pamieci dla darmowego planu Render (512 MB RAM)
CMD ["sh", "-c", "java -Xmx384m -Xms256m -XX:+UseSerialGC -Dvaadin.productionMode=true -Dserver.port=$PORT -jar app.jar"]

