#!/bin/bash

java -Xmx768m \
    -Dvar1="${VAR_1}" \
    -Dvar2="${VAR_2}" \
    -jar validator-0.0.1-SNAPSHOT.jar