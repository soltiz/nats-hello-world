#!/bin/bash -ue


ES_API=http://localhost:9200
CREDS='testuser:cedric'
INDEX=test-events

# WARNING: NOT FOR PRODUCTION : inefficient (1 request per document)
# For each line on stdin that start with an '{'
# insert this line through ES API on localhost

# Trick for K8S : kubectl -n doc-store port-forward service/elasticsearch 9200:9200


while read json_doc ; do
	echo "${json_doc}"
	if [[ "${json_doc}" =~ ^\{ ]] ; then   # True if $a starts with a "z" (wildcard matching).
		res=$(curl -sf -XPOST -H 'content-type: application/json' ${ES_API}/${INDEX}/_doc -u "$CREDS" --data-binary "${json_doc}") || echo "Failure to index: ${res}"
	fi
done