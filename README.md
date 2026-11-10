# About
This repository provides a graphical user interface for the Framed Autonomy Tool described by [Acitelli et al. (2025)](https://doi.org/10.1016/j.is.2025.102573) and [Wittlinger et al. (2026)](https://arxiv.org/abs/2607.16738)
It takes procedural process models in ".pnml" (Petri nets) and declarative process specifications in ".decl" format (Multi-Perspective DECLARE), and returns an optimal continuation for a given (possibly empty) prefix.
This facilitates different kind of analyses in regards to the prefix or the porcess frame.

### Prerequisits
We have tested the tool on Windows (10 and 11), and Linux.

In either case, you will need Java 21 installed and added to your system PATH.

- Download [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) and add it to the system PATH.
- Install [ENHSP](https://github.com/hstairs/enhsp) (Numeric planning System) and add it to the "dependencies" folder. For your convenience, we provide to ways to simplify the installation.

    - *git*: If you have git installed, you can simply execute ```setup_planner.bat``` (Windows) or ```setup_planner.sh``` (Linux)
    - *maven*: If you prefer maven, you can execute ```mvn install``` in the folder "./planner" to download and install the planner.

### Running FrAIm

Once you have everything installed, you can run FrAIm by either (i) double clicking on the launcher ``framedinterface-1.0-launcher.jar`` or (ii) executing ``java -jar framedinterface-1.0-launcher.jar`` from the root directory.

This will launch the main overview page of FrAIm. Here you can upload process models.

![The overview page that is launched when FrAIm first starts up.](docs\images\screenshots\fraim-overview-page.png "Main page")


Under the tab ``Optimization'' you can modify various cost models (Constraint Violation/Satisfaction, Activity Cost) and build a prefix by clicking on the Activities shown in the process viewers. You can change the cost of the constraints or activities by either selecting them and right-clicking, or by editing the cost cell directly. 

When you want to find a continuation, click the button `Run Planner' in the bottom right.
By default, the recovery strategy "Reset" is used. If you want to try another one (no-recovery, discard, skip), you can select it by clicking the cog wheel immediately left of the "Run Planner" button.

![The main optimization page of FrAIm.](docs\images\screenshots\mp-fraim-page.png "Optimization page")




**Multi-Perspective**

You can find the source code of the backend of FrAIm [here](https://github.com/pwittlinger/data-framed-autonomy/blob/resource-alloc/pddl_gen-1.0-SNAPSHOT-launcher.jar) (opens GitHub). The multi-perspective framed autonomy tool implements the automata-theoretic techniques described by Acitelli et al.

For your convenience, the jar is included in this project under the folder "dependencies".



## Building with Maven (Data-Aware page)

The planner (ENHSP) and the PDDL generator are fetched and built by Maven, nothing has to be installed by hand:

```
cd ../data-framed-autonomy/pddl_gen && mvn initialize && mvn install   # the generator (initialize installs the ProM jars)
cd ../../framedinterface/planner && mvn install                       # ENHSP, downloaded and built from a fixed commit
cd .. && mvn package                                                   # FrAIm; copies enhsp.jar and the generator into dependencies/
java -jar target/framedinterface-1.0-launcher.jar                      # run it from this folder
```

Solved problems are cached in `~/.fraim/plan-cache`, so planning the same prefix on the same model again is instant.
Delete that folder to clear the cache.

## Good to know
- Multi-perspective process frames (including either data- or temporal guards) are solved using the Expressive Numeric Heuristic Search Planner [ENHSP](https://sites.google.com/view/enhsp/), with A* and a fully blind heuristic. You can change the heuristic in the settings.
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
