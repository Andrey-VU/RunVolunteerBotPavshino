#!/usr/bin/bash

docker container rm -f $APP_CONTAINER_NAME
docker image rm -f $APP_IMAGE_NAME
docker volume prune -f

