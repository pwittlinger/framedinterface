#!/bin/bash

cd enhsp
bash compile
cp enhsp-dist/libs ../framedinterface/dependencies -r
cp enhsp-dist/enhsp.jar ../framedinterface/dependencies

