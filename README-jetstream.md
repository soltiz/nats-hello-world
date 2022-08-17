# Tests latence Jetstream

## Links


https://docs.nats.io/using-nats/nats-tools/nats_cli/natsbench

https://www.google.com/search?channel=fs&client=ubuntu&q=what+is+nats+request-reply

https://docs.nats.io/using-nats/nats-tools/nats_cli/natsbench#run-a-request-reply-latency-test

https://nats.io/blog/jetstream-java-client-03-consume/

## Setup


### Server

https://docs.nats.io/running-a-nats-service/introduction/installation#installing-via-docker

	docker pull nats:latest
	docker run -p 4222:4222 -ti nats:latest -js


### Cli

https://github.com/nats-io/natscli/releases

	wget https://github.com/nats-io/natscli/releases/download/v0.0.33/nats-0.0.33-amd64.deb
	sudo gdebi nats-0.0.33-amd64.deb 


## Building tester image:

	bash ./gradlew assemble && docker build -t nats-tester .


## Running writer in docker

	docker run -it --rm --add-host=host.docker.internal:host-gateway nats-tester writer -s host.docker.internal:4222 -n 200


## Running reader in docker

	docker run -it --rm --add-host=host.docker.internal:host-gateway nats-tester reader --pull 10 -s host.docker.internal:4222

## Running reader in k8s

	docker save nats-tester:latest -o /tmp/nats-tester.tar

	load-image.sh /tmp/nats-tester.tar


	kubectl apply -f k8s/writer-job.yaml 
	kubectl apply -f k8s/pull-reader-job.yaml 
	klogs -l job-name=nats-pull-reader # Do not stop the reader (at first, it receives nothing) : 
									   # start the writer in an other window
	kubectl apply -f k8s/writer-job.yaml  


## Running nats cli in k8s

	kubectl exec -n default -it deployment/nats-box -- /bin/sh -l
	nats stream ls
	nats sub flow # Subscribe to writers
	nats consumer ls teststream
	nats consumer info teststream pull-client




## Example NATS-CLI commands

### consumers

 nats consumer rm teststream push-client

### streams
 nats stream ls
 nats stream purge teststream


