sbt universal:stage
cd target
rm -rf project
mkdir project
cd project
../universal/stage/bin/gwen init --docker --jenkins
../universal/stage/bin/gwen -p samples --parallel -b
../universal/stage/bin/gwen --process samples --dry-run --batch
../universal/stage/bin/gwen gwen/samples --parallel --dry-run
GWEN_PROCESS=samples ../universal/stage/bin/gwen -bn
cd ..
rm -rf project
cd ..
