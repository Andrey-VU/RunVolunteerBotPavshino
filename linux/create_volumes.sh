#!/usr/bin/bash

docker volume ls | egrep -q "^local+[[:blank:]]+${APP_VOLUME_CONFIG_NAME}$" || docker volume create ${APP_VOLUME_CONFIG_NAME} || exit 1
docker volume ls | egrep -q "^local+[[:blank:]]+${APP_VOLUME_STORAGE_NAME}$" || docker volume create ${APP_VOLUME_STORAGE_NAME} || exit 1

exit 0
