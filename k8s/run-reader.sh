#!/bin/bash -ue

# This script runs a PUSH reader job, that will accept 3 listening sessions, then stop

TEST_ID="$(date +%s)"
JOB_NAME=push-reader-${TEST_ID}
: ${NB_SESSIONS:=1}
: ${TIMEOUT:=240s}

echo "Starting '${JOB_NAME}' job..."

kubectl apply -f - << EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: reader
          image: "kast-registry:30005/nats-tester:latest"
          imagePullPolicy: Always
          args:
            - reader
            - --json
            - -s
            - nats.default:4222
            - --flow
            - myflow
            - --client-id
            - push-client
            - --max-listening-sessions
            - "${NB_SESSIONS}"
            - --test-id
            - "pull-test-${TEST_ID}"
EOF

{ 
	kubectl wait --for=condition=Ready pods -l job-name=${JOB_NAME} --timeout=${TIMEOUT}
	echo "Reader pods are running. following pods logs..."
	kubectl logs -l job-name=${JOB_NAME} --tail -1 -f
} &



kubectl wait --for=condition=Complete job/${JOB_NAME} --timeout=${TIMEOUT}

echo "Reader pod completed."

kubectl delete job "${JOB_NAME}"