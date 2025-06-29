@echo off
setlocal

REM Check if "apptainer" directory exists
if exist apptainer/ (
	echo "folder exists"
	timeout /t 5
) else (
    echo Folder "apptainer" not found. Running installation...
    wsl sudo apt update
    wsl sudo apt install rpm2cpio
    wsl sudo apt install cpio
    wsl curl -s https://raw.githubusercontent.com/apptainer/apptainer/main/tools/install-unprivileged.sh | wsl bash -s - apptainer
    wsl apptainer/bin/apptainer pull fast-downward.sif docker://aibasel/downward:latest
    timeout /t 10

)

endlocal