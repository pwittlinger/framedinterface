package org.framedinterface.controller.help;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.framedinterface.controller.common.AbstractController;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

//Guided tour of the Data-Aware page: each step highlights one part of a sketch of the page (covering the rest) and explains it
//in a callout, and the highlighted panes of the sketch can be clicked for a short explanation of each.
//The texts are kept high-level on purpose, so that only major changes to the Data-Aware page need to be reflected here.
public class HelpController extends AbstractController {

	private static final PseudoClass CURRENT_PSEUDO_CLASS = PseudoClass.getPseudoClass("current");
	private static final PseudoClass SPOTLIGHT_PSEUDO_CLASS = PseudoClass.getPseudoClass("spotlight");
	private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
	private static final PseudoClass HINT_PSEUDO_CLASS = PseudoClass.getPseudoClass("hint");

	private static final String HOTSPOT_HINT = "Click a highlighted part of the sketch to learn more about it.";

	//One step of the tour: the areas of the sketch it highlights, and the covered area next to them that holds the callout
	private static class Step {
		private final Button button;
		private final String title;
		private final String intro;
		private final List<StackPane> areas;
		private final StackPane calloutArea;
		private final boolean calloutOnLeft; //Whether the callout is to the left of the highlighted areas (pointing right) or to their right (pointing left)

		private Step(Button button, String title, String intro, List<StackPane> areas, StackPane calloutArea, boolean calloutOnLeft) {
			this.button = button;
			this.title = title;
			this.intro = intro;
			this.areas = areas;
			this.calloutArea = calloutArea;
			this.calloutOnLeft = calloutOnLeft;
		}
	}

	//Explanation shown in the callout when a part (hotspot) of the sketch is clicked
	private static class Topic {
		private final String title;
		private final String text;

		private Topic(String title, String text) {
			this.title = title;
			this.text = text;
		}
	}

	@FXML
	private Button step1Button;
	@FXML
	private Button step2Button;
	@FXML
	private Button step3Button;

	@FXML
	private StackPane specificationsArea;
	@FXML
	private StackPane viewersArea;
	@FXML
	private StackPane prefixArea;
	@FXML
	private StackPane timelineArea;

	@FXML
	private Region specificationsHotspot;
	@FXML
	private Region constraintsHotspot;
	@FXML
	private Region activitiesHotspot;
	@FXML
	private Region attributesHotspot;
	@FXML
	private Region declareHotspot;
	@FXML
	private Region petriNetHotspot;
	@FXML
	private Region prefixHeaderHotspot;
	@FXML
	private Region prefixHotspot;
	@FXML
	private Region continuationHotspot;
	@FXML
	private Region plannerHotspot;
	@FXML
	private Region timelineHotspot;

	@FXML
	private HBox callout;
	@FXML
	private Region calloutPointerLeft;
	@FXML
	private Region calloutPointerRight;
	@FXML
	private Label stepCounterLabel;
	@FXML
	private Label stepTitleLabel;
	@FXML
	private Label stepIntroLabel;
	@FXML
	private Label topicTitleLabel;
	@FXML
	private Label topicTextLabel;
	@FXML
	private Button backButton;
	@FXML
	private Button nextButton;

	private List<Step> steps;
	private Map<Node, Topic> topics = new HashMap<Node, Topic>();
	private int currentStepIndex;
	private Node selectedHotspot;

