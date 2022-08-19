# Tests latence Jetstream

## Setup


### Local NATS Server

https://docs.nats.io/running-a-nats-service/introduction/installation#installing-via-docker

	docker pull nats:latest
	docker run -p 4222:4222 -ti nats:latest -js


### Installing NATS Cli

https://github.com/nats-io/natscli/releases

	wget https://github.com/nats-io/natscli/releases/download/v0.0.33/nats-0.0.33-amd64.deb
	sudo gdebi nats-0.0.33-amd64.deb 


### Building tester image:

	bash ./gradlew assemble && docker build -t nats-tester .

### Pushing tester image to K8S kast registry

This is of course only needed when running tests in a Kast cluster

Please ensure that you have a correctly configured KUBECONFIG:

	kubectl get nodes

	docker save nats-tester:latest -o /tmp/nats-tester.tar
	load-image.sh /tmp/nats-tester.tar

## Running topic initialization (in docker)

Note: topic initialization can be done using 'nats create'. When running in a cluster, this tool is useful to simplify testing process,
in particular because this init subcommand will delete the topic and re-create it (except if '--no-delete' arg is provided)

	docker run -it --rm --add-host=host.docker.internal:host-gateway nats-tester init -s host.docker.internal:4222 --stream mystream --flow myflow



Typical errors:

* "io.nats.client.JetStreamApiException: stream name already in use [10058]"

	This may indicate that the stream definition is different than the one used to create the same stream name.
	Note that by default, the "init" subcommand will delete the existing stream. So this error should only arise when using '--no-delete' option





## Running writer in docker


* With human-intended console logging, 40 000 messages, synchronously (1 message at a time)

	docker run -it --rm --add-host=host.docker.internal:host-gateway nats-tester writer -s host.docker.internal:4222 --flow myflow -n 40000

* With machine-intended console json logging, 100 000 messages, assynchronously (5000 messages acks waiting at a time)

	docker run -it --rm --add-host=host.docker.internal:host-gateway nats-tester writer -s host.docker.internal:4222 -n 100000 --async-batches 5000 --json


## Running reader in docker

* With human-intended console logging, in 'push mode' 

	docker run -it --rm --add-host=host.docker.internal:host-gateway nats-tester reader -s host.docker.internal:4222 --flow myflow


* With human-intended console logging, in 'pull mode' with a 1000-messages fetching strategy

	docker run -it --rm --add-host=host.docker.internal:host-gateway nats-tester reader --pull 1000 -s host.docker.internal:4222


## Running topic initialization in k8s



	kubectl apply -f k8s/init-job.yaml --wait


## Running reader in k8s


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


## Test run

* Have an available elasticsearch locally. E.g.:


	kubectl -n doc-store port-forward service/elasticsearch 9200:9200


* In some window, monitor your test jobs:

	watch -n 1 kubectl get jobs,pods

* Start the test (in an other window, of course)

	cd k8s
	./run-test.sh  | ./es_indexer.sh 


* Go to kibana 



## Tricks

* Running reader with dump of Java JIT compilations:
* 
	docker run -e NATS_TESTER_OPTS="-XX:+PrintCompilation" -it --rm --add-host=host.docker.internal:host-gateway nats-tester reader -s host.docker.internal:4222 


* Running reader without Tiered java compilation (stablizes performances quicker)

	docker run -e NATS_TESTER_OPTS="-XX:-TieredCompilation" -it --rm --add-host=host.docker.internal:host-gateway nats-tester reader -s host.docker.internal:4222 


* Forwarding local 9200 to cluster Elasticsearch API:

	kubectl -n doc-store port-forward service/elasticsearch 9200:9200
	curl localhost:9200 -u 'admin:$0Adm1n'






## Links


https://docs.nats.io/using-nats/nats-tools/nats_cli/natsbench

https://www.google.com/search?channel=fs&client=ubuntu&q=what+is+nats+request-reply

https://docs.nats.io/using-nats/nats-tools/nats_cli/natsbench#run-a-request-reply-latency-test

https://nats.io/blog/jetstream-java-client-03-consume/

* For JVM compilation:
* 
https://blogs.oracle.com/javamagazine/post/the-best-hotspot-jvm-options-and-switches-for-java-11-through-java-17
https://theboreddev.com/analysing-jit-compilation-in-jvm/


https://stackoverflow.com/questions/53885832/why-is-server-option-there-when-server-vm-is-the-default-option

-client and -server are ignored on modern JVMs, as easy as that. There are two JITcompilers C1 and C2, but there are 5 tiers, all the details in the entire glory are here in the comments.

These flags used to control how C1 and C2 would act - disable or not; this is now controlled by two other flags : XX:-TieredCompilation -XX:TieredStopAtLevel=1




