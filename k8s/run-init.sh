#!/bin/bash -ue

# This script runs a stream init to ensure stream is created with appropriate parameters

: ${TEST_ID:="$(date +%s)"}
: ${STREAM_NAME:=mystream}
: ${FLOW_NAME:=myflow}
: ${REPLICAS:=1}
: ${TIMEOUT:=20s}

echo "Init for stream '${STREAM_NAME}'..."
JOB_NAME=stream-init-${STREAM_NAME}


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
        - name: streaminit
          image: "kast-registry:30005/nats-tester:latest"
          imagePullPolicy: Always
          args:
            - init
            - -s
            - nats.default:4222
            - --stream
            - ${STREAM_NAME}
            - --flow
            - ${FLOW_NAME}
            - --replicas
            - "${REPLICAS}"
EOF

kubectl wait --for=condition=Complete job/${JOB_NAME} --timeout=${TIMEOUT}

kubectl logs -l job-name=${JOB_NAME} --tail -1

kubectl delete job "${JOB_NAME}"