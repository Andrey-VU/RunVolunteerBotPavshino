#!/usr/bin/bash

. ./docker.env

docker build -t $imageName $buildPath
docker container create --name $containerName $imageName
docker container start $containerName
