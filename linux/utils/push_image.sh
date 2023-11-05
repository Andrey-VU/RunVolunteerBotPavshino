#!/bin/bash

docker push $APP_IMAGE_NAME || exit 1

exit 0
