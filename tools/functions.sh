#!/bin/bash -ue
FILE=$_


function fatal() {
	echo "" 1>&2
	echo "FATAL ERROR: $*" 1>&2
	echo "" 1>&2
	exit 1
}

function error() {
  echo "$*" 1>&2
}

function step () {
  echo ""
  echo "###########################################################"
  echo "#"
  echo "#  Step $* ..."
  echo "#"
  echo "###########################################################"
  echo ""
  if [ ${STEPS_TO_SKIP:-0} -ge $1 ] || [ ${SKIP_STEPS_AFTER:-9999} -lt $1 ]; then
    echo "SKIPPED"
    return 1
  fi
  return 0

} 

function yaml_to_json() {


  input=${1:-}
  [ -z "$input" ] && error "Please provide a yaml file path as parameter to 'yaml_to_json'." && return -1
  if ! [ -e "${input}" ] ; then
    error "Could not read '$input' yaml file."
    return -2
  fi
  python3 <<- EOF
import yaml
import json
from yaml import Loader
with open("${input}","r") as f:
  y = yaml.load(f, Loader)
  print(json.dumps(y))
EOF
}


function get_kast_master() {
  grep -A1 -- '\[kube_master' ${KAST_CONFIG}/inventories/hosts | tail -n -1 | sed 's/\s.*$//g'
}


function get_first_ingress() {
    grep -A1 -- '\[ingress' ${KAST_CONFIG}/inventories/hosts | tail -n -1 | sed 's/\s.*$//g'
}

function get_kast_domain() {
  sed -n 's/^\s*domain\s*:\s*//p' "${DEPLOYMENT_VALUES}"
}

[ "$0" != "${FILE}" ] || fatal "'$0' must be invoked through 'source $0'"
