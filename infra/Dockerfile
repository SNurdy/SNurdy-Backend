FROM amazoncorretto:21-al2023-jdk AS builder
WORKDIR /app

COPY build/libs/app.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM amazoncorretto:21-al2023-jdk
WORKDIR /app

COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "org.springframework.boot.loader.launch.JarLauncher"]