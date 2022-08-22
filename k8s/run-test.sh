#!/bin/bash -ue

export PATH=$PATH:.

: ${REPLICAS:=2}

echo ""
echo "Initializing stream..."
echo ""
run-init.sh


export NB_SESSIONS=5
export TIMEOUT=300s
export NB_MSGS="10000,10000,100000,300000,1000000"
echo ""
echo "Starting reader..."
echo ""
run-reader.sh &
READER_ID=$!

sleep 10
echo "Injecting documents (${NB_SESSIONS} times) ..."
echo ""
run-writer.sh

echo ""
echo "Waiting for reader stats end..."
wait ${READER_ID}

echo "Test is finished."