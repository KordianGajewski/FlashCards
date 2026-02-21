# ==================== Etap 1: Build ====================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Kopiuj pliki Maven
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Pobierz zależności (cache warstwy Dockera)
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

# Kopiuj źródła
COPY src src

# Buduj JAR z profilem produkcyjnym (Vaadin frontend bundle)
RUN ./mvnw clean package -Pproduction -DskipTests -B

# ==================== Etap 2: Runtime ====================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Kopiuj zbudowany JAR
COPY --from=build /app/target/*.jar app.jar

# Render.com domyślnie ustawia PORT=10000
ENV PORT=10000

# Optymalizacja pamięci dla darmowego planu Render (512 MB RAM)
# sh -c gwarantuje rozwinięcie $PORT w runtime
CMD ["sh", "-c", "java -Xmx384m -Xms256m -XX:+UseSerialGC -Dserver.port=$PORT -jar app.jar"]

