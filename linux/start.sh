#!/bin/bash

./remove_container.sh && ./create_volumes.sh && ./run_image.sh || exit 1

exit 0
