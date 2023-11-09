#!/usr/bin/bash

cont_id=$(docker container ls -a --format "table {{.ID}}\t{{.Names}}" | egrep "^[a-z0-9]+[[:blank:]]+${APP_CONTAINER_NAME}$" | awk '{print $1}')
test -n "$cont_id" && ( docker rm -vf $cont_id || exit 1 )

exit 0
