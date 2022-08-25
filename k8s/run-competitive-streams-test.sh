#!/bin/bash -ue

: ${NB_READERS:=2}
export NB_READERS

: ${NB_FLOWS:=2}
export NB_FLOWS

: ${TEST_ID:=multistreams-${NB_READERS}-${NB_FLOWS}}
export TEST_ID

: ${REPLICAS:=2}
export REPLICAS

: ${NB_SESSIONS:=4}
export NB_SESSIONS

: ${NB_MSGS:=10000,200000,1000000,4000000}
export NB_MSGS

: ${TIMEOUT:=600s}
export TIMEOUT

: ${MAX_MSG_RATE:=1000}
export MAX_MSG_RATE

indexes=$(eval 'echo {1..'${NB_FLOWS}'}')

export PATH=$PATH:.

for i in ${indexes} ; do
	export FLOW_NAME=testflow-${i}
	export STREAM_NAME=teststream-${i}
	echo ""
	echo "Initializing stream..."
	echo ""
	run-init.sh
done

for i in ${indexes} ; do
	export FLOW_NAME=testflow-${i}
	echo ""
	echo "Starting reader $i ..."
	echo ""
	run-reader.sh &
	READER_ID[$i]=$!
done

sleep 15

for i in ${indexes} ; do
	export FLOW_NAME=testflow-${i}
	echo "Injecting documents (${NB_SESSIONS} times) for stream $i ..."
	echo ""
	run-writer.sh &
	WRITER_ID[$i]=$!
done


for i in ${indexes} ; do
	echo ""
	echo "Waiting for writer #$i end..."
	wait ${WRITER_ID[$i]}
done


for i in ${indexes} ; do
	echo ""
	echo "Waiting for reader #$i end..."
	wait ${READER_ID[$i]}
done

echo "Test is finished."