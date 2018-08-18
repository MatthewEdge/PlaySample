FROM bigtruedata/sbt AS build
WORKDIR /myapp

# Dependencies cache
COPY ./project /myapp/project
COPY ./lib /myapp/lib
COPY build.sbt /myapp
RUN sbt compile

# Code assembly
COPY . .
RUN sbt assembly && mv ./target/scala*/app.jar ./target

FROM openjdk:8-alpine
WORKDIR /myapp
COPY --from=build /myapp/target/app.jar .
EXPOSE 9000

# Required workaround since ARGs are not expanded at runtime.
# See https://github.com/moby/moby/issues/18492
ARG APP_ENV=application
ENV env=${APP_ENV}
ENTRYPOINT java -Dconfig.resource=${env}.conf -jar /myapp/app.jar
