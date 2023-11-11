#!/bin/bash -x

docker run -d \
--mount source=$APP_VOLUME_CONFIG_NAME,target=$APP_CONFIG_PATH \
--mount source=$APP_VOLUME_STORAGE_NAME,target=$APP_STORAGE_PATH \
--mount type=volume,source=test,target=/bot/mnt \
--restart=always \
--name $APP_CONTAINER_NAME \
$APP_IMAGE_NAME || exit 1

exit 0
