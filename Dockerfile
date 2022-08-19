FROM openjdk:11
RUN mkdir /app
COPY build/distributions/nats-tester.tar /app
WORKDIR /app
RUN tar xf nats-tester.tar
ENTRYPOINT ["/app/nats-tester/bin/nats-tester"]
