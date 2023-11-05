#!/usr/bin/bash

docker build -t $APP_IMAGE_NAME $APP_BUILD_PATH
docker container create --name $APP_CONTAINER_NAME $APP_IMAGE_NAME
