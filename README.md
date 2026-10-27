# About
This repository provides a graphical user interface for the Framed Autonomy Tool described by [Acitelli et al. (2025)](https://doi.org/10.1016/j.is.2025.102573) and [Wittlinger et al. (2026)](https://arxiv.org/abs/2607.16738)
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
- ENHSP (Numeric planning System)

The tool is known to work with Java 17 and 21.

### Running FrAIm

**Multi-Perspective**

Download the JAR for the multi-perspective framed autonomy tool [here](https://github.com/pwittlinger/data-framed-autonomy/blob/resource-alloc/pddl_gen-1.0-SNAPSHOT-launcher.jar) (opens GitHub). Put the Jar file into the same directory as the launcher project. If you run it from this project, you can simply add it to the root folder. Also copy the PDDL domain files to the folder ./dependencies:
- 
- 
- 
-

In order to run the Planner, please download [ENHSP](https://github.com/hstairs/enhsp) and follow the instructions to compile the jar file. Please add the following files and folders to ./dependencies:
- enhsp.jar
- libs/*





**Data-Agnostic**

Download the JAR of the (data-agnostic) Framed Autonomy Tool [here] (https://github.com/giacomo1096/FramedAutonomyTool/blob/79dcaaa88d8f51f28e64723c8650e8d166f6ff9e/tool/FramedAutonomyTool.jar) and put it into the same directory as the launcher project. If you run it from this project, you can simply add it to the root folder.

Additionally copy the two PDDL Domain files [domain_no_reset.pddl](https://github.com/giacomo1096/FramedAutonomyTool/blob/79dcaaa88d8f51f28e64723c8650e8d166f6ff9e/tool/domain_no_reset.pddl) and [domain_with_reset.pddl](https://github.com/giacomo1096/FramedAutonomyTool/blob/79dcaaa88d8f51f28e64723c8650e8d166f6ff9e/tool/domain_with_reset.pddl) into the same directory as the launcher.

The Fast Downward Planner ([here](https://www.fast-downward.org/latest/)) can be installed by executing the file "setup_planner" (Windows: setup_Planner.bat; Linux: setup_planner.sh). For Windows this requires 'WSL' (Windows Subsystem for Linux), which should come pre-installed with Windows 10 or newer. If you have not used it before, install it (requires a restart of the machine).

Alternatively, you can compile Fast Downward yourself. Compile it into a directory called "fast-downward". A full description on how to install the Fast Downward can be found [here](https://github.com/aibasel/downward/blob/main/BUILD.md). This is a little bit more involved for Windows Users, as it involves installing [Visual Studio](https://visualstudio.microsoft.com/de/vs/older-downloads/), [Python](https://www.python.org/downloads/windows/) (make sure it is set in your PATH variables!) and [CMake](http://www.cmake.org/download/). 


## Good to know
- The planner currently supported is Fast-Downward, with A* and a fully blind heuristic for data-agnostic process frames.
- Multi-perspective process frames (data- or temporal guards) are solved using the Expressive Numeric Heuristic Search Planner [ENHSP](https://sites.google.com/view/enhsp/), with A* and a fully blind heuristic.
- The names of the different transition should be unique within a single process model (i.e. no duplicate labels within a single process model)
- Silent transitions are not natively supported.
- Preferably, the activity labels are alphanumeric. We did not (extensively) test activity labels with the following characters:
    - Special characters (dot, comma, semicolon, etc.)
    - White spaces (neither leading, trailing, or inbetween)

- The semantics of *Precedence* Constraints are such that the activation activity is inverted. In case you are using data conditions, please ensure that the Activation and Target conditions are referring to the correct label.


## Known Issues

- **Multiple Petri nets in process frame.** While using multiple Petri nets for the process frame is supported, the resulting grounding time may be prohibitively long. This is a known issue, and we are working on improving the performance.

- **Declare constraints that differ only in their data/time conditions are merged.** When a `.decl` model is loaded, two constraints with the same template and activities are treated as duplicates (`DeclareConstraint.equals`/`hashCode` ignore the conditions), so only the first one is kept. For example, of
  ```
  Response[ActivityG, ActivityP] |A.integer > 10 |T.integer < 10 |
  Response[ActivityG, ActivityP] |A.integer > 50 | |
  ```
  only the first constraint is shown in the Data-Aware page's Constraints pane, replayed on the models and written to `output/violation_costs.txt`. The data-aware PDDL generator keeps both (naming the second `Response_ActivityG_ActivityP_2`), so the second constraint's violation cost is missing from that file.
  A fix would be to include the conditions in `DeclareConstraint.equals`/`hashCode`; on the Data-Agnostic page, which ignores conditions, such constraints would then show up twice.
