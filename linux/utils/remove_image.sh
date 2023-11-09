#!/usr/bin/bash

img_id=$(docker image ls --format "table {{.ID}}\t{{.Repository}}:{{.Tag}}" | egrep "^[a-z0-9]+[[:blank:]]+${APP_IMAGE_NAME}$" | awk '{print $1}')
test -n "$img_id" && ( docker rmi -f $img_id || exit 1 )

exit 0
