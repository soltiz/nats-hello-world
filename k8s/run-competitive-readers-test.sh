#!/bin/bash -ue

: ${NB_READERS:=2}
: ${TEST_ID:=competitive-readers-${NB_READERS}}
: ${REPLICAS:=2}
export NB_SESSIONS=4
: ${NB_MSGS:=10000,200000,1000000,4000000}
: ${TIMEOUT:=900s}
export TIMEOUT
export NB_MSGS
export REPLICAS
export NB_READERS
export test_id


export PATH=$PATH:.


echo ""
echo "Initializing stream..."
echo ""
run-init.sh

sleep 20


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