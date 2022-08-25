#!/bin/bash -ue

export TOOLS_DIR=$(realpath $(dirname "$0"))
source "${TOOLS_DIR}/functions.sh"

RMIMG=0
if [ "${1:-}" == "--rm-after-load" ] ; then 
  RMIMG=1
  shift
fi

[ -z ${1:-} ] && fatal "Please provide an image file name as parameter"


export MASTER=$(get_kast_master)
export IMAGES_DIR=${TOOLS_DIR}/docker-images
export KAST_REGISTRY

[ -z "${KAST_REGISTRY:-}" ] && fatal "please define KAST_REGISTRY ; maybe source 'environment.sh' ?"


for folder in "" "." "${TOOLS_DIR}/docker-images" "${PUNCH_DIR:-/opt/punch}/offline_punch/images_tar"  ; do
  if [ -e "${folder}/$1" ] ; then
    IMG="${folder}/$1"
  fi
done

[ -z ${IMG:-} ] &&  fatal "Could not locate image '$1'."



echo "Loading image locally from '${IMG}'..."

export LOADED=$(cat $IMG | docker image load | grep Loaded)

export IMG_REF="${LOADED#Loaded image: }"

export ALT_REF="${KAST_REGISTRY}/${IMG_REF#*/}"



echo "Tagging local image for Kast registry as '${ALT_REF}' ..."
docker image tag "${IMG_REF}" "${ALT_REF}"

echo "Sending image into master '${MASTER}' node images library..."
docker image save "${ALT_REF}" | ssh "${MASTER}" sudo ctr -n=k8s.io  image import -

echo "Pushing image from master into kast registry..."
ssh "${MASTER}" sudo ctr -n=k8s.io image push --plain-http=true "${ALT_REF}" 

echo "Removing image from master"
ssh "${MASTER}" sudo ctr -n=k8s.io image rm "${ALT_REF}"

if [ $RMIMG -eq 1 ] ; then
  echo "Removing image from local"
  docker image rm "${IMG_REF}" 
  if [ "${IMG_REF}" != "${ALT_REF}" ] ; then
    docker image rm "${ALT_REF}"
  fi
fi

