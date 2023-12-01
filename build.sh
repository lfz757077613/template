#!/bin/bash

if [ $# != 1 ]; then
  echo "usage: build.sh docker_repo_password"
  exit 1
fi

# 政务云
#region_array=("cn-north-2-gov-1")
for region in ${region_array[@]}; do
  echo ${region}
  EXTERNAL_REGISTRY_DOMAIN=messaging-registry.${region}.cr.aliyuncs.com
  docker login --username=xxx --password=$1 $EXTERNAL_REGISTRY_DOMAIN
  if [ $? != 0 ]; then
    echo "docker login registry failed!"
    exit 1
  fi
  mvn -e -B -q -U clean package -Dmaven.test.skip=true -DskipTests -P${region}
  if [ $? != 0 ]; then
    echo "mvn build failed!"
    exit 1
  fi
  IMAGE_REPO=$EXTERNAL_REGISTRY_DOMAIN/xxx
  docker build -t $IMAGE_REPO:${region} -f Dockerfile .
  if [ $? != 0 ]; then
    echo "docker build failed!"
    exit 1
  fi
  docker push $IMAGE_REPO:${region}
  if [ $? != 0 ]; then
    echo "docker push image failed!"
    exit 1
  fi
done