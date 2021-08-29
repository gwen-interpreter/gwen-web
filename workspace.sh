#!/usr/bin/env bash
rm -f gwen-workspace/.history
rm -rf gwen-workspace/target
rm -rf target/workspace
mkdir target
mkdir target/workspace
cd ../gwen-gpm
git checkout tags/v3.0.5
sbt assembly
cp target/gwen-gpm-assembly-3.0.5.jar ../gwen-web/gwen-workspace/gwen-gpm.jar
git switch -
git checkout master
cd ../gwen-web
cp -r samples gwen-workspace/samples
zip -r target/workspace/gwen-workspace.zip gwen-workspace -x "*.DS_Store"
./workspace.py
rm gwen-workspace/gwen-gpm.jar
rm -rf gwen-workspace/samples
cd target/workspace
unzip gwen-workspace.zip
