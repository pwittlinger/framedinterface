#!/bin/bash
if [-d apptainer]; then
 echo "Directory exists."
else
 sudo apt update
 sudo apt install rpm2cpio
 sudo apt install cpio
 curl -s https:////raw.githubusercontent.com/apptainer/apptainer/main/tools/install-unprivileged.sh | bash -s - apptainer
 apptainer/bin/apptainer pull fast-downward.sif docker://aibasel/downward:latest
fi
