
export KAST_REGISTRY=kast-registry:30005

export TOOLS_DIR=$(dirname $(realpath "${BASH_SOURCE}"))
export PATH=$PATH:${TOOLS_DIR}

export KAST_CONFIG=${OVH_GIT_ROOT}/configurations/kast
export KAST_CORE_INSTALL_DIR=${OVH_GIT_ROOT}/kast_core
export KUBECONFIG=~/.kube/ovh.conf

source ${SCRIPTS_DIR}/functions.sh
export MASTER=ovhk1
export INGRESS_HOST=ovhk4
export KAST_DOMAIN=punk.ovh
