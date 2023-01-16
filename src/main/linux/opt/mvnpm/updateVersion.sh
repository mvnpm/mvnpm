#!/bin/bash
if [ -n "$1" ]; then
  echo "Updating to $1" 
  sudo systemctl stop mvnpm.service
  ln -sf /home/pkruger/uploads/mvnpm-$1-runner mvnpm-runner
  sudo systemctl start mvnpm.service
else
  echo "Please provide the version number as the first argument"
fi
