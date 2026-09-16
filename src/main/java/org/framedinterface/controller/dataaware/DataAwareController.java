package org.framedinterface.controller.dataaware;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.framedinterface.controller.common.AbstractController;
import org.framedinterface.event.EventCell;
import org.framedinterface.event.EventData;
import org.framedinterface.model.AbstractModel;
import org.framedinterface.model.AttributeDomain;
import org.framedinterface.model.DeclareModel;
import org.framedinterface.model.ModelRegistry;
import org.framedinterface.model.ModelType;
import org.framedinterface.model.PlannerSession;
import org.framedinterface.model.PnModel;
import org.framedinterface.utils.FileUtils;
import org.framedinterface.utils.ValidationUtils;
import org.framedinterface.utils.enums.MonitoringState;
import org.controlsfx.control.ToggleSwitch;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.kordamp.ikonli.javafx.FontIcon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;

import java.util.function.Consumer;

public class DataAwareController extends AbstractController {

	@FXML
	private TableView<AbstractModel> modelTabelView;
	@FXML
	private TableColumn<AbstractModel, Boolean> modelSelectColumn;
	@FXML
	private TableColumn<AbstractModel, String> modelNameColumn;
	@FXML
	private TableColumn<AbstractModel, String> modelTypeColumn;

	//Tracks which process specifications are checked in modelTabelView, to later be used for populating the data-aware planner
	private Map<AbstractModel, BooleanProperty> modelSelectionState = new HashMap<AbstractModel, BooleanProperty>();

	@FXML
	private TitledPane modelRegistryPane;
	@FXML
	private TreeView<String> activitiesTreeView;
	@FXML
	private TitledPane activitiesAttributesPane;
	@FXML
	private TreeView<String> attributesTreeView;
	@FXML
	private TitledPane attributesPane;

	//Unique (case-insensitive) activity labels, and their bound attributes (Declare models only), of the currently selected process specifications; refreshed by updateActivitiesListView()
	private Map<String, Set<String>> activityToAttributes = new TreeMap<String, Set<String>>();

	@FXML
	private SplitPane resultsSplitPane;

	@FXML
	private ChoiceBox<AbstractModel> declModelChoice;
	@FXML
	private WebView declWebView;
	@FXML
	private Slider declZoomSlider;
	@FXML
	private TextField declZoomValueField;
	@FXML
	private Button toolTipButton;
	@FXML
	private ToggleSwitch dataConditionsToggle;

	@FXML
	private ChoiceBox<AbstractModel> pnModelChoice;
	@FXML
	private WebView pnWebView;
	@FXML
	private Slider pnZoomSlider;
	@FXML
	private TextField pnZoomValueField;
	@FXML
	private Label labelFinalMarking;
	@FXML
	private CheckBox bttnDisplayViolations;
	@FXML
	private Button toolTipButtonPN;

	@FXML
	private Label prefixOnlyLabel;
	@FXML
	private Label selectedDecl;
	@FXML
	private Label selectedPN;
	@FXML
	private TitledPane prefixPane;
	@FXML
	private ChoiceBox<PrefixEntry> prefixChoice;
	@FXML
	private Button addPrefixButton;
	@FXML
	private Button removePrefixButton;
	@FXML
	private ListView<EventData> planListView;
	@FXML
	private TitledPane continuationPane;
	@FXML
	private Label labelCost;
	@FXML
	private Button buttonPrefix;
	@FXML
	private Button importPrefixButton;
	@FXML
	private Button toolTipButtonPlan;

	@FXML
	private HBox timelineControls;
	@FXML
	private Button stepBackwardButton;
	@FXML
	private Button playPauseButton;
	@FXML
	private FontIcon playFonticon;
	@FXML
	private Button stepForwardButton;
	@FXML
	private Label currentEventNumber;
	@FXML
	private Label totalEventsNumber;
	@FXML
	private Slider eventSlider;

	private static String precentageFormat = "%.1f";
	private static final DateTimeFormatter XES_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"); //Matches the "timestamp" attribute format EventCell's editor expects
	private String initialDeclWebViewScript;
	private String initialPnWebViewScript;

	private ObjectProperty<Double> declZoomSliderValueObject;
	private ObjectProperty<Double> pnZoomSliderValueObject;
	private ObjectProperty<Double> declWebViewZoomObject;
	private ObjectProperty<Double> pnWebViewZoomObject;

	private Timeline animationTimeline;
	private boolean animationInProgress;
	private SimpleIntegerProperty currentEventIndex = new SimpleIntegerProperty(0);
	private FontIcon pauseFontIcon = new FontIcon("fa-pause");
	private boolean showDataConditions = false;
	private boolean timestampFieldVisible = false; //Whether any SELECTED Declare model has a constraint with a time condition, exposed to EventCell's attribute editor

	//The manually-built prefixes offered by prefixChoice; only the selected entry's prefix/continuation is actually live in PlannerSession at any time (see syncOutToEntry/syncInFromEntry)
	private ObservableList<PrefixEntry> prefixes = FXCollections.observableArrayList();

	//One prefix's worth of PlannerSession state: its manually-built events, their attribute values, and whatever planner continuation was generated from it
	private static class PrefixEntry {
		private List<String> prefixEvents = new ArrayList<String>();
		private Map<Integer, Map<String, String>> attributeValues = new HashMap<Integer, Map<String, String>>();
		private List<String> plan = new ArrayList<String>();
		private boolean planPresent = false;
	}

