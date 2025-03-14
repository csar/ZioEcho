#FROM amazoncorretto:21 AS stage0
#LABEL snp-multi-stage="intermediate"
#LABEL snp-multi-stage-id="5bf11b04-d2ee-47ba-86fe-bff5332dd72b"
#WORKDIR /opt/docker
#COPY target/docker/stage/2/opt /opt/docker
#COPY target/docker/stage/4/opt /opt/docker
#USER root

FROM amazoncorretto:21 AS mainstage
USER root
WORKDIR /opt/docker
COPY target/universal/stage/lib /opt/docker
EXPOSE 8080
ENTRYPOINT ["java","-jar", "zioecho.zioecho-0.1.0-SNAPSHOT-launcher.jar"]
CMD []
