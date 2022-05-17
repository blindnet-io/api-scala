FROM eclipse-temurin:17

COPY target/scala-3.1.1/blindnet.jar /srv/blindnet.jar

# Should always be set when deployed anyway, but this is a sane default
ENV BN_ENV=${BN_ENV:-production}
CMD java -jar /srv/blindnet.jar