	@FXML
	private void initialize() {

		//Model list: view/select only, mirrors Process Frame Overview's table without upload/remove
		modelTabelView.setItems(ModelRegistry.getInstance().getModels());
		modelTabelView.setPlaceholder(new Label("No process specifications selected"));
		modelNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelName()));
		modelTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelType().toString()));
		modelSelectColumn.setCellValueFactory(data -> getModelSelectedProperty(data.getValue()));
		modelSelectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(modelSelectColumn));

		//Activities & Attributes panel: view-only tree of unique activity labels (with their bound attributes, if any) across the currently selected process specifications
		activitiesTreeView.setShowRoot(false);
		attributesTreeView.setShowRoot(false);
		updateActivitiesListView();

		//Collapsed panes should only take up their title bar's height, not keep claiming a share of the column's growable space
		for (TitledPane pane : new TitledPane[] {modelRegistryPane, activitiesAttributesPane, attributesPane, prefixPane, continuationPane}) {
			bindPaneGrowToExpanded(pane);
		}

		//Selecting a row switches the visualized model, same as Process Frame Overview
		modelTabelView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				if (newValue.getModelType() == ModelType.DECLARE) {
					declModelChoice.getSelectionModel().select(newValue);
				} else if (newValue.getModelType() == ModelType.PN) {
					pnModelChoice.getSelectionModel().select(newValue);
				}
			}
		});

		//Enable and disable resultsSplitPane and timelineControls based on if there are input models or not
		//(models may already be loaded by the time this page is first opened, so this can't just default to disabled)
		resultsSplitPane.setDisable(ModelRegistry.getInstance().getModels().isEmpty());
		timelineControls.setDisable(ModelRegistry.getInstance().getModels().isEmpty());
		ModelRegistry.getInstance().getModels().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				resultsSplitPane.setDisable(ModelRegistry.getInstance().getModels().isEmpty());
				timelineControls.setDisable(ModelRegistry.getInstance().getModels().isEmpty());
				updateActivitiesListView();
			}
		});

		//Setting up model ChoiceBoxes
		declModelChoice.setItems(new FilteredList<AbstractModel>(ModelRegistry.getInstance().getModels(), item -> item.getModelType()==ModelType.DECLARE));
		pnModelChoice.setItems(new FilteredList<AbstractModel>(ModelRegistry.getInstance().getModels(), item -> item.getModelType()==ModelType.PN));
		StringConverter<AbstractModel> modelStringConverter = new StringConverter<AbstractModel>() {
			@Override
			public String toString(AbstractModel abstractModel) {
				return abstractModel==null ? "" : abstractModel.getModelName();
			}
			@Override
			public AbstractModel fromString(String string) {
				return null;
			}
		};
		declModelChoice.setConverter(modelStringConverter);
		pnModelChoice.setConverter(modelStringConverter);
		declModelChoice.getItems().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				if (declModelChoice.getSelectionModel().getSelectedIndex()==-1 || !declModelChoice.getItems().contains(declModelChoice.getSelectionModel().getSelectedItem())) {
					declModelChoice.getSelectionModel().selectFirst();
				}
			}
		});
		pnModelChoice.getItems().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				if (pnModelChoice.getSelectionModel().getSelectedIndex()==-1 || !pnModelChoice.getItems().contains(pnModelChoice.getSelectionModel().getSelectedItem())) {
					pnModelChoice.getSelectionModel().selectFirst();
				}
			}
		});

		//Setting up WebViews and zoom functionality
		declZoomSliderValueObject = declZoomSlider.valueProperty().asObject();
		pnZoomSliderValueObject = pnZoomSlider.valueProperty().asObject();
		declWebViewZoomObject = declWebView.zoomProperty().asObject();
		pnWebViewZoomObject = pnWebView.zoomProperty().asObject();
		setupWebView(declWebView, ModelType.DECLARE, declZoomSlider, declZoomSliderValueObject, declZoomValueField, declWebViewZoomObject);

		//Triggering visualization update when the model selection is changed
		declModelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				List<String> activeTrace = PlannerSession.getInstance().getActiveTrace();
				if (!activeTrace.isEmpty()) {
					newValue.resetModel();
					((DeclareModel) newValue).updateMonitoringStatesWithData(activeTrace, buildAttributeValuesPerEvent(activeTrace), PlannerSession.getInstance().isDisplayViolations());
				}
			}
			updateVisualization(declWebView, newValue, ModelType.DECLARE);
			updateplanListViewStatistics(newValue, planListView.getItems());
		});
		pnModelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				PnModel p_ = (PnModel) newValue;
				labelFinalMarking.setText(p_.finalMarking);

				List<String> activeTrace = PlannerSession.getInstance().getActiveTrace();
				if (!activeTrace.isEmpty()) {
					newValue.resetModel();
					newValue.updateMonitoringStates(activeTrace, PlannerSession.getInstance().isDisplayViolations());
				}
			}
			updateVisualization(pnWebView, newValue, ModelType.PN);
			updateplanListViewStatistics(newValue, planListView.getItems());
		});

		dataConditionsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			showDataConditions = newValue;
			updateVisualization(declWebView, declModelChoice.getSelectionModel().getSelectedItem(), ModelType.DECLARE);
		});

		//Timeline setup
		setupTimelineControls();

		//Callback to handle clicking on events in planListView
		Consumer<Integer> selectionCallback = new Consumer<Integer>() {
			@Override
			public void accept(Integer selectedIndex) {
				handleplanListViewSelection(selectedIndex);
			}
		};
		planListView.setOnKeyReleased((event) -> {
			if(event.getCode() == KeyCode.UP || event.getCode() == KeyCode.KP_UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.KP_DOWN) {
				handleplanListViewSelection(planListView.getSelectionModel().selectedIndexProperty().intValue());
			}
		});
		planListView.setCellFactory(value -> new EventCell(selectionCallback, this::getAttributesForActivity, this::deletePrefixEvent, this::onAttributeValueChanged, () -> timestampFieldVisible));

		bttnDisplayViolations.setSelected(PlannerSession.getInstance().isDisplayViolations());

		//Sets up the prefix selector with a single initial entry; this also reflects whatever trace/plan is already active in
		//PlannerSession (e.g. carried over from Data-Agnostic), same as the updateTrace(null) it used to do here directly
		setupPrefixSelector();
	}

	//Wires up prefixChoice to switch which prefix's data (events, attribute values, planner continuation) is live in PlannerSession
	private void setupPrefixSelector() {
		prefixChoice.setItems(prefixes);
		prefixChoice.setConverter(new StringConverter<PrefixEntry>() {
			@Override
			public String toString(PrefixEntry entry) {
				int index = prefixes.indexOf(entry);
				return index == -1 ? "" : "Prefix " + (index + 1);
			}
			@Override
			public PrefixEntry fromString(String string) {
				return null;
			}
		});
		removePrefixButton.disableProperty().bind(Bindings.size(prefixes).lessThanOrEqualTo(1));

		//The first entry picks up whatever PlannerSession already holds, so a trace/plan carried over from Data-Agnostic still shows up
		PrefixEntry initialEntry = new PrefixEntry();
		syncOutToEntry(initialEntry);
		prefixes.add(initialEntry);

		prefixChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldEntry, newEntry) -> {
			if (newEntry == null || newEntry == oldEntry) {
				return; //Transient clear while prefixes are being added/removed; the explicit select() below always follows up with a real selection
			}
			if (oldEntry != null) {
				syncOutToEntry(oldEntry);
			}
			syncInFromEntry(newEntry);
			updateTrace(null);
			updateSelectedModelVisualizations();
		});
		prefixChoice.getSelectionModel().select(initialEntry);
	}

	//Captures whatever prefix/continuation is currently live in PlannerSession into the given (about to be deselected) entry
	private void syncOutToEntry(PrefixEntry entry) {
		entry.prefixEvents = PlannerSession.getInstance().getCurrentPrefix();
		entry.attributeValues = PlannerSession.getInstance().getPrefixAttributeValuesMap();
		entry.plan = PlannerSession.getInstance().getCurrentPlan();
		entry.planPresent = PlannerSession.getInstance().isPlanPresent();
	}

	//Makes the given (newly selected) entry's prefix/continuation the one live in PlannerSession
	private void syncInFromEntry(PrefixEntry entry) {
		PlannerSession.getInstance().setCurrentPrefix(entry.prefixEvents);
		PlannerSession.getInstance().setPrefixAttributeValuesMap(entry.attributeValues);
		PlannerSession.getInstance().setCurrentPlan(entry.plan);
		PlannerSession.getInstance().setPlanPresent(entry.planPresent);
	}

	//Adds a new, empty prefix and selects it, always leaving the previously selected prefix untouched in the list
	@FXML
	private void onClickAddPrefix() {
		PrefixEntry newEntry = new PrefixEntry();
		prefixes.add(newEntry);
		prefixChoice.getSelectionModel().select(newEntry);
	}

	//Removes the currently selected prefix (at least one must always remain) and selects a neighboring one
	@FXML
	private void onClickRemovePrefix() {
		PrefixEntry current = prefixChoice.getSelectionModel().getSelectedItem();
		if (current == null || prefixes.size() <= 1) {
			return;
		}
		int index = prefixes.indexOf(current);
		prefixes.remove(current);
		prefixChoice.getSelectionModel().select(prefixes.get(Math.max(0, index - 1)));
	}

	//Replaces the current set of prefixes with one manually-built prefix per trace of an imported XES log, each carrying the trace's event attribute values
	@FXML
	private void onClickImportPrefix() {
		File file = FileUtils.showXesOpenDialog(getStage());
		if (file == null) {
			return;
		}

		List<PrefixEntry> importedEntries;
		try {
			importedEntries = parseXesTracesAsPrefixes(file);
		} catch (Exception e) {
			System.err.println("Could not parse XES log: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		if (importedEntries.isEmpty()) {
			return;
		}

		prefixes.setAll(importedEntries);
		prefixChoice.getSelectionModel().select(0);
	}

	//One PrefixEntry per trace of the log's first process (each event's activity as a prefix step, its non-concept/lifecycle attributes as that step's attribute values)
	private List<PrefixEntry> parseXesTracesAsPrefixes(File file) throws Exception {
		List<XLog> logs = new XesXmlParser().parse(file);
		List<PrefixEntry> entries = new ArrayList<PrefixEntry>();
		if (logs.isEmpty()) {
			return entries;
		}

		for (XTrace trace : logs.get(0)) {
			PrefixEntry entry = new PrefixEntry();
			for (int i = 0; i < trace.size(); i++) {
				XEvent event = trace.get(i);
				entry.prefixEvents.add(XConceptExtension.instance().extractName(event));

				Map<String, String> eventAttributeValues = new LinkedHashMap<String, String>();
				for (XAttribute attribute : event.getAttributes().values()) {
					String key = attribute.getKey();
					if (key.equals(XConceptExtension.KEY_NAME) || key.equals(XLifecycleExtension.KEY_TRANSITION)) {
						continue;
					} else if (key.equals(XTimeExtension.KEY_TIMESTAMP)) {
						Instant timestamp = XTimeExtension.instance().extractTimestamp(event).toInstant();
						eventAttributeValues.put("timestamp", XES_TIMESTAMP_FORMATTER.format(LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())));
					} else {
						eventAttributeValues.put(key, extractAttributeValueString(attribute));
					}
				}
				if (!eventAttributeValues.isEmpty()) {
					entry.attributeValues.put(i + 1, eventAttributeValues);
				}
			}
			entries.add(entry);
		}
		return entries;
	}

	//Renders a XES attribute's value the same way it would be typed into the attribute editor popup (e.g. "6" rather than "6.0" for a whole-numbered float)
	private static String extractAttributeValueString(XAttribute attribute) {
		if (attribute instanceof XAttributeLiteral) {
			return ((XAttributeLiteral) attribute).getValue();
		} else if (attribute instanceof XAttributeDiscrete) {
			return Long.toString(((XAttributeDiscrete) attribute).getValue());
		} else if (attribute instanceof XAttributeContinuous) {
			double value = ((XAttributeContinuous) attribute).getValue();
			return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
		} else if (attribute instanceof XAttributeBoolean) {
			return Boolean.toString(((XAttributeBoolean) attribute).getValue());
		}
		return attribute.toString();
	}

	//A collapsed TitledPane should only take up its title bar's height; only an expanded pane should compete for the column's leftover growable space
	private void bindPaneGrowToExpanded(TitledPane pane) {
		VBox.setVgrow(pane, pane.isExpanded() ? Priority.ALWAYS : Priority.NEVER);
		pane.expandedProperty().addListener((observable, oldValue, expanded) -> VBox.setVgrow(pane, expanded ? Priority.ALWAYS : Priority.NEVER));
	}

	private BooleanProperty getModelSelectedProperty(AbstractModel model) {
		return modelSelectionState.computeIfAbsent(model, m -> {
			BooleanProperty selected = new SimpleBooleanProperty(false);
			selected.addListener((observable, oldValue, newValue) -> updateActivitiesListView());
			return selected;
		});
	}

	//Refreshes the Activities & Attributes panel with the unique (case-insensitive) activity labels, and their bound attributes (Declare models only), of the currently selected process specifications
	private void updateActivitiesListView() {
		activityToAttributes.clear();
		timestampFieldVisible = false;
		for (AbstractModel model : getSelectedModels()) {
			for (String activity : model.getActivities()) {
				String activityLabel = activity.toLowerCase();
				Set<String> attributes = activityToAttributes.computeIfAbsent(activityLabel, a -> new TreeSet<String>());
				if (model instanceof DeclareModel) {
					for (String attribute : ((DeclareModel) model).getAttributesForActivity(activityLabel)) {
						attributes.add(attribute.toLowerCase());
					}
				}
			}
			if (model instanceof DeclareModel && ((DeclareModel) model).hasTimeConditions()) {
				timestampFieldVisible = true;
			}
		}

		TreeItem<String> activitiesRoot = new TreeItem<String>();
		if (activityToAttributes.isEmpty()) {
			activitiesRoot.getChildren().add(new TreeItem<String>("No process specifications selected")); //TreeView has no built-in placeholder support, unlike ListView/TableView
		}
		for (Map.Entry<String, Set<String>> entry : activityToAttributes.entrySet()) {
			TreeItem<String> activityItem = new TreeItem<String>(entry.getKey());
			activityItem.setExpanded(true);
			for (String attribute : entry.getValue()) {
				activityItem.getChildren().add(new TreeItem<String>(attribute));
			}
			activitiesRoot.getChildren().add(activityItem);
		}
		activitiesTreeView.setRoot(activitiesRoot);

		planListView.refresh(); //Already-rendered prefix events need to re-check attribute availability now that the selected models (and thus known attributes) may have changed
		updateAttributesTreeView();
	}

	//Refreshes the Attributes panel with the declared value range/set of each attribute, merged (widest range / union of values) across the currently selected Declare models
	private void updateAttributesTreeView() {
		Map<String, AttributeDomain> mergedDomains = new TreeMap<String, AttributeDomain>();
		for (AbstractModel model : getSelectedModels()) {
			if (model instanceof DeclareModel) {
				for (Map.Entry<String, AttributeDomain> entry : ((DeclareModel) model).getAttributeDomains().entrySet()) {
					mergedDomains.merge(entry.getKey(), entry.getValue(), AttributeDomain::merge);
				}
			}
		}

		TreeItem<String> attributesRoot = new TreeItem<String>();
		if (mergedDomains.isEmpty()) {
			attributesRoot.getChildren().add(new TreeItem<String>("No process specifications selected"));
		}
		for (Map.Entry<String, AttributeDomain> entry : mergedDomains.entrySet()) {
			TreeItem<String> attributeItem = new TreeItem<String>(entry.getKey());
			attributeItem.setExpanded(true);
			attributeItem.getChildren().add(new TreeItem<String>(entry.getValue().describe()));
			attributesRoot.getChildren().add(attributeItem);
		}
		attributesTreeView.setRoot(attributesRoot);
	}

	//Attribute names bound to the given activity (from the currently selected Declare models), for the planListView attribute editor
	private Set<String> getAttributesForActivity(String activityName) {
		return activityToAttributes.getOrDefault(activityName == null ? "" : activityName.toLowerCase(), Collections.emptySet());
	}

	//Process specifications checked in modelTabelView, to be handed to the data-aware planner
	public List<AbstractModel> getSelectedModels() {
		List<AbstractModel> selectedModels = new ArrayList<AbstractModel>();
		for (AbstractModel model : ModelRegistry.getInstance().getModels()) {
			if (getModelSelectedProperty(model).get()) {
				selectedModels.add(model);
			}
		}
		return selectedModels;
	}

	@FXML
	void onClickPrefix(ActionEvent event) {
		PlannerSession.getInstance().setPlanPresent(false);
		ModelRegistry.getInstance().getModels().forEach(abstractModel -> abstractModel.resetModel());
		PlannerSession.getInstance().getCurrentPrefix().clear();
		PlannerSession.getInstance().clearPrefixAttributeValues();

		updateTrace(null);
		updateSelectedModelVisualizations();
		labelCost.setText("");
	}

	//Removes a single event from the manually-built prefix (only reachable while no plan is present)
	private void deletePrefixEvent(int eventNumber) {
		PlannerSession.getInstance().removePrefixEvent(eventNumber);
		updateTrace(null);
		updateSelectedModelVisualizations();
	}

	//Re-replays the trace after an attribute value is confirmed in planListView's attribute editor popup, since the
	//constraint automata need to be re-run with the new value rather than waiting for the next prefix event to trigger a replay
	private void onAttributeValueChanged(int eventNumber) {
		updateTrace(currentEventIndex.get());
	}

	//Called from JavaScript when the user clicks on a graph element, extends the manually-built prefix
	private void addToTracePrefix(String modelId, String activityEncoding) {
		if (PlannerSession.getInstance().isPlanPresent()) {
			return;
		}

		String activityName = null;
		for (AbstractModel abstractModel : ModelRegistry.getInstance().getModels()) {
			if (abstractModel.getModelId().equals(modelId)) {
				activityName = abstractModel.getActivityByEncoding(activityEncoding);
				break;
			}
		}
		if (activityName != null) {
			PlannerSession.getInstance().getCurrentPrefix().add(activityName);
			updateTrace(PlannerSession.getInstance().getCurrentPrefix().size());
		}
	}

	//Attribute values (attribute name -> value) attached to each event of the given trace, in order; only the manually-built prefix carries
	//attribute values (a planner-produced continuation has none), so those events get an empty map
	private List<Map<String, String>> buildAttributeValuesPerEvent(List<String> activeTrace) {
		boolean editable = !PlannerSession.getInstance().isPlanPresent();
		List<Map<String, String>> attributeValuesPerEvent = new ArrayList<Map<String, String>>();
		for (int i = 0; i < activeTrace.size(); i++) {
			attributeValuesPerEvent.add(editable ? PlannerSession.getInstance().getPrefixAttributeValues(i + 1) : Collections.emptyMap());
		}
		return attributeValuesPerEvent;
	}

	//Resets all models against whatever trace is currently active (plan continuation if present, otherwise the manually-built prefix) and refreshes the plan list / timeline
	//Declare models replay data conditions against attribute values here (Data-Aware page only); Petri net models have no data conditions to consider
	private void updateTrace(Integer selectIndex) {
		List<String> activeTrace = PlannerSession.getInstance().getActiveTrace();
		List<Map<String, String>> attributeValuesPerEvent = buildAttributeValuesPerEvent(activeTrace);

		ModelRegistry.getInstance().getModels().forEach(abstractModel -> abstractModel.resetModel());
		ModelRegistry.getInstance().getModels().forEach(abstractModel -> {
			if (abstractModel instanceof DeclareModel) {
				((DeclareModel) abstractModel).updateMonitoringStatesWithData(activeTrace, attributeValuesPerEvent, PlannerSession.getInstance().isDisplayViolations());
			} else {
				abstractModel.updateMonitoringStates(activeTrace, PlannerSession.getInstance().isDisplayViolations());
			}
		});
		updateplanListView(activeTrace);
		updateTimelineControls(activeTrace);

		if (selectIndex != null) {
			eventSlider.setValue(selectIndex);
			currentEventIndex.setValue(selectIndex);
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
		}
	}

	private void handleplanListViewSelection(int selectedIndex) {
		if (animationInProgress) {
			animationTimeline.stop();
		}
		eventSlider.setValue(selectedIndex);
		currentEventIndex.setValue(selectedIndex);
		animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
		if (animationInProgress) {
			animationTimeline.pause();
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		}
	}

	@FXML
	private void playPause() {
		if (animationInProgress) {
			playPauseButton.setGraphic(playFonticon);
			animationTimeline.pause();
			animationInProgress = false;
		} else {
			playPauseButton.setGraphic(pauseFontIcon);
			animationTimeline.play();
			animationInProgress = true;
		}
	}

	@FXML
	private void stepBackward() {
		if (animationInProgress)
			animationTimeline.stop();

		eventSlider.setValue(eventSlider.getValue() - 1);
		currentEventIndex.setValue((int)eventSlider.getValue());
		animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));

		if (animationInProgress) {
			animationTimeline.pause();
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		}
	}

	@FXML
	private void stepForward() {
		if (animationInProgress)
			animationTimeline.stop();

		eventSlider.setValue(eventSlider.getValue() + 1);
		currentEventIndex.setValue((int)eventSlider.getValue());

		if (eventSlider.getValue() == eventSlider.getMax())
			animationTimeline.jumpTo(new Duration(0));
		else
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));

		if (animationInProgress) {
			animationTimeline.pause();
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		}
	}

	private void setupWebView(WebView visualizationWebView, ModelType modelType, Slider zoomSlider, ObjectProperty<Double> zoomSliderValueObject, TextField zoomValueField, ObjectProperty<Double> webViewZoomObject) {
		ChangeListener<Worker.State> initialLoadListener = new ChangeListener<Worker.State>() {
			@Override
			public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
				if(newValue == Worker.State.SUCCEEDED) {
					((JSObject)visualizationWebView.getEngine().executeScript("window")).setMember("app", DataAwareController.this);
					if (modelType == ModelType.DECLARE) { //Loading pnWebView only after declWebView is already loaded
						setupWebView(pnWebView, ModelType.PN, pnZoomSlider, pnZoomSliderValueObject, pnZoomValueField, pnWebViewZoomObject);
					}
					visualizationWebView.getEngine().getLoadWorker().stateProperty().removeListener(this);
				}
			}
		};

		visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener(initialLoadListener);

		visualizationWebView.getEngine().load((getClass().getClassLoader().getResource("visPage.html")).toString());
		visualizationWebView.setContextMenuEnabled(false);

		if (modelType == ModelType.DECLARE) {
			visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
				if(newValue == Worker.State.SUCCEEDED && initialDeclWebViewScript != null) {
					if (initialDeclWebViewScript.equals("")) {visualizationWebView.getEngine().executeScript("clearModel()");}
					else {visualizationWebView.getEngine().executeScript(initialDeclWebViewScript);}
					initialDeclWebViewScript = null;
				}
			});
		} else if (modelType == ModelType.PN) {
			visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
				if(newValue == Worker.State.SUCCEEDED && initialPnWebViewScript != null) {
					if (initialPnWebViewScript.equals("")) {visualizationWebView.getEngine().executeScript("clearModel()");}
					else {visualizationWebView.getEngine().executeScript(initialPnWebViewScript);}
					initialPnWebViewScript = null;
				}
			});
		}

		visualizationWebView.addEventFilter(ScrollEvent.SCROLL, e -> {
			if (e.isControlDown()) {
				double deltaY = e.getDeltaY();
				if (deltaY > 0) {
					zoomSlider.setValue(zoomSlider.getValue() + 0.1d);
				} else if (deltaY < 0) {
					zoomSlider.setValue(zoomSlider.getValue() - 0.1d);
				}
				e.consume();
			}
		});

		Bindings.bindBidirectional(zoomValueField.textProperty(), zoomSliderValueObject, new StringConverter<Double>() {
			@Override
			public String toString(Double object) {
				return String.format(precentageFormat, object.doubleValue() * 100);
			}
			@Override
			public Double fromString(String string) {
				try {
					double value = Double.parseDouble(string) / 100;
					if (value > zoomSlider.getMax()) {
						return zoomSlider.getMax();
					} else {
						return value;
					}
				} catch (NumberFormatException e) {
					return 1d;
				}
			}
		});

		Bindings.bindBidirectional(zoomSliderValueObject, webViewZoomObject);
		ValidationUtils.addMandatoryPrecentageBehavior(precentageFormat, zoomSlider.getMax() * 100, zoomValueField);
	}

	private void setupTimelineControls() {
		currentEventNumber.textProperty().bind(currentEventIndex.asString());

		eventSlider.setOnMousePressed(event -> {
			if (animationInProgress) {
				animationTimeline.stop();
			}
		});

		eventSlider.setOnMouseReleased(event -> {
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
			if (animationInProgress) {
				animationTimeline.play();
			}
			currentEventIndex.set((int)eventSlider.getValue());
		});

		eventSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (animationTimeline.getStatus() == Status.RUNNING && oldValue.intValue() != newValue.intValue()) {
				currentEventIndex.set(newValue.intValue());
			}

			if (newValue.doubleValue() == 0) {
				stepBackwardButton.setDisable(true);
				stepForwardButton.setDisable(false);
			} else if (newValue.doubleValue() == eventSlider.getMax()) {
				stepBackwardButton.setDisable(false);
				stepForwardButton.setDisable(true);
			} else {
				stepBackwardButton.setDisable(false);
				stepForwardButton.setDisable(false);
			}
		});

		currentEventIndex.addListener((observable, oldValue, newValue) -> {
			updateSelectedModelVisualizations();
			planListView.scrollTo(newValue.intValue());
			planListView.getSelectionModel().clearAndSelect(newValue.intValue());
		});

		stepBackwardButton.setDisable(true);
	}

	//Updates the planListView to match the trace and the currently selected models
	private void updateplanListView(List<String> activities) {

		boolean editable = !PlannerSession.getInstance().isPlanPresent(); //Attribute values can only be attached to a manually-built prefix, not a planner-produced continuation
		List<EventData> eventDataList = new ArrayList<EventData>();
		eventDataList.add(EventData.createStartEvent());
		for (int i = 0; i < activities.size(); i++) {
			String[] planAction = activities.get(i).split(";");
			EventData eventData = planAction.length == 1
					? new EventData(i+1, activities.get(i))
					: new EventData(i+1, planAction[1], planAction[0]);
			if (editable) {
				eventData.setAttributeValues(PlannerSession.getInstance().getPrefixAttributeValues(i+1));
			}
			eventDataList.add(eventData);
		}
		eventDataList.add(EventData.createEndEvent(activities.size()+1));
		updateplanListViewStatistics(declModelChoice.getSelectionModel().getSelectedItem(), eventDataList);
		updateplanListViewStatistics(pnModelChoice.getSelectionModel().getSelectedItem(), eventDataList);

		planListView.getItems().clear();
		planListView.getItems().addAll(eventDataList);
		planListView.getSelectionModel().selectFirst();
	}

	//Updates the statistics shown in the planListView
	private void updateplanListViewStatistics(AbstractModel abstractModel, List<EventData> eventDataList) {
		if ((abstractModel != null) && (abstractModel.getModelType() == ModelType.DECLARE) ) {
			DeclareModel declareModel = (DeclareModel) abstractModel;
			for (int i = 0; i < eventDataList.size(); i++) {
				eventDataList.get(i).setDeclMonitoringStateCounts(declareModel.getMonitoringStateCounts(i));
			}
		}
		else if (abstractModel == null) {
			for (EventData eventData : eventDataList) {
				eventData.setDeclMonitoringStateCounts(Map.of(
						MonitoringState.SAT, 0,
						MonitoringState.POSS_SAT, 0,
						MonitoringState.POSS_VIOL, 0,
						MonitoringState.VIOL, 0
						));
			}
		}
		planListView.refresh();
	}

	//Updates the TimelineControls to match the trace length and creates a matching slider animation
	private void updateTimelineControls(List<String> activities) {
		if (animationInProgress) {
			animationTimeline.stop();
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		}

		eventSlider.setValue(0d);
		currentEventIndex.setValue(0);
		totalEventsNumber.setText(Integer.toString(activities.size()+1));
		eventSlider.setMax(activities.size()+1);

		animationTimeline = new Timeline();
		animationTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(0),
				new KeyValue(eventSlider.valueProperty(), eventSlider.getMin())));
		animationTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(1500 * eventSlider.getMax()),
				new KeyValue(eventSlider.valueProperty(), eventSlider.getMax())));

		animationTimeline.setOnFinished(event -> {
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		});

		currentEventIndex.setValue(0);
	}

	//Convenience method for when both visualizations need to be updated
	private void updateSelectedModelVisualizations() {
		try {
			updateVisualization(declWebView, declModelChoice.getSelectionModel().getSelectedItem(), ModelType.DECLARE);
		} catch (Exception e) {
			// No model selected
		}

		try {
			updateVisualization(pnWebView, pnModelChoice.getSelectionModel().getSelectedItem(), ModelType.PN);
		} catch (Exception e) {
			// No model selected
		}
	}

	private void updateVisualization(WebView visualizationWebView, AbstractModel abstractModel, ModelType modelType) {
		String visualizationString;
		String script;

		if (abstractModel == null) {
			if (modelType == ModelType.DECLARE) {initialDeclWebViewScript = "";}
			if (modelType == ModelType.PN) {initialPnWebViewScript = "";}
			visualizationWebView.getEngine().executeScript("clearModel()");
		} else {
			visualizationString = abstractModel instanceof DeclareModel
					? ((DeclareModel) abstractModel).getVisualisationString(currentEventIndex.get(), PlannerSession.getInstance().isDisplayViolations(), showDataConditions)
					: abstractModel.getVisualisationString(currentEventIndex.get(), PlannerSession.getInstance().isDisplayViolations());
			if (visualizationString != null) {
				script = "setModel('" + visualizationString + "')";
				if (visualizationWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) {
					visualizationWebView.getEngine().executeScript(script);
					if (abstractModel.getModelType() == ModelType.DECLARE) {initialDeclWebViewScript = null;}
					if (abstractModel.getModelType() == ModelType.PN) {initialPnWebViewScript = null;}
				} else {
					if (abstractModel.getModelType() == ModelType.DECLARE) {initialDeclWebViewScript = script;}
					if (abstractModel.getModelType() == ModelType.PN) {initialPnWebViewScript = script;}
				}
			} else {
				if (abstractModel.getModelType() == ModelType.DECLARE) {initialDeclWebViewScript = "";}
				if (abstractModel.getModelType() == ModelType.PN) {initialPnWebViewScript = "";}
				visualizationWebView.getEngine().executeScript("clearModel()");
			}
		}
	}

	//Called from JavaScript, adds click listeners to all graph nodes
	public void addGraphClickHandlers(Object documentObject) {
		if (documentObject instanceof Document) {
			Document document = (Document)documentObject;
			Element rootDiv = ((Document)document).getElementById("rootDiv");
			if (rootDiv != null) {
				for (int i = 0; i < rootDiv.getFirstChild().getChildNodes().getLength(); i++) {
					Node node_i = rootDiv.getFirstChild().getChildNodes().item(i);
					if (node_i.getAttributes() != null && node_i.getAttributes().getNamedItem("id") != null && node_i.getAttributes().getNamedItem("id").getTextContent().startsWith("graphRoot_")) {
						String graphRootId = node_i.getAttributes().getNamedItem("id").getTextContent();
						String modelId = graphRootId.substring(graphRootId.indexOf("_")+1, graphRootId.length());

						for (int j = 0; j < node_i.getChildNodes().getLength(); j++) {
							Node node_j = node_i.getChildNodes().item(j);
							if (node_j.getNodeName().equals("g")) {
								for (int k = 0; k < node_j.getChildNodes().getLength(); k++) {
									Node node_k = node_j.getChildNodes().item(k);
									if (node_k.getNodeName().equals("title")) {
										((EventTarget)node_j).addEventListener("click", ev -> {
											addToTracePrefix(modelId, node_k.getTextContent());
										}, false);
									}
								}
							}
						}
						return; //Assuming there is only one graph
					}
				}
				System.err.println("Cannot find graphRoot");
			} else {
				System.err.println("Cannot find rootDiv element");
			}
		} else {
			System.err.println("documentObject must be instance of Document class");
		}
	}

	@FXML
	private void switchViolationStrings() {
		PlannerSession.getInstance().setDisplayViolations(bttnDisplayViolations.isSelected());
		updateSelectedModelVisualizations();
	}

	@FXML
	private void onClickToolTip() {
		Popup legendPopup = new Popup();

		VBox legendBox = new VBox(10);
		legendBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");

		Label helpT = new Label("To add to the Prefix, click on the Activity name.");
		Label title = new Label("Color Legend:");
		legendBox.getChildren().addAll(
			helpT,
			title,
			createLegendItem(Color.web("#79a888"), "Constraint Temporarily Satisfied"),
			createLegendItem(Color.web("#66ccff"), "Constraint Permanently Satisfied"),
			createLegendItem(Color.web("#ffd700"), "Constraint Temporarily Violated"),
			createLegendItem(Color.web("#d44942"), "Constraint Permanently Violated")
		);

		legendPopup.getContent().add(legendBox);
		legendPopup.setAutoHide(true);

		toolTipButton.setOnAction(e -> {
			if (!legendPopup.isShowing()) {
				Bounds screenBounds = toolTipButton.localToScreen(toolTipButton.getBoundsInLocal());
				legendPopup.show(toolTipButton, screenBounds.getMinX(), screenBounds.getMinY() + toolTipButton.getHeight());
			} else {
				legendPopup.hide();
			}
		});
	}

	private HBox createLegendItem(Color color, String description) {
		Rectangle rect = new Rectangle(20, 20, color);
		rect.setStroke(Color.BLACK);
		rect.setStrokeWidth(1);
		Label label = new Label(description);
		HBox item = new HBox(10, rect, label);
		return item;
	}

	@FXML
	private void onClickToolTipPN() {
		Popup legendPopup = new Popup();

		VBox legendBox = new VBox(10);
		legendBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");

		Label helpT = new Label("To add to the Prefix, click on the Transition label.\n");
		Label title = new Label("Color Legend:");
		legendBox.getChildren().addAll(
			helpT,
			title,
			createLegendItem(Color.web("#66ccff"), "Transition fired"),
			createLegendItem(Color.web("#FFFFFF"), "Transition enabled"),
			createLegendItem(Color.web("#d44942"), "Transition violated (fired but not enabled)"),
			createLegendItem(Color.web("#D3D3D3"), "Transition not enabled")
		);

		legendPopup.getContent().add(legendBox);
		legendPopup.setAutoHide(true);

		toolTipButtonPN.setOnAction(e -> {
			if (!legendPopup.isShowing()) {
				Bounds screenBounds = toolTipButtonPN.localToScreen(toolTipButtonPN.getBoundsInLocal());
				legendPopup.show(toolTipButtonPN, screenBounds.getMinX(), screenBounds.getMinY() + toolTipButtonPN.getHeight());
			} else {
				legendPopup.hide();
			}
		});
	}

	@FXML
	private void onClickToolTipPlan() {
		Popup legendPopup = new Popup();

		VBox legendBox = new VBox(10);
		legendBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");

		legendBox.getChildren().addAll(
			new Label("Click on one of the Elements in the pane below to replay the prefix/trace onto the selected process models."),
			new Label(""),
			new Label("The first line shows the current activity in the plan."),
			new Label("The next line shows the action that was taken by the planner (if a plan was generated on the Data-Agnostic page):"),
			new Label("\t i) \tprefix_sync: Successfully replayed the activity of the prefix onto the process models"),
			new Label("\t ii) \tprefix_violate_pn: Currently executed transition was in violation of the control flow of the Petri net"),
			new Label("\t iii) \tprefix_violate_decl: Currently executed activity was in violation of the DECLARE model"),
			new Label("\t iv) \treset: Reset the currently displayed frame component (constraint/petri net)"),
			new Label("\t v) \tsync: Currently executed transition was successfully replayed on the process frame")
		);

		legendPopup.getContent().add(legendBox);
		legendPopup.setAutoHide(true);

		toolTipButtonPlan.setOnAction(e -> {
			if (!legendPopup.isShowing()) {
				Bounds screenBounds = toolTipButtonPlan.localToScreen(toolTipButtonPlan.getBoundsInLocal());
				legendPopup.show(toolTipButtonPlan, screenBounds.getMinX(), screenBounds.getMinY() + toolTipButtonPlan.getHeight());
			} else {
				legendPopup.hide();
			}
		});
	}

}
