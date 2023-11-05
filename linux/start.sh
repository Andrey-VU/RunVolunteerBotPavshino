#!/bin/bash

./remove_container.sh && create_volume.sh && ./run_image.sh || exit 1

exit 0