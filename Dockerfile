FROM gradle:jdk8

WORKDIR /scratch

COPY . .

RUN gradle war

FROM openjdk:8-jdk

COPY --from=0 /scratch/build/libs/rr.war /rr.war

ENTRYPOINT ["java", "-jar", "rr.war"]
