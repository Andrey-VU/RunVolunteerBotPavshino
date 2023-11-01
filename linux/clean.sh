#!/usr/bin/bash

. ./docker.env

docker container stop $containerName
docker container rm -f $containerName
docker image rm -f $imageName
docker volume ls | grep '^local' | awk '{print $2}' | xargs -n1 docker volume rm

