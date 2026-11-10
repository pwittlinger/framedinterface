## Prerequisite

Before starting with FrAIm, please ensure that you have Java 21 installed on your machine.
You can download it [here](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html).


Furthermore, you need to install the [Expressive Numeric Heuristic Search Planner](https://sites.google.com/view/enhsp/) (ENHSP), which fortunately also is java based.
FrAIm provides two ways to simplify the installation.

## Interacting with the Optimization Page

1. Upload a process frame (consisting of at least one .decl file)
2. Navigate to the `Optimization' page
3. Select a process model in the `Process Specification' Table (top left)
4. (Optional) Change the Constraint Violation Cost, or modify a selected constraint to be hard
5. (Optional) Provide a prefix by clicking on the Activities in the process model viewers.
6. Execute the "Run Planner" button to get the optimal suffix.

![Optimization page](images/screenshots/mp-fraim-page.png)