FROM amazoncorretto:21-al2023-jdk AS builder
WORKDIR /app

COPY build/libs/app.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM amazoncorretto:21-al2023-jdk
WORKDIR /app

# 컨테이너 escape 시 호스트 root 권한이 잡히지 않도록 비특권 사용자로 실행
RUN groupadd -r spring && useradd -r -g spring -d /app -s /sbin/nologin spring
USER spring

COPY --from=builder --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/application/ ./

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "org.springframework.boot.loader.launch.JarLauncher"]
