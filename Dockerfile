FROM bigtruedata/sbt AS build
WORKDIR /usr/src/app
COPY . .
RUN sbt assembly && mv /usr/src/app/target/scala*/app.jar /usr/src/app/target

FROM openjdk:alpine
WORKDIR /usr/src/app
RUN apk add git --no-cache
COPY --from=build /usr/src/app/target/app.jar .
CMD ["java", "-jar", "/usr/src/app/app.jar"]