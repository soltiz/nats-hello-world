#!/bin/bash -ue

export NB_READERS=2
export TEST_ID=cooperative-readers-${NB_READERS}
: ${REPLICAS:=2}
export NB_SESSIONS=4
export NB_MSGS="10000,200000,1000000,4000000"
export TIMEOUT=600s



export PATH=$PATH:.


echo ""
echo "Initializing stream..."
echo ""
run-init.sh



echo ""
echo "Starting reader..."
echo ""
run-persistent-reader.sh &
READER_ID=$!

sleep 10
echo "Injecting documents (${NB_SESSIONS} times) ..."
echo ""
run-writer.sh

echo ""
echo "Waiting for reader stats end..."
wait ${READER_ID}

echo "Test is finished."