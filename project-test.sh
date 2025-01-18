#!/bin/sh

./project-create.sh
cd target/project
../universal/stage/bin/gwen -p samples --parallel -b
../universal/stage/bin/gwen --profile samples --dry-run --batch
../universal/stage/bin/gwen gwen/features/samples --parallel --dry-run
GWEN_PROFILE=samples ../universal/stage/bin/gwen -bn
cd ..
rm -rf project
cd ..
