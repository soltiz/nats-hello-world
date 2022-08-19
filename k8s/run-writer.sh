#!/bin/bash -ue

# This script runs a PUSH reader job, that will accept 3 listening sessions, then stop

TEST_NUM="$(date +%s)"
: ${TEST_ID:=${TEST_NUM}}
: ${NB_MSGS:=10000}
: ${TIMEOUT:=120s}
: ${BATCH_SIZE:=200}

JOB_NAME=writer-${TEST_ID}



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
        - name: writer
          image: "kast-registry:30005/nats-tester:latest"
          imagePullPolicy: Always
          args:
            - writer
            - -s
            - nats.default:4222
            - --json
            - -n
            - "${NB_MSGS}"
            - --flow
            - myflow
            - --test-id
            - "pull-test-${TEST_ID}"
            - --async-batches
            - "${BATCH_SIZE}"
EOF




{ 
  kubectl wait --for=condition=Ready pods -l job-name=${JOB_NAME} --timeout=${TIMEOUT}
  echo "Writer pod is running. following pods logs..."
  kubectl logs -l job-name=${JOB_NAME} --tail -1 -f
} &


kubectl wait --for=condition=Complete job/${JOB_NAME} --timeout=${TIMEOUT}
echo "Writer pod completed."

kubectl delete job "${JOB_NAME}"




