# Launching FrAIm

You can launch FrAIm by (i) double clicking on the .jar file (framedinterface-1.0-launcher.jar) or (ii) by running ``java -jar framedinterface-1.0-launcher.jar`` from the root of the directory.

This will launch FrAIm on the overview page, which can be used to upload and inspect process models.
After uploading at least one declarative process model (.decl), you can switch to the `optimization` page.

![Main window](images/screenshots/fraim-overview-page.png)


## Optimization Page

The `optimization` page is the primary functionality of FrAIm. Here you can select multiple process models, build a prefix, and find an optimal continuation for the given prefix.

On the left-hand side of the page, you can see all uploaded process models (Process Specification pane). When you select a model by ticking the checkbox next to the model name, the model will be selected to the process frame. When selecting the model, its contained Constraints and Activities get added to the `Constraints` and `Activities` panes below the `Process Specification` pane.


![Process Specficication pane](images/screenshots/process-specification.png)

![Adding a process specification to the frame adds the constraints and activities to the frame - shown in the Constraints and Activities panes. ](images/screenshots/selected-constraints.png)