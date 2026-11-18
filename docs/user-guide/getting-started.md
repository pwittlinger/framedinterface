## Prerequisite

Before starting with FrAIm, please ensure that you have Java 21 installed on your machine.
You can download it [here](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html).
This should come with the necessary JavaFX libraries for both Linux or Windows.


Furthermore, you need to install the [Expressive Numeric Heuristic Search Planner](https://sites.google.com/view/enhsp/) (ENHSP), which fortunately also is java based.
FrAIm provides two ways to simplify the installation, either with `maven` o

## Interacting with the Optimization Page

Before you do an analysis in the Optimization page, you need to have uploaded at least one DECLARE specification. FrAIm accepts process frames 1...n DECLARE specifications and 0...n Petri nets.

The general workflow of interaction looks like this:
1. Select at least one process model in the `Process Specification' Table (top left)
2. (Optional) Change the Constraint Violation Cost, or modify a selected constraint to be hard
5. (Optional) Provide a prefix by clicking on the Activities in the process model viewers.
6. Execute the "Run Planner" button to get the optimal suffix.

![Optimization page](images/screenshots/mp-fraim-page.png)