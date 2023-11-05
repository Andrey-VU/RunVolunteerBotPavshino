#!/bin/bash

docker run -d \
--mount source=$APP_VOLLUME_CONFIG_NAME, target=$APP_CONFIG_PATH \
--mount source=$APP_VOLLUME_STORAGE_NAME, target=$APP_STORAGE_PATH
--restart=always \
--name $APP_CONTAINER_NAME \
$APP_IMAGE_NAME || exit 1

exit 0
