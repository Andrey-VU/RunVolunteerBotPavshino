#!/usr/bin/bash

docker build -t $APP_IMAGE_NAME $APP_BUILD_PATH || exit 1

exit 0
