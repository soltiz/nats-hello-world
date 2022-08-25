#!/bin/bash -ue

# This script runs a PUSH reader job, that will accept 3 listening sessions, then stop

TEST_NUM="$(date +%s)"
: ${NB_MSGS:=10000}
: ${FLOW_NAME:=myflow}
: ${TEST_ID:=pull-${TEST_NUM}}
: ${TIMEOUT:=900s}
: ${BATCH_SIZE:=200}
: ${SESSIONS_INTERVAL:=20}
: ${MAX_MSG_RATE:=0}
echo "RATE=${MAX_MSG_RATE}"

JOB_NAME=writer-${TEST_ID}-${FLOW_NAME}



echo "Starting '${JOB_NAME}' job to send ${NB_MSGS} messages..."

kubectl apply -f - << EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
spec:
  template:
    metadata:
      labels:
        app: nats-writer
        app.kubernetes.io/instance: nats-writer
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
            - ${FLOW_NAME}
            - --test-id
            - "${TEST_ID}"
            - --async-batches
            - "${BATCH_SIZE}"
            - --interval-between-sessions
            - "${SESSIONS_INTERVAL}"
            - --max-rate
            - "${MAX_MSG_RATE}"
EOF




{ 
  kubectl wait --for=condition=Ready pods -l job-name=${JOB_NAME} --timeout=${TIMEOUT}
  echo "Writer pods are running for job/${JOB_NAME}. following pods logs..."
  kubectl logs -l job-name=${JOB_NAME} --tail -1 -f
} &
LISTENER=$!


kubectl wait --for=condition=Complete job/${JOB_NAME} --timeout=${TIMEOUT}
echo "Writer pod completed for job/${JOB_NAME}."

wait $LISTENER


kubectl delete job "${JOB_NAME}"




