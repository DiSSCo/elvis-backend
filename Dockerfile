FROM gradle:6.6-jdk14 as build

ADD --chown=gradle . /src
WORKDIR /src
RUN gradle installDist

FROM openjdk:14-alpine

COPY --from=build /src/build/install/elvis /app
WORKDIR /app

EXPOSE 8080
CMD [ "./bin/elvis" ]