#!/bin/bash -ue

export TEST_ID=single-reader
: ${REPLICAS:=2}
export NB_SESSIONS=5
export NB_MSGS="20000,20000,20000,1000000,5000000"
export TIMEOUT=400s



export PATH=$PATH:.


echo ""
echo "Initializing stream..."
echo ""
run-init.sh



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