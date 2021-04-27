FROM openjdk:11-jre
COPY target/ app/
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/beerstock-0.0.2.jar"]
