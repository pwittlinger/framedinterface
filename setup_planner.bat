@echo off
setlocal

cd ..
git clone https://github.com/hstairs/enhsp.git

"C:\Program Files\Git\bin\sh.exe" --login -c "./framedinterface/compile_and_copy.sh"