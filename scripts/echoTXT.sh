#!/bin/bash
#
# Sample script to show that gwen core has the ability to execute scripts
# This script will read env var, and print it out to stdout.
#
if [ -n "${TMP}" ]; then
  echo "${TMP}"
fi
