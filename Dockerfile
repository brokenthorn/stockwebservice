FROM openjdk:8-jre-alpine
LABEL maintainer="brokenthorn@gmail.com"

# Set runtime and buildtime environment variables.
ENV JAVA_OPTS ""
ENV JDBC_URL ""
ENV JDBC_USERNAME ""
ENV JDBC_PASSWORD ""
ENV API_PASSWORD ""

# We define the user we will use in this instance to prevent using root
# that even in a container, can be a security risk.
ARG APPLICATION_USER=ktor
RUN adduser -D -g '' $APPLICATION_USER

# Create app folder.
RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

# Marks this container to use the specified user for the following RUN, CMD, ENTRYPOINT commands.
USER $APPLICATION_USER

# Extract a distribution of the application into the app folder.
ADD "./build/distributions/stockwebservice-0.1.tar" /app

# Inform Docker of exposed ports (use docker run -P to publish all exposed ports).
EXPOSE 8080/tcp

WORKDIR /app

# Set container entrypoint.
ENTRYPOINT ["./stockwebservice-0.1/bin/stockwebservice"]

