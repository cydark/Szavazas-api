# 1. lépés: build JDK-val
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# a függőségek külön rétegben, így forráskód-változásnál nem töltődnek le újra
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar \
    && java -Djarmode=tools -jar build/libs/voting-api-*.jar extract --layers --launcher --destination extracted

# 2. lépés: futtatás csak JRE-vel, nem root felhasználóval
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --no-create-home app \
    && mkdir /data \
    && chown app:app /data

# rétegenként, a ritkán változótól a gyakran változóig
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
