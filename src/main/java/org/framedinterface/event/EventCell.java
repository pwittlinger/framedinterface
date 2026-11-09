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

/**
 * A row of the Prefix and Continuation lists: one event of the trace, or the trace's start or end. It shows the event's activity, the plan
 * action that replayed it, and how many constraints of the visualized Declare model are satisfied and violated at that point, as
 * {@code SAT (POSS_SAT)} and {@code VIOL (POSS_VIOL)}. Its layout comes from {@code EventCell.fxml}.
 * <p>
 * For the events of an editable prefix (no plan present), the cell can also offer a button that opens a popup to edit the event's
 * attribute values and timestamp, and a button that deletes the event. Each is only offered if the list supports it, i.e. if the
 * corresponding callbacks were given to the constructor.
 *
 * @see EventData
 */
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
	private static final Pattern TIMESTAMP_SHAPE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$");

	private int eventNumber;
	private Consumer<Integer> selectionCallback;
	private Function<String, Set<String>> activityAttributesLookup; //Attribute names bound to a given activity; null if this list doesn't support attribute editing (e.g. Data-Agnostic)
	private Consumer<Integer> deleteCallback; //Removes this event from the prefix; null if this list doesn't support deleting events (e.g. Data-Agnostic)
	private Consumer<Integer> attributeChangeCallback; //Re-replays the trace after an attribute value is confirmed; null if this list doesn't support attribute editing (e.g. Data-Agnostic)
	private BooleanSupplier timestampFieldVisible; //Whether any selected Declare model has a time condition; null if this list doesn't support attribute editing (e.g. Data-Agnostic)
	private FXMLLoader loader;
	private Popup attributesPopup;


	/**
	 * Creates a cell that only shows its event, without attribute editing or deletion (e.g. for the Continuation list).
	 *
	 * @param selectionCallback called with the event number when the row is clicked, or with 0 when the click deselects it; may be
	 *        {@code null}
	 */
	public EventCell(Consumer<Integer> selectionCallback) {
		this(selectionCallback, null, null, null, null);
	}

	/**
	 * Creates a cell that can also let the user edit the attribute values of prefix events, and delete them (e.g. for the Prefix list).
	 *
	 * @param selectionCallback called with the event number when the row is clicked, or with 0 when the click deselects it; may be
	 *        {@code null}
	 * @param activityAttributesLookup the names of the attributes bound to an activity, or {@code null} if the list doesn't support
	 *        attribute editing
	 * @param deleteCallback removes the event with the given number from the prefix, or {@code null} if the list doesn't support deleting
	 *        events
	 * @param attributeChangeCallback called with the event number once an attribute value is confirmed, e.g. to replay the trace again; may
	 *        be {@code null}
	 * @param timestampFieldVisible whether the attribute popup offers a timestamp field (when any selected Declare model has a time
	 *        condition), or {@code null} if the list doesn't support attribute editing
	 */
	public EventCell(Consumer<Integer> selectionCallback, Function<String, Set<String>> activityAttributesLookup, Consumer<Integer> deleteCallback, Consumer<Integer> attributeChangeCallback, BooleanSupplier timestampFieldVisible) {
		this.selectionCallback = selectionCallback;
		this.activityAttributesLookup = activityAttributesLookup;
		this.deleteCallback = deleteCallback;
		this.attributeChangeCallback = attributeChangeCallback;
		this.timestampFieldVisible = timestampFieldVisible;
	}

	/**
	 * Called once the cell's FXML has been loaded: makes clicking on the row report its event number to the selection callback, or 0 if
	 * the click deselected the row.
	 */
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

	/**
	 * Shows the given event, or nothing for an empty row. Any open attribute popup is closed, as the cell may now show a different event.
	 * The start and end of the trace are shown as "-trace start-" and "-trace end-", in italics. The attribute and delete buttons are only
	 * shown for the events of an editable prefix, and only if the list supports them; the attribute button only if the event's activity
	 * has bound attributes or a timestamp field is offered.
	 *
	 * @param item the event, or {@code null} for an empty row
	 * @param empty whether the row is empty
	 */
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

	/**
	 * Handles the delete button: closes the attribute popup and removes the event from the prefix, through the delete callback.
	 */
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

	/**
	 * Handles the attribute button: opens the attribute popup (see {@link #showAttributesPopup(EventData)}), or closes it if it's open.
	 */
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

	/**
	 * Shows a small popup to the left of the attribute button, with one editable field per attribute bound to the event's activity, plus a
	 * timestamp field when any selected Declare model has a time condition. A value is confirmed with its check button or Enter: it's then
	 * stored in the event's attribute values, and the attribute change callback is called. The values aren't checked against the
	 * attributes' declared domains; only the timestamp is checked (see {@link #validateTimestamp(String)}).
	 *
	 * @param item the event
	 */
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

	/**
	 * Builds the popup's timestamp field. Unlike the other attributes, the timestamp isn't bound to the event's activity: it's offered on
	 * every prefix event whenever any selected Declare model has a time condition, since any event may need to take part in a time
	 * condition's evaluation. The field is pre-filled with the event's timestamp, or a suggested one (see {@link #suggestTimestamp()}). A
	 * timestamp that can't be used is rejected, with a message below the field saying why (see {@link #validateTimestamp(String)}).
	 *
	 * @param item the event
	 * @param values the event's attribute values, which a confirmed timestamp is stored in, exactly as entered
	 * @return the field, with its label, confirm button and error message
	 */
	private VBox buildTimestampRow(EventData item, Map<String, String> values) {
		HBox row = new HBox(6);
		row.setAlignment(Pos.CENTER_LEFT);
		TextField valueField = new TextField(values.getOrDefault(TIMESTAMP_ATTRIBUTE, suggestTimestamp()));
		valueField.setPromptText(TIMESTAMP_PATTERN);
		Button confirmButton = new Button();
		confirmButton.getStyleClass().add("small-button");
		FontIcon confirmIcon = new FontIcon("fa-check");
		confirmIcon.getStyleClass().add("small-button__icon");
		confirmButton.setGraphic(confirmIcon);
		Label errorLabel = new Label(); //Says why the entered timestamp was rejected
		errorLabel.setStyle("-fx-text-fill: red;");
		errorLabel.setWrapText(true);
		errorLabel.maxWidthProperty().bind(row.widthProperty());
		errorLabel.setVisible(false);
		errorLabel.setManaged(false);
		Runnable confirmValue = () -> {
			String text = valueField.getText().strip();
			String error = validateTimestamp(text);
			if (error == null) {
				valueField.setStyle("");
				values.put(TIMESTAMP_ATTRIBUTE, text);
				if (attributeChangeCallback != null) {
					attributeChangeCallback.accept(item.getEventNumber());
				}
			} else {
				valueField.setStyle("-fx-border-color: red; -fx-border-width: 1.5px;");
			}
			errorLabel.setText(error);
			errorLabel.setVisible(error != null);
			errorLabel.setManaged(error != null);
		};
		confirmButton.setOnAction(event -> confirmValue.run());
		valueField.setOnAction(event -> confirmValue.run()); //Enter key also confirms
		row.getChildren().addAll(new Label(TIMESTAMP_ATTRIBUTE + ":"), valueField, confirmButton);
		return new VBox(4, row, errorLabel);
	}

	/**
	 * Suggests a timestamp for an event that has none yet; the Data-Aware page gives every prefix event one as it's added, so this is
	 * rarely needed. It's now, or the preceding event's timestamp if that's later, since an event can't happen before the one preceding it.
	 *
	 * @return the suggested timestamp, in {@link #TIMESTAMP_PATTERN}
	 */
	private String suggestTimestamp() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime previous = getNeighborTimestamp(-1);
		return (previous != null && previous.isAfter(now) ? previous : now).format(TIMESTAMP_FORMATTER);
	}

	/**
	 * Checks whether the given timestamp can be used for this event: it must be a date and time in {@link #TIMESTAMP_PATTERN} (see
	 * {@link #parseTimestamp(String)}), and not before the event directly preceding this one, nor after the one directly following it.
	 *
	 * @param text the timestamp as entered
	 * @return why the timestamp can't be used, or {@code null} if it can
	 */
	private String validateTimestamp(String text) {
		LocalDateTime timestamp = parseTimestamp(text);
		if (timestamp == null) {
			return "Not a valid timestamp (" + TIMESTAMP_PATTERN.replace("'", "") + ")";
		}
		LocalDateTime previous = getNeighborTimestamp(-1);
		if (previous != null && timestamp.isBefore(previous)) {
			return "Can't be before the previous event (" + previous.format(TIMESTAMP_FORMATTER) + ")";
		}
		LocalDateTime next = getNeighborTimestamp(1);
		if (next != null && timestamp.isAfter(next)) {
			return "Can't be after the next event (" + next.format(TIMESTAMP_FORMATTER) + ")";
		}
		return null;
	}

	/**
	 * Parses a timestamp in {@link #TIMESTAMP_PATTERN}: a rudimentary format check followed by an actual parse. The parse uses the
	 * formatter's default (smart) resolving, so a day that doesn't exist in its month isn't rejected but moved to the month's last day
	 * (e.g. {@code 2023-02-30T10:00:00} is read as February 28), and {@code 24:00:00} is read as midnight of the next day.
	 *
	 * @param text the timestamp, may be {@code null}
	 * @return the timestamp, or {@code null} if it doesn't have the right format
	 */
	private static LocalDateTime parseTimestamp(String text) {
		if (text == null || !TIMESTAMP_SHAPE_PATTERN.matcher(text).matches()) {
			return null;
		}
		try {
			return LocalDateTime.parse(text, TIMESTAMP_FORMATTER);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Returns the timestamp of the event the given number of rows away from this one.
	 *
	 * @param offset the number of rows, e.g. -1 for the preceding event
	 * @return the timestamp, or {@code null} if that row isn't a prefix event (e.g. the trace start or end) or has no valid timestamp
	 */
	private LocalDateTime getNeighborTimestamp(int offset) {
		int index = getIndex() + offset;
		if (getListView() == null || index < 0 || index >= getListView().getItems().size()) {
			return null;
		}
		Map<String, String> neighborValues = getListView().getItems().get(index).getAttributeValues();
		return neighborValues == null ? null : parseTimestamp(neighborValues.get(TIMESTAMP_ATTRIBUTE));
	}


	/**
	 * Loads the cell's layout from {@code EventCell.fxml}, with this cell as its controller, the first time it's needed.
	 *
	 * @return {@code true} if the layout is loaded, {@code false} if loading it failed. Only the first call tries to load it: if that
	 *         failed, later calls return {@code true} anyway
	 */
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
