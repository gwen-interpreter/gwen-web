#!/bin/sh

sbt universal:stage
cd target
rm -rf project
mkdir project
cd project
yarn add -D @gweninterpreter/gwen-web || true
../universal/stage/bin/gwen init --docker --jenkins

if [ $1 = "test" ]; then
  cd target/project
  ../universal/stage/bin/gwen -p samples --parallel -b
  ../universal/stage/bin/gwen --profile samples --dry-run --batch
  ../universal/stage/bin/gwen gwen/features/samples --parallel --dry-run
  GWEN_PROFILE=samples ../universal/stage/bin/gwen -bn
  cd ..
  rm -rf project
  cd ..
fi