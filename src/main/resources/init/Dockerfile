# base alpine image with node
FROM node:21-alpine

# install bash and java
RUN apk update \
    && apk add bash \
    && apk add openjdk17-jre

# Run as a non root user
RUN addgroup -S gwen && adduser -S gwen -G gwen -u 4936
USER gwen
