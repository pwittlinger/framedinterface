package org.framedinterface.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.framedinterface.model.AbstractModel;
import org.framedinterface.utils.enums.MonitoringState;

/**
 * Controller of a trace monitoring view, with one row each for: the events (with their attributes and an optional recommendation), the
 * monitoring state of each process model, the global monitoring state, the current cost, the best cost, and debug messages. Each
 * {@code add...} method appends one entry to its row, e.g. once per event.
 * <p>
 * This class isn't used anywhere in FrAIm: no FXML file uses it as its controller or defines the elements it expects, and none of the
 * CSS classes it styles its entries with ({@code monitor-tile}, {@code monitor-cost}, ...) are defined. It has been unchanged since the
 * project's first commit.
 */
public class TraceVisualisationController {

	@FXML
	private HBox costCurrHbox;
	@FXML
	private HBox costBestHbox;
	@FXML
	private HBox globalStateHbox;
	@FXML
	private VBox modelsMonitoringVbox;
	@FXML
	private HBox eventLabelHbox;
	@FXML
	private VBox debugVbox;

	private MonitoringState globalState;
	private Map<AbstractModel, MonitoringState> modelStates = new HashMap<AbstractModel, MonitoringState>();
	private Map<AbstractModel, HBox> modelVisualisations = new HashMap<AbstractModel, HBox>();

	/**
	 * Empties all rows once the FXML has been loaded, removing any content the FXML puts there.
	 */
	@FXML
	private void initialize() {
		costCurrHbox.getChildren().clear();
		costBestHbox.getChildren().clear();
		globalStateHbox.getChildren().clear();
		modelsMonitoringVbox.getChildren().clear();
		eventLabelHbox.getChildren().clear();
		debugVbox.getChildren().clear();
	}
	
	/**
	 * Adds a row for each of the given process models, headed by the model's name. {@link #addModelState(AbstractModel, MonitoringState)}
	 * appends to these rows.
	 *
	 * @param processModels the models to monitor
	 */
	public void setModels(List<AbstractModel> processModels) {
		for (AbstractModel processModel : processModels) {
			Label modelNameLabel = new Label(processModel.getModelName());
			modelNameLabel.getStyleClass().add("monitor-label");
			modelsMonitoringVbox.getChildren().add(modelNameLabel);
			modelStates.put(processModel, null);
			HBox modelStateHbox = new HBox();
			modelsMonitoringVbox.getChildren().add(modelStateHbox);
			modelVisualisations.put(processModel, modelStateHbox);
		}
	}
	
	/**
	 * Appends a tile for the given monitoring state to the model's row, styled by the state. The state's name is only written on the tile
	 * when it differs from the model's previous state, so a run of equal states shows as one labelled tile followed by blank ones. Tiles for
	 * the {@link MonitoringState#INIT} and {@link MonitoringState#CONFLICT} states get no state style.
	 *
	 * @param processModel the model; must have been passed to {@link #setModels(List)}
	 * @param modelState the model's monitoring state
	 */
	public void addModelState(AbstractModel processModel, MonitoringState modelState) {
		Label monitoringTile = new Label();
		monitoringTile.getStyleClass().add("monitor-tile");
		switch (modelState) {
		case POSS_SAT:
			monitoringTile.getStyleClass().add("monitor-tile__poss-sat");
			break;
		case POSS_VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__poss-viol");
			break;
		case SAT:
			monitoringTile.getStyleClass().add("monitor-tile__perm-sat");
			break;
		case VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__perm-viol");
			break;
		default:
			break;
		}

		if (modelState != modelStates.get(processModel)) {
			monitoringTile.setText(modelState.getMobuconltlName());
			modelStates.put(processModel, modelState);
		}
		modelVisualisations.get(processModel).getChildren().add(monitoringTile);
	}

