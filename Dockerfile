FROM eclipse-temurin:17

COPY target/scala-3.1.1/blindnet.jar /srv/blindnet.jar

ARG BN_ENV
ENV BN_ENV=${BN_ENV:-production}
CMD java -jar /srv/blindnet.jar