	@FXML
	private void initialize() {
		steps = List.of(
			new Step(step1Button, "Specifications & Costs",
					"The left-hand column sets which models the planner works with, and what each deviation from them costs.",
					List.of(specificationsArea), viewersArea, false),
			new Step(step2Button, "Model Viewers",
					"The two viewers draw a Declare model and a Petri net, colored by the state reached at the current position of the timeline. "
					+ "Clicking an activity or transition adds it to the prefix.",
					List.of(viewersArea), prefixArea, false),
			new Step(step3Button, "Prefix & Continuation",
					"This is where you work with traces:\n"
					+ "1.  Build a prefix by clicking the model viewers, or import one.\n"
					+ "2.  Press Run Planner to find an optimal continuation.\n"
					+ "3.  Replay the result with the timeline at the bottom.",
					List.of(prefixArea, timelineArea), viewersArea, true));

		//Step 1: Specifications & Costs
		topics.put(specificationsHotspot, new Topic("Process Specifications",
				"Lists the models loaded on the Process Frame Overview page. Tick a model to use it here: the panes below and the planner only consider ticked models. "
				+ "Selecting a row shows the model in its viewer."));
		topics.put(constraintsHotspot, new Topic("Constraints",
				"The constraints of the ticked models, grouped by model (a Petri net counts as one constraint). "
				+ "A Hard constraint must never be violated; for all others, Cost is the penalty for violating it. "
				+ "Double-click a cost to change it, or select several rows and use Set Cost... (also in the right-click menu). "
				+ "Hover over a constraint to see its conditions."));
		topics.put(activitiesHotspot, new Topic("Activities",
				"The activities of the ticked models. Cost is paid each time the planner adds the activity to a continuation, and is changed the same way as for constraints. "
				+ "Expand an activity to see its attributes and the models it appears in."));
		topics.put(attributesHotspot, new Topic("Attributes",
				"The data attributes used by the ticked Declare models, with the values they can take. "
				+ "A handy reference when entering attribute values for prefix events."));

		//Step 2: Model Viewers
		topics.put(declareHotspot, new Topic("Declare Model",
				"Pick the Declare model to show and zoom with the slider. Colors show which constraints are satisfied or violated so far; Help shows the color legend. "
				+ "Data conditions also draws the conditions of the constraints. Click an activity to add it to the prefix."));
		topics.put(petriNetHotspot, new Topic("PN Model",
				"Pick the Petri net to show and zoom with the slider. Colors show which transitions fired and which are enabled; Help shows the color legend. "
				+ "Show Past Violations keeps earlier violations visible. Click a transition label to add it to the prefix."));

		//Step 3: Prefix & Continuation
		topics.put(prefixHeaderHotspot, new Topic("Prefix+Continuation",
				"Import loads an XES event log as prefixes, one per trace. Export saves the planner's continuations as an XES log. Help explains the steps of a plan. "
				+ "After planning, Models lists the models that were planned with."));
		topics.put(prefixHotspot, new Topic("Prefix",
				"The events observed so far. Switch between prefixes with the drop-down; + adds a prefix and - removes it. "
				+ "On an event, the trash icon deletes it and + edits its attribute values. "
				+ "Each event shows how many constraints of the shown Declare model are satisfied and violated (temporarily, in brackets). "
				+ "While a plan exists, the prefix is locked; Reset unlocks it."));
		topics.put(continuationHotspot, new Topic("Continuation",
				"The plan found for the selected prefix, replayed instead of the prefix. Each step is marked as sync (a prefix event kept as is), "
				+ "skip (a prefix event no model refers to), add (an event inserted by the planner) or reset (a violated constraint starts over)."));
		topics.put(plannerHotspot, new Topic("Run Planner",
				"Run Planner computes a continuation for every prefix; Plan Cost shows the cost of the selected one. "
				+ "The gear button sets what is optimized, how violated constraints are handled and how the planner searches. "
				+ "Reset discards the selected prefix's plan; pressing it again clears the prefix."));
		topics.put(timelineHotspot, new Topic("Timeline",
				"Step backward or forward, play, or drag the slider to move through the trace. Clicking an event in the Prefix or Continuation list jumps to it. "
				+ "The model viewers always show the state at this position."));

		for (int i = 0; i < steps.size(); i++) {
			steps.get(i).button.setText((i + 1) + "   " + steps.get(i).title);
		}
		showStep(0);
	}

	private void showStep(int stepIndex) {
		Step step = steps.get(stepIndex);
		currentStepIndex = stepIndex;

		//The scrim of every area that isn't highlighted is shown through CSS
		for (Step otherStep : steps) {
			otherStep.button.pseudoClassStateChanged(CURRENT_PSEUDO_CLASS, otherStep == step);
			for (StackPane area : otherStep.areas) {
				area.pseudoClassStateChanged(SPOTLIGHT_PSEUDO_CLASS, step.areas.contains(area));
			}
		}

		//Places the callout on top of the neighboring area's scrim, pointing towards the highlighted areas
		if (callout.getParent() != step.calloutArea) {
			step.calloutArea.getChildren().add(callout); //Also removes it from the area it was in before
		}
		StackPane.setAlignment(callout, step.calloutOnLeft ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
		StackPane.setMargin(callout, new Insets(12, step.calloutOnLeft ? 4 : 12, 12, step.calloutOnLeft ? 12 : 4));
		calloutPointerLeft.setVisible(!step.calloutOnLeft);
		calloutPointerLeft.setManaged(!step.calloutOnLeft);
		calloutPointerRight.setVisible(step.calloutOnLeft);
		calloutPointerRight.setManaged(step.calloutOnLeft);

		stepCounterLabel.setText("STEP " + (stepIndex + 1) + " OF " + steps.size());
		stepTitleLabel.setText(step.title);
		stepIntroLabel.setText(step.intro);
		backButton.setDisable(stepIndex == 0);
		nextButton.setDisable(stepIndex == steps.size() - 1);
		selectHotspot(null);
	}

	//Shows the topic of the given hotspot in the callout, or the hint to click one if null
	private void selectHotspot(Node hotspot) {
		if (selectedHotspot != null) {
			selectedHotspot.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
		}
		selectedHotspot = hotspot;
		if (hotspot != null) {
			hotspot.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, true);
		}

		Topic topic = hotspot == null ? null : topics.get(hotspot);
		topicTitleLabel.setText(topic == null ? "" : topic.title);
		topicTitleLabel.setVisible(topic != null);
		topicTitleLabel.setManaged(topic != null);
		topicTextLabel.setText(topic == null ? HOTSPOT_HINT : topic.text);
		topicTextLabel.pseudoClassStateChanged(HINT_PSEUDO_CLASS, topic == null);
	}

	@FXML
	private void onClickHotspot(MouseEvent event) {
		selectHotspot((Node) event.getSource());
	}

	//Clicking a covered part of the sketch jumps to the step that explains it
	@FXML
	private void onClickScrim(MouseEvent event) {
		Node area = ((Node) event.getSource()).getParent();
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).areas.contains(area)) {
				showStep(i);
				return;
			}
		}
	}

	@FXML
	private void onClickStepButton(ActionEvent event) {
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).button == event.getSource()) {
				showStep(i);
				return;
			}
		}
	}

	@FXML
	private void showPreviousStep() {
		showStep(currentStepIndex - 1);
	}

	@FXML
	private void showNextStep() {
		showStep(currentStepIndex + 1);
	}

}
