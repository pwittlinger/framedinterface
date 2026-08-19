# About
This repository provides a graphical user interface for the Framed Autonomy Tool described by [Acitelli et. al (2025)](https://doi.org/10.1016/j.is.2025.102573).
It takes process models in ".pnml" and ".decl" format, and returns an optimal continuation for the given Prefix.
This facilitates different kind of analyses in regards to the Prefix or the process models.

# Getting Started


## Preliminiaries

### Prerequisits
Download the following software to get started.

- Java 17 or 21
- Fast Downward Planning System
    - Apptainer (recommended)
    - python3 (compile it yourself) 

The tool is known to work with Java 17 and 21.

### Running FrAIm

Download the JAR of the Framed Autonomy Tool [here] (https://github.com/giacomo1096/FramedAutonomyTool/blob/79dcaaa88d8f51f28e64723c8650e8d166f6ff9e/tool/FramedAutonomyTool.jar) and put it into the same directory as the launcher project. If you run it from this project, you can simply add it to the root folder.

Additionally copy the two PDDL Domain files [domain_no_reset.pddl](https://github.com/giacomo1096/FramedAutonomyTool/blob/79dcaaa88d8f51f28e64723c8650e8d166f6ff9e/tool/domain_no_reset.pddl) and [domain_with_reset.pddl](https://github.com/giacomo1096/FramedAutonomyTool/blob/79dcaaa88d8f51f28e64723c8650e8d166f6ff9e/tool/domain_with_reset.pddl) into the same directory as the launcher.

The Fast Downward Planner ([here](https://www.fast-downward.org/latest/)) can be installed by executing the file "setup_planner" (Windows: setup_Planner.bat; Linux: setup_planner.sh). For Windows this requires 'WSL' (Windows Subsystem for Linux), which should come pre-installed with Windows 10 or newer. If you have not used it before, install it (requires a restart of the machine).

Alternatively, you can compile Fast Downward yourself. Compile it into a directory called "fast-downward". A full description on how to install the Fast Downward can be found [here](https://github.com/aibasel/downward/blob/main/BUILD.md). This is a little bit more involved for Windows Users, as it involves installing [Visual Studio](https://visualstudio.microsoft.com/de/vs/older-downloads/), [Python](https://www.python.org/downloads/windows/) (make sure it is set in your PATH variables!) and [CMake](http://www.cmake.org/download/). 


## Good to know
- The planner currently supported is Fast-Downward, with A* and a fully blind heuristic.
- The names of the different transition should be unique within a single process model (?)
- Silent transitions are not natively supported.
