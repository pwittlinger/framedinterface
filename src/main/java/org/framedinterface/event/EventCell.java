package org.framedinterface.event;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import org.framedinterface.utils.enums.MonitoringState;

public class EventCell extends ListCell<EventData> {

	@FXML
	private VBox rootRegion;
	
	@FXML
	private Label activityNameLabel;
	@FXML
	private Label satisfiedLabel;
	@FXML
	private Label violatedLabel;
	
	private int eventNumber;
	private Consumer<Integer> selectionCallback;
	private FXMLLoader loader;
	
	
	public EventCell(Consumer<Integer> selectionCallback) {
		this.selectionCallback = selectionCallback;
	}
	
	@FXML
	private void initialize() {
		if (selectionCallback != null) {
			this.setOnMouseClicked(event -> {
				if (this.isSelected()) {
					selectionCallback.accept(eventNumber);
				} else {
					selectionCallback.accept(0);
				}
			});
		}
	}
	
	@Override
	protected void updateItem(EventData item, boolean empty) {
		//https://openjfx.io/javadoc/11/javafx.controls/javafx/scene/control/Cell.html#updateItem(T,boolean)
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		} else {
			if (loadFxml()) {
				eventNumber = item.getEventNumber();
				//TODO: Styling for start, end and normal events
				if (item.isStart()) {
					activityNameLabel.setText("-trace start-");
					activityNameLabel.getStyleClass().add("event-title__artificial");
				} else if (item.isEnd()) {
					activityNameLabel.setText("-trace end-");
					activityNameLabel.getStyleClass().add("event-title__artificial");
				} else {
					activityNameLabel.setText(item.getActivityName());
					activityNameLabel.getStyleClass().remove("event-title__artificial");
				}
				satisfiedLabel.setText(item.getDeclMonitoringStateCounts().get(MonitoringState.SAT) + " (" + item.getDeclMonitoringStateCounts().get(MonitoringState.POSS_SAT) + ")");
				violatedLabel.setText(item.getDeclMonitoringStateCounts().get(MonitoringState.VIOL) + " (" + item.getDeclMonitoringStateCounts().get(MonitoringState.POSS_VIOL) + ")");
				
				setText(null);
				setGraphic(rootRegion);
			}
		}
	}
	
	
	private boolean loadFxml() {
		if (loader == null) {
			//Load ActionCell contents if not already loaded
			loader = new FXMLLoader(getClass().getClassLoader().getResource("EventCell.fxml"));
			loader.setController(this);
			try {
				loader.load();
				return true;
			} catch (IOException | IllegalStateException e) {
				return false;
			}
		} else {
			return true;
		}
	}
}
