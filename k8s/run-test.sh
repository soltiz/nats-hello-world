#!/bin/bash -ue

export PATH=$PATH:.

echo ""
echo "Initializing stream..."
echo ""
run-init.sh

echo ""
echo "Starting reader..."
echo ""
NB_SESSIONS=5 TIMEOUT=300s run-reader.sh &
READER_ID=$!

sleep 10
echo "Injecting documents (3 times) ..."
echo ""
NB_MSGS=10000 run-writer.sh
sleep 6 # To trigger end of session for reader
NB_MSGS=10000 run-writer.sh
sleep 6 # To wait end of session for reader
NB_MSGS=100000 run-writer.sh
sleep 6 # To wait end of session for reader
NB_MSGS=300000 run-writer.sh
sleep 6  # To wait end of session for reader
NB_MSGS=1000000 run-writer.sh

echo ""
echo "Waiting for reader stats end..."
wait ${READER_ID}

echo "Test is finished."