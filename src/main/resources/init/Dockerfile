# base alpine image with node
FROM node:21-alpine

# install bash and java
RUN apk update \
    && apk add bash \
    && apk add jq \
    && apk add curl \
    && apk add openjdk17-jre
