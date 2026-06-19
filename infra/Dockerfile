FROM amazoncorretto:21-al2023-jdk AS builder
WORKDIR /app

COPY build/libs/app.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM amazoncorretto:21-al2023-jdk
WORKDIR /app

# 컨테이너 escape 시 호스트 root 권한이 잡히지 않도록 비특권 사용자로 실행
# AL2023 베이스 이미지에는 shadow-utils가 없어 groupadd/useradd가 빠져있으므로 함께 설치
RUN dnf install -y shadow-utils && \
    dnf clean all && \
    rm -rf /var/cache/dnf && \
    groupadd -r spring && \
    useradd -r -g spring -d /app -s /sbin/nologin spring
USER spring

COPY --from=builder --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/application/ ./

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "org.springframework.boot.loader.launch.JarLauncher"]
