FROM amazoncorretto:21-al2023-jdk

WORKDIR /app

RUN yum install -y curl && yum clean all

COPY build/libs/*-SNAPSHOT.jar app.jar

ARG PHASE=dev
ENV SPRING_PROFILES_ACTIVE=${PHASE}

ENTRYPOINT ["java", "-jar", "app.jar"]