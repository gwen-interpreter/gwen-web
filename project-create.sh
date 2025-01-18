#!/bin/sh

sbt universal:stage
cd target
rm -rf project
mkdir project
cd project
yarn add -D @gweninterpreter/gwen-web || true
../universal/stage/bin/gwen init --docker --jenkins
