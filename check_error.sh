#!/usr/bin/env bash
if [ $(find ~/logs/seata -name "*.log" | xargs grep "Seata test failed" |wc -l) -gt 0 ]; then exit 1; fi
