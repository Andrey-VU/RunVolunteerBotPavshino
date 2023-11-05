#!/bin/bash

APP_BUILD_PATH=..

APP_IMAGE_NAME=pavshino5verst/runvolunteerbotpavshino_img:v1
APP_CONTAINER_NAME=runvolunteerbotpavshino_cnt

APP_VOLUME_CONFIG_NAME=config
APP_VOLUME_STORAGE_NAME=storage

APP_CONFIG_PATH=/bot/local_storage
APP_STORAGE_PATH=/bot/local_storage

export APP_BUILD_PATH APP_IMAGE_NAME APP_CONTAINER_NAME APP_VOLUME_CONFIG_NAME APP_VOLUME_STORAGE_NAME APP_CONFIG_PATH APP_STORAGE_PATH

echo
cat hub.pass | docker login -u pavshino5verst --password-stdin

echo
env | grep '^APP\_'
echo