	/**
	 * Appends a value to the current cost row.
	 *
	 * @param costCurrValue the current cost
	 */
	public void addCostCurrValue(Integer costCurrValue) {
		Label costCurrLabel = new Label(costCurrValue.toString());
		costCurrLabel.getStyleClass().add("monitor-cost");
		costCurrHbox.getChildren().add(costCurrLabel);
	}

	/**
	 * Appends a value to the best cost row.
	 *
	 * @param costBestValue the best cost
	 */
	public void addCostBestValue(Integer costBestValue) {
		Label costCurrLabel = new Label(costBestValue.toString());
		costCurrLabel.getStyleClass().add("monitor-cost");
		costBestHbox.getChildren().add(costCurrLabel);
	}

	/**
	 * Appends a tile for the given global monitoring state to the global state row, in the same way as
	 * {@link #addModelState(AbstractModel, MonitoringState)} does for a model.
	 *
	 * @param globalState the global monitoring state
	 */
	public void addGlobalState(MonitoringState globalState) {
		Label monitoringTile = new Label();
		
		monitoringTile.getStyleClass().add("monitor-tile");
		switch (globalState) {
		case POSS_SAT:
			monitoringTile.getStyleClass().add("monitor-tile__poss-sat");
			break;
		case POSS_VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__poss-viol");
			break;
		case SAT:
			monitoringTile.getStyleClass().add("monitor-tile__perm-sat");
			break;
		case VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__perm-viol");
			break;
		default:
			break;
		}

		if (globalState != this.globalState) {
			monitoringTile.setText(globalState.getMobuconltlName());
			this.globalState = globalState;
		}
		globalStateHbox.getChildren().add(monitoringTile);
	}
	
	/**
	 * Appends an event to the event row. An event with attributes, e.g. {@code A [x=1]}, is shown as its name with the attributes
	 * ({@code [x=1]}) on a second line. If a recommendation is given, it's shown in a tooltip while the mouse is over the event's name.
	 *
	 * @param eventString the event, optionally followed by its attributes in brackets
	 * @param recommendationString the recommendation for the event, or {@code null} for none
	 */
	public void addEventLabel(String eventString, String recommendationString) {
		VBox eventLabelVbox = new VBox();
		eventLabelVbox.getStyleClass().add("monitor-event-vbox");
		Label eventLabel = new Label();
		
		if (eventString.contains(" [") && eventString.contains("=") && eventString.contains("]")) {
			int attStartIndex = eventString.indexOf(" [");
			
			eventLabel.setText(eventString.substring(0, attStartIndex));
			eventLabel.getStyleClass().add("monitor-event-name");
			eventLabelVbox.getChildren().add(eventLabel);
			
			Label attributeLabel = new Label(eventString.substring(attStartIndex+1));
			attributeLabel.getStyleClass().add("monitor-event-attribute");
			eventLabelVbox.getChildren().add(attributeLabel);
		} else {
			eventLabel.setText(eventString);
			eventLabel.getStyleClass().add("monitor-event-name");
			eventLabelVbox.getChildren().add(eventLabel);
		}
		if (recommendationString!=null) {
			Tooltip recommendationTooltip = new Tooltip();
			recommendationTooltip.setShowDelay(Duration.INDEFINITE);
			recommendationTooltip.setHideDelay(Duration.INDEFINITE);
			recommendationTooltip.setText(recommendationString);
			Tooltip.install(eventLabel, recommendationTooltip);
			eventLabel.setOnMouseEntered(event -> {
				recommendationTooltip.show(eventLabel, event.getScreenX()+35, event.getScreenY()+35);
			});
			eventLabel.setOnMouseExited(event -> {
				recommendationTooltip.hide();
			});
		}
		
		Group eventLabelGroup = new Group(eventLabelVbox);
		eventLabelHbox.getChildren().add(eventLabelGroup);
	}

	/**
	 * Appends a debug message, as a new line of the debug area.
	 *
	 * @param message the message
	 */
	public void addDebugMessage (String message) {
		debugVbox.getChildren().add(new Label(message));
	}
}
