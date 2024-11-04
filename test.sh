#!/bin/sh

sbt universal:stage
cd target
rm -rf project
mkdir project
cd project
../universal/stage/bin/gwen init --docker --jenkins
../universal/stage/bin/gwen -p samples --parallel -b
../universal/stage/bin/gwen --profile samples --dry-run --batch
../universal/stage/bin/gwen gwen/features/samples --parallel --dry-run
GWEN_PROFILE=samples ../universal/stage/bin/gwen -bn
cd ..
if [ $# -eq 0 ]
  then
    rm -rf project
fi
cd ..
