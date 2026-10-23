package org.framedinterface.event;

import java.io.IOException;
//import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.framedinterface.utils.enums.MonitoringState;
import org.kordamp.ikonli.javafx.FontIcon;

public class EventCell extends ListCell<EventData> {

	@FXML
	private VBox rootRegion;

	@FXML
	private Label activityNameLabel;
	@FXML
	private Button extendButton;
	@FXML
	private Button deleteButton;
	@FXML
	private Label satisfiedLabel;
	@FXML
	private Label violatedLabel;
	@FXML
	private Label actionLabel;

	//Attribute name used for the per-event timestamp, and the format it must be entered in (also used as the default/pre-filled value)
	private static final String TIMESTAMP_ATTRIBUTE = "timestamp";
	private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);
	private static final Pattern TIMESTAMP_SHAPE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

	private int eventNumber;
	private Consumer<Integer> selectionCallback;
	private Function<String, Set<String>> activityAttributesLookup; //Attribute names bound to a given activity; null if this list doesn't support attribute editing (e.g. Data-Agnostic)
	private Consumer<Integer> deleteCallback; //Removes this event from the prefix; null if this list doesn't support deleting events (e.g. Data-Agnostic)
	private Consumer<Integer> attributeChangeCallback; //Re-replays the trace after an attribute value is confirmed; null if this list doesn't support attribute editing (e.g. Data-Agnostic)
	private BooleanSupplier timestampFieldVisible; //Whether any selected Declare model has a time condition; null if this list doesn't support attribute editing (e.g. Data-Agnostic)
	private FXMLLoader loader;
	private Popup attributesPopup;


	public EventCell(Consumer<Integer> selectionCallback) {
		this(selectionCallback, null, null, null, null);
	}

	public EventCell(Consumer<Integer> selectionCallback, Function<String, Set<String>> activityAttributesLookup, Consumer<Integer> deleteCallback, Consumer<Integer> attributeChangeCallback, BooleanSupplier timestampFieldVisible) {
		this.selectionCallback = selectionCallback;
		this.activityAttributesLookup = activityAttributesLookup;
		this.deleteCallback = deleteCallback;
		this.attributeChangeCallback = attributeChangeCallback;
		this.timestampFieldVisible = timestampFieldVisible;
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
		if (attributesPopup != null) {
			attributesPopup.hide(); //Cell is being recycled for different data - any open attribute editor no longer applies
		}
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
					actionLabel.setVisible(false);
					actionLabel.setManaged(false);
				} else if (item.isEnd()) {
					activityNameLabel.setText("-trace end-");
					activityNameLabel.getStyleClass().add("event-title__artificial");
					actionLabel.setVisible(false);
					actionLabel.setManaged(false);
				} else {
					activityNameLabel.setText(item.getActivityName());
					activityNameLabel.getStyleClass().remove("event-title__artificial");
					actionLabel.setVisible(true);
					actionLabel.setManaged(true);
					actionLabel.setText(item.getPlanAction());
				}
				satisfiedLabel.setText(item.getDeclMonitoringStateCounts().get(MonitoringState.SAT) + " (" + item.getDeclMonitoringStateCounts().get(MonitoringState.POSS_SAT) + ")");
				violatedLabel.setText(item.getDeclMonitoringStateCounts().get(MonitoringState.VIOL) + " (" + item.getDeclMonitoringStateCounts().get(MonitoringState.POSS_VIOL) + ")");

				//Attribute editing and deletion are only offered for real, editable (prefix, no plan present) events
				boolean isPrefixEvent = !item.isStart() && !item.isEnd() && item.getAttributeValues() != null;
				boolean hasBoundAttributes = isPrefixEvent && activityAttributesLookup != null && !activityAttributesLookup.apply(item.getActivityName()).isEmpty();
				boolean showTimestampField = isPrefixEvent && timestampFieldVisible != null && timestampFieldVisible.getAsBoolean();
				boolean hasAttributes = hasBoundAttributes || showTimestampField;
				extendButton.setVisible(hasAttributes);
				extendButton.setManaged(hasAttributes);
				boolean canDelete = isPrefixEvent && deleteCallback != null;
				deleteButton.setVisible(canDelete);
				deleteButton.setManaged(canDelete);

				setText(null);
				setGraphic(rootRegion);
			}
		}
	}

	@FXML
	private void onClickDelete() {
		EventData item = getItem();
		if (item != null && deleteCallback != null) {
			if (attributesPopup != null) {
				attributesPopup.hide();
			}
			deleteCallback.accept(item.getEventNumber());
		}
	}

	@FXML
	private void onClickExtend() {
		EventData item = getItem();
		if (item == null) {
			return;
		}
		if (attributesPopup != null && attributesPopup.isShowing()) {
			attributesPopup.hide();
			return;
		}
		showAttributesPopup(item);
	}

	//Shows a small popup, to the side of the row, with one editable field per attribute bound to the event's activity
	//(plus a "timestamp" field when any selected Declare model has a time condition)
	private void showAttributesPopup(EventData item) {
		Set<String> attributes = activityAttributesLookup == null ? Set.of() : activityAttributesLookup.apply(item.getActivityName());
		Map<String, String> values = item.getAttributeValues();

		VBox box = new VBox(6);
		box.getStyleClass().add("event-attributes-popup");
		box.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");
		box.getChildren().add(new Label("Attributes for " + item.getActivityName()));
		for (String attribute : attributes) {
			HBox row = new HBox(6);
			row.setAlignment(Pos.CENTER_LEFT);
			TextField valueField = new TextField(values.getOrDefault(attribute, ""));
			valueField.setPromptText("value");
			Button confirmButton = new Button();
			confirmButton.getStyleClass().add("small-button");
			FontIcon confirmIcon = new FontIcon("fa-check");
			confirmIcon.getStyleClass().add("small-button__icon");
			confirmButton.setGraphic(confirmIcon);
			Runnable confirmValue = () -> {
				values.put(attribute, valueField.getText());
				if (attributeChangeCallback != null) {
					attributeChangeCallback.accept(item.getEventNumber());
				}
			};
			confirmButton.setOnAction(event -> confirmValue.run());
			valueField.setOnAction(event -> confirmValue.run()); //Enter key also confirms
			row.getChildren().addAll(new Label(attribute + ":"), valueField, confirmButton);
			box.getChildren().add(row);
		}

		if (timestampFieldVisible != null && timestampFieldVisible.getAsBoolean()) {
			box.getChildren().add(buildTimestampRow(item, values));
		}

		attributesPopup = new Popup();
		attributesPopup.setAutoHide(true);
		attributesPopup.getContent().add(box);

		Bounds screenBounds = extendButton.localToScreen(extendButton.getBoundsInLocal());
		attributesPopup.show(extendButton, screenBounds.getMaxX(), screenBounds.getMinY());
		attributesPopup.setX(screenBounds.getMinX() - attributesPopup.getWidth() - 10); //Reposition to the left now that the popup's actual width is known
	}

	//The "timestamp" field is not bound to the event's activity (unlike the other attributes above) - it is offered on every prefix event
	//whenever any selected Declare model has a time condition, since any event may need to participate in a time condition's evaluation
	private HBox buildTimestampRow(EventData item, Map<String, String> values) {
		HBox row = new HBox(6);
		row.setAlignment(Pos.CENTER_LEFT);
		String defaultValue = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
		TextField valueField = new TextField(values.getOrDefault(TIMESTAMP_ATTRIBUTE, defaultValue));
		valueField.setPromptText(TIMESTAMP_PATTERN);
		Button confirmButton = new Button();
		confirmButton.getStyleClass().add("small-button");
		FontIcon confirmIcon = new FontIcon("fa-check");
		confirmIcon.getStyleClass().add("small-button__icon");
		confirmButton.setGraphic(confirmIcon);
		Runnable confirmValue = () -> {
			String text = valueField.getText();
			if (isValidTimestamp(text)) {
				valueField.setStyle("");
				values.put(TIMESTAMP_ATTRIBUTE, text);
				if (attributeChangeCallback != null) {
					attributeChangeCallback.accept(item.getEventNumber());
				}
			} else {
				valueField.setStyle("-fx-border-color: red; -fx-border-width: 1.5px;");
			}
		};
		confirmButton.setOnAction(event -> confirmValue.run());
		valueField.setOnAction(event -> confirmValue.run()); //Enter key also confirms
		row.getChildren().addAll(new Label(TIMESTAMP_ATTRIBUTE + ":"), valueField, confirmButton);
		return row;
	}

	//Rudimentary format check followed by an actual parse, so e.g. "2023-02-30T10:00:00.000" (not a real date) is also rejected
	private static boolean isValidTimestamp(String text) {
		if (text == null || !TIMESTAMP_SHAPE_PATTERN.matcher(text).matches()) {
			return false;
		}
		try {
			LocalDateTime.parse(text, TIMESTAMP_FORMATTER);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}


	private boolean loadFxml() {
		if (loader == null) {
			//Load EventCell contents if not already loaded
			loader = new FXMLLoader(getClass().getResource("/org/framedinterface/EventCell.fxml"));
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
