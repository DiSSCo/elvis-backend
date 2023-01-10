FROM gradle:7-jdk17-alpine as build

ADD --chown=gradle . /src
WORKDIR /src
RUN gradle installDist

FROM eclipse-temurin:17-jre-alpine

COPY --from=build /src/build/install/elvis /app
WORKDIR /app

EXPOSE 8080
CMD [ "./bin/elvis" ]