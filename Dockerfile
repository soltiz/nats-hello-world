FROM openjdk:11
RUN mkdir /app
COPY build/distributions/hello-world.tar /app
WORKDIR /app
RUN tar xf hello-world.tar
ENTRYPOINT ["/app/hello-world/bin/hello-world"]
# ENTRYPOINT ["/app/hello-world/bin/hello-world", "reader", "-s", "host.docker.internal:4222"]
