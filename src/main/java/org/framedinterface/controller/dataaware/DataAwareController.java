package org.framedinterface.controller.dataaware;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.framedinterface.controller.ProgressLayerController;
import org.framedinterface.controller.common.AbstractController;
import org.framedinterface.event.EventCell;
import org.framedinterface.event.EventData;
import org.framedinterface.model.AbstractModel;
import org.framedinterface.model.AttributeDomain;
import org.framedinterface.model.DeclareConstraint;
import org.framedinterface.model.DeclareModel;
import org.framedinterface.model.ModelRegistry;
import org.framedinterface.model.ModelType;
import org.framedinterface.model.PlannerSession;
import org.framedinterface.model.PnModel;
import org.framedinterface.task.GenerateDataAwarePDDLTask;
import org.framedinterface.task.RunEnhspTask;
import org.framedinterface.utils.AlertUtils;
import org.framedinterface.utils.EnhspPlanParser;
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
import javafx.application.Platform;
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
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;


import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
	private ListView<EventData> continuationListView;
	@FXML
	private Label labelCost;
	@FXML
	private Button buttonPrefix;
	@FXML
	private Button buttonRunPlanner;
	@FXML
	private Button importPrefixButton;
	@FXML
	private VBox mainContents;
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
	private static final String PREFIX_OUTPUT_FOLDER = "output/prefixes"; //Where the prefixes handed to the planner are exported as XES
	private static final DateTimeFormatter PREFIX_FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
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

	private String currentPath = Paths.get(".").toAbsolutePath().normalize().toString(); //Project root; dependencies/ and output/ are resolved against it
	private javafx.scene.Node progressLayer;
	private ProgressLayerController progressLayerController;
	private Task<?> runningPlannerTask; //Whichever planner step (PDDL generation or ENHSP) is currently executing, so the progress layer's cancel button can stop it

	//The manually-built prefixes offered by prefixChoice; only the selected entry's prefix/continuation is actually live in PlannerSession at any time (see syncOutToEntry/syncInFromEntry)
	private ObservableList<PrefixEntry> prefixes = FXCollections.observableArrayList();
	private PrefixEntry selectedEntry; //prefixChoice's current selection, whose prefix/continuation is the one live in PlannerSession

	//One prefix's worth of PlannerSession state: its manually-built events, their attribute values, and whatever planner continuation was generated from it
	private static class PrefixEntry {
		private List<String> prefixEvents = new ArrayList<String>();
		private Map<Integer, Map<String, String>> attributeValues = new HashMap<Integer, Map<String, String>>();
		private List<String> plan = new ArrayList<String>();
		private boolean planPresent = false;
		//Only set for plans loaded from the data-aware planner (not for a plan carried over from Data-Agnostic)
		private List<Map<String, String>> planAttributeValues; //Parallel to plan
		private List<Integer> planPrefixSteps; //Prefix event (0-based) -> the (1-based) plan step that replayed it, or -1
		private String planCost = "";
		private ZonedDateTime exportStartTime; //The time the plan's time offsets are relative to (the first event's timestamp as last exported for the planner)
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
			refreshListStatistics();
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
		});

		dataConditionsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			showDataConditions = newValue;
			updateVisualization(declWebView, declModelChoice.getSelectionModel().getSelectedItem(), ModelType.DECLARE);
		});

		//Timeline setup
		setupTimelineControls();

		//Clicking (or arrowing to) an event in either list jumps the timeline to where that event is replayed
		setupTimelineListView(planListView);
		setupTimelineListView(continuationListView);
		planListView.setCellFactory(value -> new EventCell(eventNumber -> handleplanListViewSelection(getTimelineIndex(planListView, eventNumber)), this::getAttributesForActivity, this::deletePrefixEvent, this::onAttributeValueChanged, () -> timestampFieldVisible));
		continuationListView.setCellFactory(value -> new EventCell(eventNumber -> handleplanListViewSelection(getTimelineIndex(continuationListView, eventNumber))));
		continuationListView.setPlaceholder(new Label("No plan for this prefix yet (Run Planner)"));

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
			selectedEntry = newEntry;
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

	//Exports all prefixes to an XES log (output/prefixes/prefixes_<timestamp>.xes), generates one PDDL problem per prefix (output/pddl/problem<i>.pddl) with the
	//data-aware PDDL generator, then runs ENHSP on each of them (output/plans/plan<i>.txt)
	@FXML
	private void onClickPlanner() {
		List<AbstractModel> plannedModels = getSelectedModels(); //In this order, as the generator names its (Petri net) constraints by position
		List<String> declPaths = new ArrayList<String>();
		List<String> pnPaths = new ArrayList<String>();
		for (AbstractModel model : plannedModels) {
			if (model.getModelType() == ModelType.DECLARE) {
				declPaths.add(model.getFilePath());
			} else if (model.getModelType() == ModelType.PN) {
				pnPaths.add(model.getFilePath());
			}
		}
		if (declPaths.isEmpty()) {
			AlertUtils.showWarning("Select at least one Declare model under Process Specifications before running the planner.");
			return;
		}

		//The selected prefix's latest edits only live in PlannerSession until it is synced back into its entry
		if (selectedEntry != null) {
			syncOutToEntry(selectedEntry);
		}
		int prefixCount = prefixes.size();

		//Kept (timestamped, never overwritten) so that this set of prefixes can be re-imported later via the Import button
		File prefixFolder = new File(currentPath, PREFIX_OUTPUT_FOLDER);
		File logFile = new File(prefixFolder, "prefixes_" + LocalDateTime.now().format(PREFIX_FILE_TIMESTAMP_FORMATTER) + ".xes");
		try {
			if (!prefixFolder.isDirectory() && !prefixFolder.mkdirs()) {
				throw new IOException("Could not create folder " + prefixFolder.getAbsolutePath());
			}
			writePrefixesAsXes(logFile);
		} catch (IOException e) {
			e.printStackTrace();
			AlertUtils.showError("Could not export the prefixes to XES: " + e.getMessage());
			return;
		}
		System.out.println("Prefixes exported to: " + logFile.getAbsolutePath());

		loadProgressLayer();
		setUiBusy(true, "Generating PDDL...");

		GenerateDataAwarePDDLTask generatePDDLTask = new GenerateDataAwarePDDLTask(currentPath, declPaths, pnPaths, logFile.getAbsolutePath());
		generatePDDLTask.setOnCancelled(taskEvent -> setUiBusy(false, null));
		generatePDDLTask.setOnFailed(taskEvent -> {
			setUiBusy(false, null);
			AlertUtils.showError("Generating PDDL failed: " + generatePDDLTask.getException().getMessage());
		});
		generatePDDLTask.setOnSucceeded(taskEvent -> {
			RunEnhspTask runEnhspTask = new RunEnhspTask(currentPath, prefixCount);
			if (progressLayerController != null) {
				progressLayerController.getProgressTextLabel().textProperty().bind(runEnhspTask.messageProperty());
			}
			runEnhspTask.setOnCancelled(plannerTaskEvent -> setUiBusy(false, null));
			runEnhspTask.setOnFailed(plannerTaskEvent -> {
				setUiBusy(false, null);
				AlertUtils.showError("Running the planner failed: " + runEnhspTask.getException().getMessage());
			});
			runEnhspTask.setOnSucceeded(plannerTaskEvent -> {
				setUiBusy(false, null);
				String planLoadError = null;
				try {
					loadPlans(plannedModels);
				} catch (Exception e) {
					e.printStackTrace();
					planLoadError = e.getMessage();
				}
				List<Integer> unsolved = runEnhspTask.getValue();
				String planFolder = new File(currentPath, RunEnhspTask.PLAN_OUTPUT_FOLDER).getAbsolutePath();
				if (planLoadError != null) {
					AlertUtils.showError("The plans could not be loaded: " + planLoadError + "\nPlanner output: " + planFolder);
				} else if (unsolved.isEmpty()) {
					AlertUtils.showSuccess("Plans found for all " + prefixCount + " prefix(es).\nPlanner output: " + planFolder);
				} else {
					AlertUtils.showWarning("No plan found for prefix(es) " + unsolved + " (out of " + prefixCount + ").\nPlanner output: " + planFolder);
				}
			});
			startPlannerTask(runEnhspTask);
		});
		startPlannerTask(generatePDDLTask);
	}

	//Replaces every prefix's continuation with its plan from output/plans/plan<i>.txt (prefix i), decoded with the generator's encodings,
	//and replays the selected prefix's plan. Prefixes without a (solved) plan are left without a continuation.
	private void loadPlans(List<AbstractModel> plannedModels) throws IOException {
		Map<String, String> resetTargets = assignPlannerConstraintNames(plannedModels);
		Set<String> categoricalAttributes = new TreeSet<String>();
		for (AbstractModel model : plannedModels) {
			if (model instanceof DeclareModel) {
				((DeclareModel) model).getAttributeDomains().forEach((attribute, domain) -> {
					if (domain.getType() == AttributeDomain.Type.CATEGORICAL) {
						categoricalAttributes.add(attribute.toLowerCase());
					}
				});
			}
		}
		EnhspPlanParser planParser = new EnhspPlanParser(new File(currentPath, GenerateDataAwarePDDLTask.OUTPUT_FOLDER), new File(currentPath, GenerateDataAwarePDDLTask.VARIABLE_VALUES_FILE),
				GenerateDataAwarePDDLTask.ACTIVITY_MAPPING_FILE_PATTERN, categoricalAttributes, resetTargets);

		for (int i = 0; i < prefixes.size(); i++) {
			PrefixEntry entry = prefixes.get(i);
			File planFile = new File(currentPath, RunEnhspTask.PLAN_OUTPUT_FOLDER + "/plan" + (i + 1) + ".txt");
			EnhspPlanParser.ParsedPlan parsedPlan = planFile.isFile()
					? planParser.parse(planFile, entry.prefixEvents, entry.attributeValues, entry.exportStartTime != null ? entry.exportStartTime : ZonedDateTime.now())
					: null;
			if (parsedPlan != null && parsedPlan.isSolved()) {
				entry.plan = parsedPlan.getSteps();
				entry.planAttributeValues = parsedPlan.getAttributeValues();
				entry.planPrefixSteps = parsedPlan.getPrefixEventSteps();
				entry.planCost = parsedPlan.getCost();
				entry.planPresent = true;
			} else {
				clearPlan(entry);
			}
		}

		if (selectedEntry != null) {
			syncInFromEntry(selectedEntry);
		}
		updateTrace(null);
		updateSelectedModelVisualizations();
	}

	private static void clearPlan(PrefixEntry entry) {
		entry.plan = new ArrayList<String>();
		entry.planAttributeValues = null;
		entry.planPrefixSteps = null;
		entry.planCost = "";
		entry.planPresent = false;
	}

	//Gives every Declare constraint the name the PDDL generator uses for it: "<template>_<activation>[_<target>]" (spaces as "_"), with repeated
	//names numbered "_2", "_3", ... across all Declare models passed to it, in order. Petri nets are named "pn" (single net) or "pn1", "pn2", ...
	//(in the order passed). Returns, for the Petri nets, the reset activity their plan resets should replay (the Declare names are replayed as is).
	private Map<String, String> assignPlannerConstraintNames(List<AbstractModel> plannedModels) {
		Map<String, Integer> nameCounts = new HashMap<String, Integer>();
		List<PnModel> petriNets = new ArrayList<PnModel>();
		for (AbstractModel model : ModelRegistry.getInstance().getModels()) {
			if (model instanceof DeclareModel && !plannedModels.contains(model)) {
				((DeclareModel) model).setPlannerConstraintNames(new HashMap<String, DeclareConstraint>()); //Its constraints weren't part of the plan
			}
		}
		for (AbstractModel model : plannedModels) {
			if (model instanceof DeclareModel) {
				Map<String, DeclareConstraint> plannerNames = new HashMap<String, DeclareConstraint>();
				for (DeclareConstraint constraint : ((DeclareModel) model).getDeclareConstraints()) {
					String name = constraint.getTemplate().getTemplateName().replace(" ", "_") + "_" + constraint.getActivationActivity()
							+ (constraint.getTargetActivity() == null || constraint.getTargetActivity().isBlank() ? "" : "_" + constraint.getTargetActivity());
					name = name.toLowerCase();
					int count = nameCounts.merge(name, 1, Integer::sum);
					plannerNames.put(count > 1 ? name + "_" + count : name, constraint);
				}
				((DeclareModel) model).setPlannerConstraintNames(plannerNames);
			} else if (model instanceof PnModel) {
				petriNets.add((PnModel) model);
			}
		}

		Map<String, String> resetTargets = new HashMap<String, String>();
		for (int i = 0; i < petriNets.size(); i++) {
			resetTargets.put(petriNets.size() == 1 ? "pn" : "pn" + (i + 1), petriNets.get(i).getResetActivity());
		}
		return resetTargets;
	}

	private void startPlannerTask(Task<?> task) {
		runningPlannerTask = task;
		Thread thread = new Thread(task);
		thread.setDaemon(true); //Must not keep the application alive if it is closed mid-run
		thread.start();
	}

	private void loadProgressLayer() {
		if (progressLayer != null) {
			return;
		}
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/framedinterface/ProgressLayer.fxml"));
			progressLayer = loader.load();
			progressLayerController = loader.getController();
			progressLayerController.getCancelButton().setOnAction(e -> {
				if (runningPlannerTask != null) {
					runningPlannerTask.cancel(true); //Interrupting the task also destroys the external process it is waiting on (see RunnerUtils.runProcess)
				}
			});
		} catch (Exception e) {
			System.out.println("Cannot load progress layer");
			e.printStackTrace();
		}
	}

	private void setUiBusy(boolean busy, String progressText) {
		Platform.runLater(() -> mainContents.setDisable(busy));

		if (progressLayer != null) {
			StackPane rootElement = (StackPane) getRootRegion();
			if (busy) {
				progressLayerController.getProgressTextLabel().textProperty().unbind();
				progressLayerController.getProgressTextLabel().setText(progressText);
				if (!rootElement.getChildren().contains(progressLayer)) {
					rootElement.getChildren().add(progressLayer);
				}
			} else {
				progressLayerController.getProgressTextLabel().textProperty().unbind();
				rootElement.getChildren().remove(progressLayer);
			}
		}
	}

	//Writes every prefix as one trace (in prefixChoice order, so prefix <i> becomes problem<i>.pddl) of an XES log that the data-aware PDDL generator can read.
	//The generator computes each event's time relative to the trace's first event and needs a timestamp on every event, so events without a
	//(valid) "timestamp" attribute reuse the previous event's timestamp (or the trace's first known one / the export time, if there is none before them)
	//Written with StAX rather than OpenXES' XesXmlSerializer, since the latter depends on the Spex library, which is not bundled with this application
	private void writePrefixesAsXes(File file) throws IOException {
		ZonedDateTime exportTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

		try (OutputStream out = new FileOutputStream(file)) {
			XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8");
			xml.writeStartDocument("UTF-8", "1.0");
			xml.writeStartElement("log");
			xml.writeAttribute("xes.version", "1.0");
			writeXesExtension(xml, "Concept", "concept", "http://www.xes-standard.org/concept.xesext");
			writeXesExtension(xml, "Lifecycle", "lifecycle", "http://www.xes-standard.org/lifecycle.xesext");
			writeXesExtension(xml, "Time", "time", "http://www.xes-standard.org/time.xesext");

			for (int p = 0; p < prefixes.size(); p++) {
				PrefixEntry entry = prefixes.get(p);
				xml.writeStartElement("trace");
				writeXesAttribute(xml, "string", XConceptExtension.KEY_NAME, "Prefix " + (p + 1));

				ZonedDateTime previousTimestamp = null;
				for (int i = 0; i < entry.prefixEvents.size() && previousTimestamp == null; i++) {
					previousTimestamp = parseTimestamp(entry.attributeValues.getOrDefault(i + 1, Collections.emptyMap()).get("timestamp"));
				}
				if (previousTimestamp == null) {
					previousTimestamp = exportTime;
				}
				entry.exportStartTime = previousTimestamp; //The generator times events relative to the first one, so this is what plan times are relative to

				for (int i = 0; i < entry.prefixEvents.size(); i++) {
					Map<String, String> eventAttributeValues = entry.attributeValues.getOrDefault(i + 1, Collections.emptyMap());
					ZonedDateTime timestamp = parseTimestamp(eventAttributeValues.get("timestamp"));
					if (timestamp == null) {
						timestamp = previousTimestamp;
					}
					previousTimestamp = timestamp;

					xml.writeStartElement("event");
					writeXesAttribute(xml, "string", XConceptExtension.KEY_NAME, getActivityNameAsDefined(entry.prefixEvents.get(i)));
					writeXesAttribute(xml, "string", XLifecycleExtension.KEY_TRANSITION, "complete");
					writeXesAttribute(xml, "date", XTimeExtension.KEY_TIMESTAMP, timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
					for (Map.Entry<String, String> attributeValue : eventAttributeValues.entrySet()) {
						String value = attributeValue.getValue() == null ? "" : attributeValue.getValue().strip();
						if (!attributeValue.getKey().equals("timestamp") && !value.isEmpty()) {
							writeXesAttribute(xml, getXesAttributeType(value), attributeValue.getKey(), value);
						}
					}
					xml.writeEndElement();
				}
				xml.writeEndElement();
			}

			xml.writeEndElement();
			xml.writeEndDocument();
			xml.close();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	//The activity's spelling in the first selected process model that contains it (case-insensitively), since the PDDL generator matches
	//activities case-sensitively; prefixes may carry other casings (e.g. lowercase from Data-Agnostic, or an imported XES log)
	//TODO: Warn the user when selected models spell the same activity with different casing
	private String getActivityNameAsDefined(String activityName) {
		for (AbstractModel model : getSelectedModels()) {
			String nameAsDefined = model.getActivityNameAsDefined(activityName);
			if (nameAsDefined != null) {
				return nameAsDefined;
			}
		}
		return activityName;
	}

	private static void writeXesExtension(XMLStreamWriter xml, String name, String prefix, String uri) throws XMLStreamException {
		xml.writeEmptyElement("extension");
		xml.writeAttribute("name", name);
		xml.writeAttribute("prefix", prefix);
		xml.writeAttribute("uri", uri);
	}

	private static void writeXesAttribute(XMLStreamWriter xml, String type, String key, String value) throws XMLStreamException {
		xml.writeEmptyElement(type);
		xml.writeAttribute("key", key);
		xml.writeAttribute("value", value);
	}

	//Parses a value in the "timestamp" attribute format EventCell's editor expects, or returns null if it is missing/invalid
	private static ZonedDateTime parseTimestamp(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDateTime.parse(value.strip(), XES_TIMESTAMP_FORMATTER).atZone(ZoneId.systemDefault());
		} catch (DateTimeParseException e) {
			System.err.println("Ignoring invalid timestamp: " + value);
			return null;
		}
	}

	//Inverse of extractAttributeValueString: the XES attribute type (element name) matching how the value was typed in
	private static String getXesAttributeType(String value) {
		if (value.matches("[-+]?\\d+")) {
			return "int";
		} else if (value.matches("[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?")) {
			return "float";
		} else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
			return "boolean";
		}
		return "string";
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

	//Discards the selected prefix's continuation if it has one (making the prefix editable again), otherwise clears the prefix itself
	@FXML
	void onClickPrefix(ActionEvent event) {
		if (PlannerSession.getInstance().isPlanPresent()) {
			if (selectedEntry != null) {
				clearPlan(selectedEntry);
			}
			PlannerSession.getInstance().setCurrentPlan(new ArrayList<String>());
			PlannerSession.getInstance().setPlanPresent(false);
		} else {
			PlannerSession.getInstance().getCurrentPrefix().clear();
			PlannerSession.getInstance().clearPrefixAttributeValues();
		}
		ModelRegistry.getInstance().getModels().forEach(abstractModel -> abstractModel.resetModel());

		updateTrace(null);
		updateSelectedModelVisualizations();
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
				//The PDDL generator matches activities case-sensitively, so the prefix uses the model file's spelling rather than the internal lowercase name
				if (abstractModel.getActivityNameAsDefined(activityName) != null) {
					activityName = abstractModel.getActivityNameAsDefined(activityName);
				}
				break;
			}
		}
		if (activityName != null) {
			PlannerSession.getInstance().getCurrentPrefix().add(activityName);
			updateTrace(PlannerSession.getInstance().getCurrentPrefix().size());
		}
	}

	//Attribute values (attribute name -> value) attached to each event of the given trace, in order: the manually-built prefix's, or the
	//data-aware plan's (prefix events keep their values, added events carry the planner's); a plan carried over from Data-Agnostic has none
	private List<Map<String, String>> buildAttributeValuesPerEvent(List<String> activeTrace) {
		boolean planPresent = PlannerSession.getInstance().isPlanPresent();
		List<Map<String, String>> planAttributeValues = planPresent && selectedEntry != null ? selectedEntry.planAttributeValues : null;
		List<Map<String, String>> attributeValuesPerEvent = new ArrayList<Map<String, String>>();
		for (int i = 0; i < activeTrace.size(); i++) {
			if (!planPresent) {
				attributeValuesPerEvent.add(PlannerSession.getInstance().getPrefixAttributeValues(i + 1));
			} else if (planAttributeValues != null && i < planAttributeValues.size()) {
				attributeValuesPerEvent.add(planAttributeValues.get(i));
			} else {
				attributeValuesPerEvent.add(Collections.emptyMap());
			}
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
		updateplanListView(PlannerSession.getInstance().getCurrentPrefix());
		updateContinuationListView();
		refreshListStatistics();
		selectTimelineIndex(0);
		labelCost.setText(PlannerSession.getInstance().isPlanPresent() && selectedEntry != null ? selectedEntry.planCost : "");
		updateTimelineControls(activeTrace);

		if (selectIndex != null) {
			eventSlider.setValue(selectIndex);
			currentEventIndex.setValue(selectIndex);
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
		}
	}

	//Clicking or arrowing to an event in a list moves the timeline to that event's replayed position
	private void setupTimelineListView(ListView<EventData> listView) {
		listView.setOnKeyReleased((event) -> {
			if(event.getCode() == KeyCode.UP || event.getCode() == KeyCode.KP_UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.KP_DOWN) {
				EventData selected = listView.getSelectionModel().getSelectedItem();
				if (selected != null) {
					handleplanListViewSelection(selected.getTimelineIndex());
				}
			}
		});
	}

	//Timeline position of the event with the given event number in the given list (0, the trace start, if it isn't found)
	private static int getTimelineIndex(ListView<EventData> listView, int eventNumber) {
		for (EventData eventData : listView.getItems()) {
			if (eventData.getEventNumber() == eventNumber) {
				return eventData.getTimelineIndex();
			}
		}
		return 0;
	}

	//Highlights, in both lists, the event replayed at the given timeline position (a prefix event may have no position of its own while a plan is replayed)
	private void selectTimelineIndex(int timelineIndex) {
		for (ListView<EventData> listView : List.of(planListView, continuationListView)) {
			int listIndex = -1;
			for (int i = 0; i < listView.getItems().size() && listIndex == -1; i++) {
				if (listView.getItems().get(i).getTimelineIndex() == timelineIndex) {
					listIndex = i;
				}
			}
			if (listIndex == -1) {
				listView.getSelectionModel().clearSelection();
			} else {
				listView.scrollTo(listIndex);
				listView.getSelectionModel().clearAndSelect(listIndex);
			}
		}
	}

	private void handleplanListViewSelection(int selectedIndex) {
		if (selectedIndex < 0) {
			return; //An event with no timeline position of its own (e.g. a prefix event a carried-over plan doesn't map back to)
		}
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
			selectTimelineIndex(newValue.intValue());
		});

		stepBackwardButton.setDisable(true);
	}

	//Updates the planListView (Prefix pane) to match the prefix. Without a plan, the prefix itself is replayed (and editable); with one, each
	//prefix event points at (and is labelled with) the plan step that replayed it, and the trace end has no position of its own
	private void updateplanListView(List<String> prefix) {
		boolean planPresent = PlannerSession.getInstance().isPlanPresent();
		List<Integer> planPrefixSteps = planPresent && selectedEntry != null ? selectedEntry.planPrefixSteps : null;
		List<String> plan = PlannerSession.getInstance().getCurrentPlan();

		List<EventData> eventDataList = new ArrayList<EventData>();
		eventDataList.add(EventData.createStartEvent());
		for (int i = 0; i < prefix.size(); i++) {
			EventData eventData = new EventData(i+1, prefix.get(i));
			if (!planPresent) {
				eventData.setAttributeValues(PlannerSession.getInstance().getPrefixAttributeValues(i+1)); //Attribute values can only be attached while no plan is present
			} else if (planPrefixSteps != null && i < planPrefixSteps.size() && planPrefixSteps.get(i) > 0) {
				int step = planPrefixSteps.get(i);
				eventData.setTimelineIndex(step);
				eventData.setPlanAction(plan.get(step - 1).split(";", 2)[0]);
			} else {
				eventData.setTimelineIndex(-1);
			}
			eventDataList.add(eventData);
		}
		EventData endEvent = EventData.createEndEvent(prefix.size()+1);
		if (planPresent) {
			endEvent.setTimelineIndex(-1);
		}
		eventDataList.add(endEvent);

		planListView.getItems().setAll(eventDataList);
	}

	//Updates the continuationListView (Continuation pane) to show the selected prefix's plan, one entry per replayed step
	private void updateContinuationListView() {
		List<EventData> eventDataList = new ArrayList<EventData>();
		if (PlannerSession.getInstance().isPlanPresent()) {
			List<String> plan = PlannerSession.getInstance().getCurrentPlan();
			List<Map<String, String>> planAttributeValues = buildAttributeValuesPerEvent(plan);
			eventDataList.add(EventData.createStartEvent());
			for (int i = 0; i < plan.size(); i++) {
				String[] planAction = plan.get(i).split(";", 2);
				eventDataList.add(planAction.length == 1
						? new EventData(i+1, plan.get(i))
						: new EventData(i+1, getPlanStepDisplayName(planAction[0], planAction[1]), describePlanStep(planAction[0], planAttributeValues.get(i))));
			}
			eventDataList.add(EventData.createEndEvent(plan.size()+1));
		}
		continuationListView.getItems().setAll(eventDataList);
	}

	//Reset steps carry the violated constraint's PDDL name (or a Petri net's reset activity), shown as the constraint / net instead
	private String getPlanStepDisplayName(String action, String activity) {
		if (!action.equals(DeclareModel.RESET_CONSTRAINT_ACTION)) {
			return activity;
		}
		for (AbstractModel model : ModelRegistry.getInstance().getModels()) {
			if (model instanceof PnModel && ((PnModel) model).getResetActivity().equals(activity)) {
				return "Petri net " + model.getModelName();
			} else if (model instanceof DeclareModel && ((DeclareModel) model).getConstraintByPlannerName(activity) != null) {
				String constraintString = ((DeclareModel) model).getConstraintByPlannerName(activity).getConstraintString();
				return constraintString.contains(" |") ? constraintString.substring(0, constraintString.indexOf(" |")).trim() : constraintString.trim(); //Without its conditions
			}
		}
		return activity;
	}

	//E.g. "add [integer=55, categorical=c3] @ 2023-08-07T12:00:00"
	private static String describePlanStep(String action, Map<String, String> attributeValues) {
		StringBuilder description = new StringBuilder(action);
		List<String> values = new ArrayList<String>();
		attributeValues.forEach((attribute, value) -> {
			if (!attribute.equals("timestamp")) {
				values.add(attribute + "=" + value);
			}
		});
		if (!values.isEmpty()) {
			description.append(" ").append(values);
		}
		if (attributeValues.containsKey("timestamp")) {
			description.append(" @ ").append(attributeValues.get("timestamp"));
		}
		return description.toString();
	}

	//Updates the constraint statistics shown for each event in both lists, from the selected Declare model's state at each event's timeline position
	private void refreshListStatistics() {
		AbstractModel declModel = declModelChoice.getSelectionModel().getSelectedItem();
		for (ListView<EventData> listView : List.of(planListView, continuationListView)) {
			for (EventData eventData : listView.getItems()) {
				eventData.setDeclMonitoringStateCounts(declModel instanceof DeclareModel
						? ((DeclareModel) declModel).getMonitoringStateCounts(eventData.getTimelineIndex())
						: Map.of(MonitoringState.SAT, 0, MonitoringState.POSS_SAT, 0, MonitoringState.POSS_VIOL, 0, MonitoringState.VIOL, 0));
			}
			listView.refresh();
		}
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
			new Label("Click on one of the Elements in the panes below to replay the prefix/trace onto the selected process models."),
			new Label(""),
			new Label("After running the planner, the Continuation pane shows the plan found for the selected prefix, which is then replayed instead of the prefix."),
			new Label("The first line shows the activity (or reset constraint), the next line the action taken by the planner, its attribute values and its time:"),
			new Label("\t i) \tsync: A prefix event, replayed as is"),
			new Label("\t ii) \tskip: A prefix event that none of the process models refer to"),
			new Label("\t iii) \tadd: An event added by the planner, with the attribute values it chose"),
			new Label("\t iv) \treset constraint: The constraint (or Petri net) was violated and is reset to its initial state"),
			new Label(""),
			new Label("Reset discards the selected prefix's plan (making the prefix editable again); pressing it again clears the prefix.")
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
