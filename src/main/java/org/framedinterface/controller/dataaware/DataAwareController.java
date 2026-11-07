package org.framedinterface.controller.dataaware;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.framedinterface.utils.enums.OptimizationTarget;
import org.framedinterface.utils.enums.RecoveryStrategy;
import org.framedinterface.utils.enums.SearchHeuristic;
import org.framedinterface.utils.enums.SearchStrategy;
import org.controlsfx.control.CheckComboBox;
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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

/**
 * Controller of the Data-Aware page, on which continuations of trace prefixes are planned with the data-aware planner.
 * <p>
 * On this page, the user can
 * <ul>
 *   <li>select, among the loaded process specifications (Declare models and Petri nets), the ones the planner works with,</li>
 *   <li>set the violation cost of each constraint (or mark it as hard, i.e. not to be violated) and the cost of each activity,</li>
 *   <li>build one or more trace prefixes, by clicking on the activities of the visualized models or by importing an XES log, and attach
 *       attribute values to their events,</li>
 *   <li>run the planner on all prefixes at once: the prefixes are exported as XES, one PDDL problem is generated per prefix
 *       ({@link GenerateDataAwarePDDLTask}) and solved with ENHSP ({@link RunEnhspTask}),</li>
 *   <li>replay the plans found (the prefixes' continuations) on the visualized models with the timeline controls, and export them as XES.</li>
 * </ul>
 * Only the selected prefix's events, attribute values and plan are live in {@link PlannerSession} at any time. The other prefixes are kept
 * in {@link PrefixEntry} objects (see {@link #syncOutToEntry(PrefixEntry)} and {@link #syncInFromEntry(PrefixEntry)}).
 */
public class DataAwareController extends AbstractController {

	//Entries for the "Process Specifications" Table
	// modelRegistryPane is the container (expandable, collapseable)
	// Whereas the TableView contains the content of loaded Process models
	@FXML
	private TitledPane modelRegistryPane;
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

	// Entries for the Constraints table.
	@FXML
	private TitledPane constraintsPane;
	@FXML
	private TreeTableView<ConstraintEntry> constraintsTreeTableView;
	@FXML
	private TreeTableColumn<ConstraintEntry, String> constraintNameColumn;
	@FXML
	private TreeTableColumn<ConstraintEntry, Boolean> constraintHardColumn;
	@FXML
	private TreeTableColumn<ConstraintEntry, Double> constraintCostColumn;

	private static final double DEFAULT_VIOLATION_COST = 100;

	//Violation cost per Process Model (DECLARE or Petri net, keyed by its model), kept across refreshes of the Constraints pane; identity-keyed since
	//DeclareConstraint.equals only compares template and activities, and the same constraint in two models should still have independent costs
	private Map<Object, DoubleProperty> violationCosts = new IdentityHashMap<Object, DoubleProperty>();

	//Whether a constraint (keyed like violationCosts) is hard, i.e. may not be violated, so its violation cost and the recovery strategy don't apply to it
	private Map<Object, BooleanProperty> hardConstraints = new IdentityHashMap<Object, BooleanProperty>();
	private static final String HARD_CONSTRAINT_COST = "x"; //Written to the violation cost file instead of the cost of a hard constraint
	private static final PseudoClass LOCKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("locked");

	//Planner settings, changed via the planner settings dialog (gear button next to Run Planner)
	@FXML
	private Button plannerSettingsButton;
	private RecoveryStrategy recoveryStrategy = RecoveryStrategy.RESET;
	private Double waitingCost = null; //Cost of waiting one time unit; null to use the generator's default
	private static final String GENERATOR_DEFAULT_WAITING_COST = "10"; //Only shown to the user, the generator applies it itself when --waiting_cost is omitted
	//Always contains CONSTRAINT_VIOLATION. Without ACTIVITY_COST the generator gets a cost model of zero costs, without EXECUTION_TIME it ignores time conditions
	private Set<OptimizationTarget> optimizationTargets = EnumSet.allOf(OptimizationTarget.class);
	//ENHSP options
	private SearchStrategy searchStrategy = SearchStrategy.WASTAR;
	private SearchHeuristic searchHeuristic = SearchHeuristic.BLIND;
	private int plannerMemoryGb = DEFAULT_PLANNER_MEMORY_GB;
	private static final int MIN_PLANNER_MEMORY_GB = 4;
	private static final int MAX_PLANNER_MEMORY_GB = 32;
	private static final int DEFAULT_PLANNER_MEMORY_GB = 8;
	private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");

	//Models whose constraints the user collapsed in the Constraints pane, so the pane keeps them collapsed when it is rebuilt
	private Set<AbstractModel> collapsedConstraintModels = Collections.newSetFromMap(new IdentityHashMap<AbstractModel, Boolean>());


	// "Activities" overview
	@FXML
	private TitledPane activitiesAttributesPane;
	@FXML
	private TreeTableView<ActivityEntry> activitiesTreeTableView;
	@FXML
	private Button setActivityCostsButton;
	@FXML
	private Label activityCostIgnoredLabel; //Shown, along with an icon in the pane's title, while Activity Cost isn't an optimization target
	@FXML
	private TreeTableColumn<ActivityEntry, String> activityNameColumn;
	@FXML
	private TreeTableColumn<ActivityEntry, Double> activityCostColumn;

	private static final double DEFAULT_ACTIVITY_COST = 0;

	//Cost per activity, keyed by its lowercased name (activities are merged case-insensitively across models), kept across refreshes of the Activities pane
	private Map<String, DoubleProperty> activityCosts = new HashMap<String, DoubleProperty>();

	//Of the loaded plans (see loadPlans): the recovery strategy they were planned with, and the violation cost the planner was given for each soft
	//Declare constraint / Petri net, keyed by the DeclareConstraint / PnModel itself (identity, see getPlannerConstraintNames)
	private RecoveryStrategy planRecoveryStrategy;
	private Map<Object, BigDecimal> planViolationCosts = new IdentityHashMap<Object, BigDecimal>();

	//Rows of the Activities pane the user collapsed ("<activity>" or "<activity>/<group>", lowercased), so the pane keeps them collapsed when it is rebuilt
	private Set<String> collapsedActivityRows = new HashSet<String>();

	//Attributes (lowercased names) the user collapsed in the Attributes pane, so the pane keeps them collapsed when it is rebuilt
	private Set<String> collapsedAttributeRows = new HashSet<String>();

	//Unique (case-insensitive) activity labels, and their bound attributes (Declare models only), of the currently selected process specifications; refreshed by updateActivitiesListView()
	private Map<String, Set<String>> activityToAttributes = new TreeMap<String, Set<String>>();


	// "Attributes" overview
	@FXML
	private TitledPane attributesPane;
	@FXML
	private TreeView<String> attributesTreeView;


	@FXML
	private SplitPane resultsSplitPane;

	// DECLARE viewer (top, middle) elements
	// All of these elements go on the top half of the screen and is used to display and control the DECLARE viewer
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

	// Petri net Viewer (bottom, middle)
	//
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
	private Button exportContinuationButton;
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
	private static final String CONTINUATION_OUTPUT_FOLDER = "output/continuations"; //Default folder for exported continuations
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

	/**
	 * One row of the Constraints pane: a model (grouping its constraints), or one of its constraints, i.e. a Declare constraint or a whole
	 * Petri net (which the planner treats as a single constraint). Only constraint rows carry a violation cost and a hard flag.
	 */
	private static class ConstraintEntry {
		private final AbstractModel model;
		private final DeclareConstraint constraint; //null for a model row and for a Petri net
		private final DoubleProperty violationCost; //null for a model row; shared with violationCosts, so edits outlive this entry
		private final BooleanProperty hard; //null for a model row; shared with hardConstraints, so edits outlive this entry

		/**
		 * Creates a row of the Constraints pane.
		 *
		 * @param model the model the row belongs to
		 * @param constraint the row's Declare constraint, or {@code null} for a model row and for a Petri net
		 * @param violationCost the constraint's violation cost (shared with {@link DataAwareController#violationCosts}, so edits outlive
		 *        this row), or {@code null} for a model row
		 * @param hard whether the constraint is hard (shared with {@link DataAwareController#hardConstraints}), or {@code null} for a model row
		 */
		private ConstraintEntry(AbstractModel model, DeclareConstraint constraint, DoubleProperty violationCost, BooleanProperty hard) {
			this.model = model;
			this.constraint = constraint;
			this.violationCost = violationCost;
			this.hard = hard;
		}

		/**
		 * Creates the row that groups the constraints of the given model.
		 *
		 * @param model the model
		 * @return a model row, without violation cost or hard flag
		 */
		private static ConstraintEntry modelRow(AbstractModel model) {
			return new ConstraintEntry(model, null, null, null);
		}

		/**
		 * Tells whether this row stands for a model rather than for one of its constraints.
		 *
		 * @return {@code true} for a model row
		 */
		private boolean isModelRow() {
			return violationCost == null;
		}

		/**
		 * Tells whether this row is a hard constraint, i.e. one that may not be violated.
		 *
		 * @return {@code true} for a hard constraint, {@code false} for a soft constraint and for a model row
		 */
		private boolean isHard() {
			return hard != null && hard.get();
		}

		/**
		 * Returns this row's violation cost, unless it is a hard constraint: a hard constraint's cost doesn't apply, so it can't be edited.
		 *
		 * @return the violation cost, or {@code null} for a hard constraint and for a model row
		 */
		private DoubleProperty getEditableViolationCost() {
			return isHard() ? null : violationCost;
		}

		/**
		 * Returns the text shown in the Constraint column.
		 *
		 * @return the model's name for a model row, "Petri net" for a Petri net, or else the constraint as written in the model file but
		 *         without its conditions, e.g. {@code Response[ActivityG, ActivityP]}
		 */
		private String getName() {
			if (isModelRow()) {
				return model.getModelName();
			} else if (constraint == null) {
				return "Petri net";
			}
			String constraintString = constraint.getConstraintString().trim();
			return constraintString.contains(" |") ? constraintString.substring(0, constraintString.indexOf(" |")).trim() : constraintString;
		}

		/**
		 * Returns the text shown as the row's tooltip, which includes what the Constraint column leaves out.
		 *
		 * @return the model's file path for a model row, "Petri net" followed by the model's name for a Petri net, or else the full
		 *         constraint line including its data/time conditions, e.g. {@code Response[ActivityG, ActivityP] |A.integer > 10 |T.integer < 10 |}
		 */
		private String getDetails() {
			if (isModelRow()) {
				return model.getFilePath();
			}
			return constraint == null ? "Petri net " + model.getModelName() : constraint.getConstraintString().trim();
		}
	}

	/**
	 * One row of the Activities pane: an activity (with its cost), one of its "Attributes"/"Models" groups, or an attribute or model within
	 * such a group.
	 */
	private static class ActivityEntry {
		private final String label;
		private final String tooltip; //null for none
		private final DoubleProperty cost; //Only set for activity rows; shared with activityCosts, so edits outlive this entry

		/**
		 * Creates a row of the Activities pane.
		 *
		 * @param label the text shown in the Activity column
		 * @param tooltip the row's tooltip, or {@code null} for none
		 * @param cost the activity's cost for an activity row (shared with {@link DataAwareController#activityCosts}, so edits outlive this
		 *        row), or {@code null} for any other row
		 */
		private ActivityEntry(String label, String tooltip, DoubleProperty cost) {
			this.label = label;
			this.tooltip = tooltip;
			this.cost = cost;
		}
	}

	/**
	 * One prefix's worth of {@link PlannerSession} state: its manually-built events, their attribute values, and the planner continuation
	 * (plan) generated from it, if any.
	 */
	private static class PrefixEntry {
		private List<String> prefixEvents = new ArrayList<String>();
		private Map<Integer, Map<String, String>> attributeValues = new HashMap<Integer, Map<String, String>>();
		private List<String> plan = new ArrayList<String>();
		private boolean planPresent = false;
		//Only set for plans loaded from the data-aware planner (not for a plan carried over from Data-Agnostic)
		private List<Map<String, String>> planAttributeValues; //Parallel to plan
		private List<Integer> planPrefixSteps; //Prefix event (0-based) -> the (1-based) plan step that replayed it, or -1
		private String planCost = "";
		private EnhspPlanParser.ParsedPlan parsedPlan; //For the plan cost's violation/activity/waiting parts (see describePlanCost); null for none
		private ZonedDateTime exportStartTime; //The time the plan's time offsets are relative to (the first event's timestamp as last exported for the planner)
	}

	/**
	 * Initializes the page once its FXML has been loaded. Sets up the Process Specifications, Constraints, Activities and Attributes panes,
	 * the Declare and Petri net visualizations with their zoom controls, the Prefix and Continuation lists, the timeline controls and the
	 * prefix selector.
	 */
	@FXML
	private void initialize() {

		//Model list: view/select only, mirrors Process Frame Overview's table without upload/remove
		modelTabelView.setItems(ModelRegistry.getInstance().getModels());
		modelTabelView.setPlaceholder(new Label("No process specifications selected"));
		modelNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelName()));
		modelTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelType().toString()));
		modelSelectColumn.setCellValueFactory(data -> getModelSelectedProperty(data.getValue()));
		modelSelectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(modelSelectColumn));

		
		selectedDecl.setVisible(false);
		selectedDecl.setManaged(false);
		selectedPN.setVisible(false);
		selectedPN.setManaged(false);
		prefixOnlyLabel.setVisible(false);
		prefixOnlyLabel.setManaged(false);

		//Constraints panel: the constraints of the currently selected process specifications, grouped by (collapsible) model; each Petri net counts as one constraint
		constraintsTreeTableView.setPlaceholder(new Label("No process specifications selected"));
		constraintNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().getName()));
		setupHardColumn();
		setupCostColumn(constraintsTreeTableView, constraintCostColumn, entry -> entry.violationCost, ConstraintEntry::isHard); //Model rows have no cost of their own
		setupBulkCostEditing(constraintsTreeTableView, ConstraintEntry::getEditableViolationCost, "constraint", "constraints"); //A selected model row stands for all of its constraints
		setupBulkHardEditing();
		constraintsTreeTableView.setRowFactory(treeTableView -> new TreeTableRow<ConstraintEntry>() {
			@Override
			protected void updateItem(ConstraintEntry item, boolean empty) {
				super.updateItem(item, empty);
				setTooltip(empty || item == null ? null : new Tooltip(item.getDetails())); //Shows the conditions the Constraint column leaves out
			}
		});

		//Activities panel: the unique activities of the currently selected process specifications, each with its cost, its bound attributes (if any) and the models containing it
		activitiesTreeTableView.setPlaceholder(new Label("No process specifications selected"));
		activityNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getValue().label));
		setupCostColumn(activitiesTreeTableView, activityCostColumn, entry -> entry.cost, entry -> false); //Only activity rows have a cost
		setupBulkCostEditing(activitiesTreeTableView, entry -> entry.cost, "activity", "activities"); //A selected group/attribute/model row stands for its activity
		activitiesTreeTableView.setRowFactory(treeTableView -> new TreeTableRow<ActivityEntry>() {
			@Override
			protected void updateItem(ActivityEntry item, boolean empty) {
				super.updateItem(item, empty);
				setTooltip(empty || item == null || item.tooltip == null ? null : new Tooltip(item.tooltip));
			}
		});
		attributesTreeView.setShowRoot(false);
		updateActivitiesListView();

		//Collapsed panes should only take up their title bar's height, not keep claiming a share of the column's growable space
		for (TitledPane pane : new TitledPane[] {modelRegistryPane, constraintsPane, activitiesAttributesPane, attributesPane, prefixPane, continuationPane}) {
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
				if (!declModelChoice.getItems().isEmpty() && (declModelChoice.getSelectionModel().getSelectedIndex()==-1 || !declModelChoice.getItems().contains(declModelChoice.getSelectionModel().getSelectedItem()))) {
					declModelChoice.getSelectionModel().selectFirst();
				}
			}
		});
		pnModelChoice.getItems().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				if (!pnModelChoice.getItems().isEmpty() && (pnModelChoice.getSelectionModel().getSelectedIndex()==-1 || !pnModelChoice.getItems().contains(pnModelChoice.getSelectionModel().getSelectedItem()))) {
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

		plannerSettingsButton.setTooltip(new Tooltip());
		updatePlannerSettingsTooltip();

		//Sets up the prefix selector with a single initial entry; this also reflects whatever trace/plan is already active in
		//PlannerSession (e.g. carried over from Data-Agnostic), same as the updateTrace(null) it used to do here directly
		setupPrefixSelector();
	}

	/**
	 * Wires up {@link #prefixChoice} to switch which prefix's data (events, attribute values, planner continuation) is live in
	 * {@link PlannerSession}. Starts with a single prefix, which takes over whatever {@link PlannerSession} already holds, so that a trace or
	 * plan carried over from the Data-Agnostic page still shows up.
	 */
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
		prefixes.addListener((Observable observable) -> updateExportButton()); //Removing/importing prefixes may remove the last continuation

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

	/**
	 * Copies the prefix and continuation currently live in {@link PlannerSession} into the given entry, e.g. right before it is deselected.
	 *
	 * @param entry the entry to update
	 * @see #syncInFromEntry(PrefixEntry)
	 */
	private void syncOutToEntry(PrefixEntry entry) {
		entry.prefixEvents = PlannerSession.getInstance().getCurrentPrefix();
		entry.attributeValues = PlannerSession.getInstance().getPrefixAttributeValuesMap();
		entry.plan = PlannerSession.getInstance().getCurrentPlan();
		entry.planPresent = PlannerSession.getInstance().isPlanPresent();
	}

	/**
	 * Makes the given entry's prefix and continuation the ones live in {@link PlannerSession}, e.g. once it is selected.
	 *
	 * @param entry the entry to load
	 * @see #syncOutToEntry(PrefixEntry)
	 */
	private void syncInFromEntry(PrefixEntry entry) {
		PlannerSession.getInstance().setCurrentPrefix(entry.prefixEvents);
		PlannerSession.getInstance().setPrefixAttributeValuesMap(entry.attributeValues);
		PlannerSession.getInstance().setCurrentPlan(entry.plan);
		PlannerSession.getInstance().setPlanPresent(entry.planPresent);
	}

	/**
	 * Handles the "+" button of the Prefix pane: adds a new, empty prefix and selects it. The previously selected prefix is kept in the list
	 * unchanged.
	 */
	@FXML
	private void onClickAddPrefix() {
		PrefixEntry newEntry = new PrefixEntry();
		prefixes.add(newEntry);
		prefixChoice.getSelectionModel().select(newEntry);
	}

	/**
	 * Handles the "-" button of the Prefix pane: removes the selected prefix and selects the one before it (or, if it was the first, the new
	 * first one). Does nothing if it is the only prefix, as at least one must always remain.
	 */
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

	/**
	 * Handles the Import button: replaces all prefixes with the traces of an XES log the user picks, one prefix per trace, each carrying the
	 * attribute values of the trace's events (see {@link #parseXesTracesAsPrefixes(File)}). The prefixes are left unchanged if the log can't
	 * be parsed or has no traces.
	 */
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

	/**
	 * Reads one prefix per trace of the first log in the given XES file. Each event becomes a prefix event (its {@code concept:name}), and its
	 * attributes other than {@code concept:name} and {@code lifecycle:transition} become that event's attribute values. Its
	 * {@code time:timestamp} is stored as the {@code timestamp} attribute, in local time.
	 *
	 * @param file the XES file
	 * @return the prefixes, in trace order; empty if the file contains no log
	 * @throws Exception if the file can't be parsed
	 */
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

	/**
	 * Renders an XES attribute's value the way it would be typed into the attribute editor popup, e.g. "6" rather than "6.0" for a
	 * whole-numbered float.
	 *
	 * @param attribute the attribute
	 * @return the attribute's value as text
	 * @see #getXesAttributeType(String)
	 */
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

	/**
	 * Handles the gear button next to Run Planner: shows the planner settings dialog and applies the settings once it is confirmed with OK.
	 * <ul>
	 *   <li>General tab: the optimization targets (Constraint Violation is mandatory), the recovery strategy and the waiting cost (left empty
	 *       to use the generator's default).</li>
	 *   <li>ENHSP tab: the planner's search strategy, heuristic and memory.</li>
	 * </ul>
	 */
	@FXML
	private void onClickPlannerSettings() {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle("Planner Settings");
		dialog.setHeaderText("Settings used when running the planner");
		dialog.initOwner(getStage());
		dialog.getDialogPane().getStylesheets().add("/org/framedinterface/dialogPane.css");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		Runnable fitDialogToContent = () -> Platform.runLater(() -> dialog.getDialogPane().getScene().getWindow().sizeToScene()); //It doesn't grow or shrink with its notes by itself

		CheckComboBox<OptimizationTarget> optimizationChoice = new CheckComboBox<OptimizationTarget>(FXCollections.observableArrayList(OptimizationTarget.values()));
		optimizationTargets.forEach(target -> optimizationChoice.getCheckModel().check(target));
		optimizationChoice.setMaxWidth(Double.MAX_VALUE);
		Label optimizationDescription = new Label("Constraint Violation is always optimized. Execution Time only matters for Declare models with temporal constraints.");
		Label optimizationNote = new Label(); //Explains the consequences of the unchecked targets
		optimizationNote.getStyleClass().add("dialog-note");
		Runnable updateOptimizationNote = () -> {
			List<String> notes = new ArrayList<String>();
			if (!optimizationChoice.getCheckModel().isChecked(OptimizationTarget.ACTIVITY_COST)) {
				notes.add("Without Activity Cost, the cost model is ignored: every activity costs 0, whatever its cost in the Activities pane.");
			}
			if (!optimizationChoice.getCheckModel().isChecked(OptimizationTarget.EXECUTION_TIME)) {
				notes.add("Without Execution Time, all temporal constraints in the Declare models are discarded as well. Time doesn't advance, so the waiting cost doesn't apply.");
			}
			optimizationNote.setText(String.join(System.lineSeparator(), notes));
			optimizationNote.setVisible(!notes.isEmpty());
			optimizationNote.setManaged(!notes.isEmpty());
		};
		updateOptimizationNote.run();
		optimizationChoice.getCheckModel().getCheckedItems().addListener((Observable observable) -> {
			if (!optimizationChoice.getCheckModel().isChecked(OptimizationTarget.CONSTRAINT_VIOLATION)) {
				//Mandatory, so unchecking it is undone; only afterwards, as the check box is still being updated from this change
				Platform.runLater(() -> optimizationChoice.getCheckModel().check(OptimizationTarget.CONSTRAINT_VIOLATION));
			}
			updateOptimizationNote.run();
			fitDialogToContent.run();
		});

		ChoiceBox<RecoveryStrategy> recoveryChoice = new ChoiceBox<RecoveryStrategy>();
		recoveryChoice.getItems().setAll(RecoveryStrategy.values());
		recoveryChoice.getSelectionModel().select(recoveryStrategy);
		Label recoveryDescription = new Label();
		recoveryDescription.textProperty().bind(Bindings.createStringBinding(() -> recoveryChoice.getValue().getDescription(), recoveryChoice.valueProperty()));

		TextField waitingCostField = new TextField(waitingCost == null ? "" : COST_CONVERTER.toString(waitingCost));
		waitingCostField.setPromptText(GENERATOR_DEFAULT_WAITING_COST + " (default)");
		waitingCostField.setPrefColumnCount(6);
		waitingCostField.setMaxWidth(Region.USE_PREF_SIZE);
		Label waitingCostDescription = new Label("The cost of waiting one time unit, used when optimizing for Execution Time: any positive number (e.g. 0.5). Leave it empty to use the generator's default ("
				+ GENERATOR_DEFAULT_WAITING_COST + ").");
		BooleanBinding waitingCostInvalid = Bindings.createBooleanBinding(() -> !isValidWaitingCost(waitingCostField.getText()), waitingCostField.textProperty());
		waitingCostInvalid.addListener((observable, wasInvalid, isInvalid) -> waitingCostField.pseudoClassStateChanged(ERROR_PSEUDO_CLASS, isInvalid));
		dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(waitingCostInvalid);

		ChoiceBox<SearchStrategy> searchStrategyChoice = new ChoiceBox<SearchStrategy>();
		searchStrategyChoice.getItems().setAll(SearchStrategy.values());
		searchStrategyChoice.getSelectionModel().select(searchStrategy);
		ChoiceBox<SearchHeuristic> searchHeuristicChoice = new ChoiceBox<SearchHeuristic>();
		searchHeuristicChoice.getItems().setAll(SearchHeuristic.values());
		searchHeuristicChoice.getSelectionModel().select(searchHeuristic);
		Label searchDescription = new Label("An optimal search strategy with an optimal heuristic finds a plan with the lowest cost.");
		Label optimalityNote = new Label(); //Warns about the non-optimal choices
		optimalityNote.getStyleClass().add("dialog-note");
		Runnable updateOptimalityNote = () -> {
			List<String> nonOptimal = new ArrayList<String>();
			if (!searchStrategyChoice.getValue().isOptimal()) {
				nonOptimal.add(searchStrategyChoice.getValue().getCliValue());
			}
			if (!searchHeuristicChoice.getValue().isOptimal()) {
				nonOptimal.add(searchHeuristicChoice.getValue().getCliValue());
			}
			optimalityNote.setText(String.join(" and ", nonOptimal) + (nonOptimal.size() == 1 ? " is" : " are") + " not optimal: the plan found may not have the lowest cost.");
			optimalityNote.setVisible(!nonOptimal.isEmpty());
			optimalityNote.setManaged(!nonOptimal.isEmpty());
		};
		updateOptimalityNote.run();
		for (ChoiceBox<?> searchChoice : List.of(searchStrategyChoice, searchHeuristicChoice)) {
			searchChoice.valueProperty().addListener(observable -> {
				updateOptimalityNote.run();
				fitDialogToContent.run();
			});
		}

		Slider memorySlider = new Slider(MIN_PLANNER_MEMORY_GB, MAX_PLANNER_MEMORY_GB, plannerMemoryGb);
		memorySlider.setMajorTickUnit(4);
		memorySlider.setMinorTickCount(3); //Whole GB
		memorySlider.setSnapToTicks(true);
		memorySlider.setBlockIncrement(1);
		memorySlider.setShowTickMarks(true);
		memorySlider.setShowTickLabels(true);
		Label memoryValue = new Label();
		memoryValue.setMinWidth(Region.USE_PREF_SIZE);
		memoryValue.textProperty().bind(Bindings.createStringBinding(() -> Math.round(memorySlider.getValue()) + " GB", memorySlider.valueProperty()));
		HBox memoryBox = new HBox(8, memorySlider, memoryValue);
		HBox.setHgrow(memorySlider, Priority.ALWAYS);
		Label memoryDescription = new Label("The most memory the planner may use (Java heap).");

		for (Label description : List.of(optimizationDescription, optimizationNote, recoveryDescription, waitingCostDescription, searchDescription, optimalityNote, memoryDescription)) {
			description.setWrapText(true);
			description.setPrefWidth(320);
			description.setMinHeight(Region.USE_PREF_SIZE);
		}
		GridPane generalContent = createSettingsGrid();
		generalContent.addRow(0, new Label("Optimize:"), optimizationChoice);
		generalContent.add(optimizationDescription, 1, 1);
		generalContent.add(optimizationNote, 1, 2);
		generalContent.addRow(3, new Label("Recovery:"), recoveryChoice);
		generalContent.add(recoveryDescription, 1, 4);
		generalContent.addRow(5, new Label("Waiting cost:"), waitingCostField);
		generalContent.add(waitingCostDescription, 1, 6);
		GridPane enhspContent = createSettingsGrid();
		enhspContent.addRow(0, new Label("Search strategy:"), searchStrategyChoice);
		enhspContent.addRow(1, new Label("Heuristic:"), searchHeuristicChoice);
		enhspContent.add(searchDescription, 1, 2);
		enhspContent.add(optimalityNote, 1, 3);
		enhspContent.addRow(4, new Label("Memory:"), memoryBox);
		enhspContent.add(memoryDescription, 1, 5);
		TabPane tabs = new TabPane(new Tab("General", generalContent), new Tab("ENHSP", enhspContent));
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		dialog.getDialogPane().setContent(tabs);

		if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
			return;
		}
		optimizationTargets = EnumSet.of(OptimizationTarget.CONSTRAINT_VIOLATION);
		optimizationTargets.addAll(optimizationChoice.getCheckModel().getCheckedItems());
		recoveryStrategy = recoveryChoice.getValue();
		waitingCost = waitingCostField.getText().isBlank() ? null : COST_CONVERTER.fromString(waitingCostField.getText());
		searchStrategy = searchStrategyChoice.getValue();
		searchHeuristic = searchHeuristicChoice.getValue();
		plannerMemoryGb = (int) Math.round(memorySlider.getValue());
		updateActivitiesPaneState();
		updatePlannerSettingsTooltip();
	}

	/**
	 * Creates the layout of a planner settings tab: a column of labels and a column of controls, with their descriptions below them.
	 *
	 * @return an empty grid with the spacing and padding of the planner settings' tabs
	 */
	private static GridPane createSettingsGrid() {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(6);
		grid.setPadding(new Insets(10));
		return grid;
	}

	/**
	 * Greys out the Activities pane's costs while Activity Cost isn't an optimization target, since they are then ignored (see
	 * {@link #buildCostModelLines()}). A warning explains why: in the pane, and as an icon in its title, which stays visible while the pane is
	 * collapsed.
	 */
	private void updateActivitiesPaneState() {
		boolean activityCostIgnored = !optimizationTargets.contains(OptimizationTarget.ACTIVITY_COST);
		activitiesTreeTableView.setDisable(activityCostIgnored);
		setActivityCostsButton.setDisable(activityCostIgnored);
		activityCostIgnoredLabel.setVisible(activityCostIgnored);
		activityCostIgnoredLabel.setManaged(activityCostIgnored);
		FontIcon warningIcon = new FontIcon("fa-exclamation-triangle");
		warningIcon.getStyleClass().add("warning-note__icon");
		Tooltip.install(warningIcon, new Tooltip(activityCostIgnoredLabel.getText()));
		activitiesAttributesPane.setGraphic(activityCostIgnored ? warningIcon : null);
	}

	/**
	 * Tells whether the given text is a valid waiting cost.
	 *
	 * @param text the text typed into the waiting cost field
	 * @return {@code true} if the text is a positive number, or empty (the generator's default is used)
	 */
	private static boolean isValidWaitingCost(String text) {
		if (text.isBlank()) {
			return true;
		}
		Double cost = COST_CONVERTER.fromString(text);
		return cost != null && cost > 0;
	}

	/**
	 * Updates the tooltip of the planner settings button to summarize the current settings, since they are otherwise only visible in their
	 * dialog.
	 */
	private void updatePlannerSettingsTooltip() {
		plannerSettingsButton.getTooltip().setText("Planner settings" + System.lineSeparator()
				+ "Optimize: " + optimizationTargets.stream().map(OptimizationTarget::toString).collect(Collectors.joining(", ")) + System.lineSeparator()
				+ "Recovery: " + recoveryStrategy + System.lineSeparator()
				+ "Waiting cost: " + (waitingCost == null ? GENERATOR_DEFAULT_WAITING_COST + " (default)" : COST_CONVERTER.toString(waitingCost)) + System.lineSeparator()
				+ "Search strategy: " + searchStrategy + System.lineSeparator()
				+ "Heuristic: " + searchHeuristic + System.lineSeparator()
				+ "Memory: " + plannerMemoryGb + " GB");
	}

	/**
	 * Handles the Run Planner button: plans a continuation for every prefix with the selected process specifications, in the background.
	 * <ol>
	 *   <li>Exports all prefixes to an XES log ({@code output/prefixes/prefixes_<timestamp>.xes}), which can be re-imported later.</li>
	 *   <li>Generates one PDDL problem per prefix ({@code output/pddl/problem<i>.pddl}) with the data-aware PDDL generator
	 *       ({@link GenerateDataAwarePDDLTask}).</li>
	 *   <li>Runs ENHSP on each of them ({@link RunEnhspTask}), which writes {@code output/plans/plan<i>.txt}.</li>
	 *   <li>Loads the plans found as the prefixes' continuations (see {@link #loadPlans(List, BigDecimal, RecoveryStrategy)}).</li>
	 * </ol>
	 * At least one Declare model must be selected, and the PDDL domain of the chosen recovery strategy must exist. The page is locked while
	 * planning, and the planner can be cancelled from the progress layer.
	 */
	@FXML
	private void onClickPlanner() {
		String selectedDeclModels = "";
		String selectedPnModels = "";
		List<AbstractModel> plannedModels = getSelectedModels(); //In this order, as the generator names its (Petri net) constraints by position
		List<String> declPaths = new ArrayList<String>();
		List<String> pnPaths = new ArrayList<String>();
		for (AbstractModel model : plannedModels) {
			if (model.getModelType() == ModelType.DECLARE) {
				declPaths.add(model.getFilePath());
				selectedDeclModels = selectedDeclModels + model.getModelName() + "+";
			} else if (model.getModelType() == ModelType.PN) {
				pnPaths.add(model.getFilePath());
				selectedPnModels = selectedPnModels + model.getModelName() + "+";
			}
		}
		if (declPaths.isEmpty()) {
			AlertUtils.showWarning("Select at least one Declare model under Process Specifications before running the planner.");
			return;
		}
		File domainFile = RunEnhspTask.getDomainFile(currentPath, recoveryStrategy);
		if (!domainFile.isFile()) { //Checked up front, rather than only after generating the PDDL problems
			AlertUtils.showError("The PDDL domain for the recovery strategy \"" + recoveryStrategy + "\" was not found:\n" + domainFile.getAbsolutePath());
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

		String waitingCostArgument = waitingCost == null ? null : COST_CONVERTER.toString(waitingCost);
		//The planner's metric is relative to the generator's default waiting cost, so the plan cost is the metric times this factor (computed as the generator does)
		BigDecimal planCostFactor = waitingCostArgument == null ? BigDecimal.ONE
				: new BigDecimal(waitingCostArgument).divide(new BigDecimal(GENERATOR_DEFAULT_WAITING_COST), MathContext.DECIMAL64).stripTrailingZeros();
		GenerateDataAwarePDDLTask generatePDDLTask = new GenerateDataAwarePDDLTask(currentPath, declPaths, pnPaths, logFile.getAbsolutePath(), recoveryStrategy, buildCostModelLines(), buildViolationCostLines(plannedModels),
				waitingCostArgument, !optimizationTargets.contains(OptimizationTarget.EXECUTION_TIME));
		generatePDDLTask.setOnCancelled(taskEvent -> setUiBusy(false, null));
		generatePDDLTask.setOnFailed(taskEvent -> {
			setUiBusy(false, null);
			AlertUtils.showError("Generating PDDL failed: " + generatePDDLTask.getException().getMessage());
		});
		generatePDDLTask.setOnSucceeded(taskEvent -> {
			RunEnhspTask runEnhspTask = new RunEnhspTask(currentPath, prefixCount, recoveryStrategy, searchStrategy, searchHeuristic, plannerMemoryGb);
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
					loadPlans(plannedModels, planCostFactor, recoveryStrategy);
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


		//prefixOnlyLabel.setVisible(false);
		//prefixOnlyLabel.setManaged(false);
		try {
			selectedDecl.setText(selectedDeclModels);
			selectedDecl.setVisible(true);
			selectedDecl.setManaged(true);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		try {
			selectedPN.setText(selectedPnModels);
			selectedPN.setVisible(true);
			selectedPN.setManaged(true);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/**
	 * Replaces every prefix's continuation with its plan from {@code output/plans/plan<i>.txt} (for prefix {@code i}), decoded with the
	 * generator's encodings, then replays the selected prefix's plan. Prefixes without a (solved) plan are left without a continuation. Plan
	 * costs are the planner's metric times {@code planCostFactor}, split into their violation, activity and waiting parts using the cost files
	 * handed to the generator.
	 *
	 * @param plannedModels the process specifications the planner was run with, in the order they were passed to the generator
	 * @param planCostFactor the factor that turns the planner's metric into the plan cost (the waiting cost relative to the generator's default)
	 * @param plannedRecoveryStrategy the recovery strategy the planner was run with
	 * @throws IOException if a plan or one of the generator's output files can't be read
	 */
	private void loadPlans(List<AbstractModel> plannedModels, BigDecimal planCostFactor, RecoveryStrategy plannedRecoveryStrategy) throws IOException {
		Map<String, String> resetTargets = assignPlannerConstraintNames(plannedModels);
		planRecoveryStrategy = plannedRecoveryStrategy;
		planViolationCosts = getSoftViolationCosts(plannedModels); //The UI was locked while planning, so these are still the costs the planner was given
		EnhspPlanParser planParser = new EnhspPlanParser(new File(currentPath, GenerateDataAwarePDDLTask.OUTPUT_FOLDER), new File(currentPath, GenerateDataAwarePDDLTask.VARIABLE_VALUES_FILE),
				GenerateDataAwarePDDLTask.ACTIVITY_MAPPING_FILE_PATTERN, new File(currentPath, GenerateDataAwarePDDLTask.COST_MODEL_FILE),
				new File(currentPath, GenerateDataAwarePDDLTask.VIOLATION_COST_FILE), getCategoricalAttributes(plannedModels), resetTargets, planCostFactor, plannedRecoveryStrategy);

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
				entry.parsedPlan = parsedPlan;
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

	/**
	 * Collects the attributes declared categorical (e.g. {@code categorical: c1, c2, c3}) by any of the given Declare models.
	 *
	 * @param models the models; those that aren't Declare models are ignored
	 * @return the lowercased names of the categorical attributes
	 */
	private static Set<String> getCategoricalAttributes(List<AbstractModel> models) {
		Set<String> categoricalAttributes = new TreeSet<String>();
		for (AbstractModel model : models) {
			if (model instanceof DeclareModel) {
				((DeclareModel) model).getAttributeDomains().forEach((attribute, domain) -> {
					if (domain.getType() == AttributeDomain.Type.CATEGORICAL) {
						categoricalAttributes.add(attribute.toLowerCase());
					}
				});
			}
		}
		return categoricalAttributes;
	}

	/**
	 * Removes the given prefix's continuation, along with everything loaded with it (its attribute values, cost, ...).
	 *
	 * @param entry the prefix
	 */
	private static void clearPlan(PrefixEntry entry) {
		entry.plan = new ArrayList<String>();
		entry.planAttributeValues = null;
		entry.planPrefixSteps = null;
		entry.planCost = "";
		entry.parsedPlan = null;
		entry.planPresent = false;
	}

	/**
	 * Computes the name the PDDL generator gives each Declare constraint of the given models: {@code <template>_<activation>[_<target>]},
	 * with spaces as "_" and the activities spelled as in the model file. Repeated names are numbered {@code _2}, {@code _3}, ... across all
	 * Declare models, in the given order. Activation and target are as in {@link DeclareConstraint}, i.e. already swapped for the Precedence
	 * templates: {@code Precedence[D, F]} is named {@code Precedence_F_D}.
	 *
	 * @param plannedModels the models handed to the generator, in the order they were passed to it
	 * @return the planner name of each Declare constraint. Identity-keyed, since {@link DeclareConstraint#equals(Object)} only compares
	 *         template and activities, and two models may contain the same constraint
	 */
	private static Map<DeclareConstraint, String> getPlannerConstraintNames(List<AbstractModel> plannedModels) {
		Map<DeclareConstraint, String> plannerNames = new IdentityHashMap<DeclareConstraint, String>();
		Map<String, Integer> nameCounts = new HashMap<String, Integer>(); //By lowercased name, as DeclareConstraint lowercases its activities
		for (AbstractModel model : plannedModels) {
			if (model instanceof DeclareModel) {
				for (DeclareConstraint constraint : ((DeclareModel) model).getDeclareConstraints()) {
					String activation = constraint.getActivationActivity();
					String target = constraint.getTargetActivity();
					//The original spelling only survives in the constraint line, e.g. "Precedence[ActivityD, ActivityF] |..."
					Matcher argumentsMatcher = Pattern.compile("\\[(.*?)\\]").matcher(constraint.getConstraintString());
					if (argumentsMatcher.find()) {
						String[] arguments = argumentsMatcher.group(1).split(",");
						if (arguments.length >= 2) {
							boolean reversed = constraint.getTemplate().getReverseActivationTarget();
							activation = (reversed ? arguments[1] : arguments[0]).trim();
							target = (reversed ? arguments[0] : arguments[1]).trim();
						} else if (arguments.length == 1) {
							activation = arguments[0].trim();
						}
					}
					String name = constraint.getTemplate().getTemplateName().replace(" ", "_") + "_" + activation
							+ (target == null || target.isBlank() ? "" : "_" + target);
					int count = nameCounts.merge(name.toLowerCase(), 1, Integer::sum);
					plannerNames.put(constraint, count > 1 ? name + "_" + count : name);
				}
			}
		}
		return plannerNames;
	}

	/**
	 * Gives every Declare constraint of the planned models the name the PDDL generator uses for it (see
	 * {@link #getPlannerConstraintNames(List)}), so that the plan's reset steps can be matched to it. The constraints of Declare models that
	 * weren't planned with lose their names. Petri nets are named {@code pn} (single net) or {@code pn1}, {@code pn2}, ... (in the given order).
	 *
	 * @param plannedModels the models handed to the generator, in the order they were passed to it
	 * @return for each Petri net's planner name, the reset activity its plan resets should replay (Declare constraint names are replayed as is)
	 */
	private Map<String, String> assignPlannerConstraintNames(List<AbstractModel> plannedModels) {
		Map<DeclareConstraint, String> constraintNames = getPlannerConstraintNames(plannedModels);
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
					plannerNames.put(constraintNames.get(constraint).toLowerCase(), constraint); //Looked up case-insensitively
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

	/**
	 * Runs the given planner step (PDDL generation or ENHSP) on a daemon thread, and remembers it as the task the progress layer's cancel
	 * button stops.
	 *
	 * @param task the task to run
	 */
	private void startPlannerTask(Task<?> task) {
		runningPlannerTask = task;
		Thread thread = new Thread(task);
		thread.setDaemon(true); //Must not keep the application alive if it is closed mid-run
		thread.start();
	}

	/**
	 * Loads the progress layer shown while the planner runs, and wires its cancel button to cancel the running planner task. It is only
	 * loaded once; if loading fails, the planner runs without a progress layer.
	 */
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

	/**
	 * Locks or unlocks the page while the planner runs, showing or hiding the progress layer on top of it.
	 *
	 * @param busy {@code true} to lock the page and show the progress layer, {@code false} to unlock it and hide the layer
	 * @param progressText the text shown on the progress layer; ignored when {@code busy} is {@code false}
	 */
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

	/**
	 * Writes every prefix as one trace of an XES log the data-aware PDDL generator can read, in {@link #prefixChoice} order, so that prefix
	 * {@code i} becomes {@code problem<i>.pddl}. The generator computes each event's time relative to the trace's first event and needs a
	 * timestamp on every event: events still without one (e.g. of an imported prefix that was never selected) get one here, the same way
	 * {@link #updateTrace(Integer)} gives one to the selected prefix's events. Also records each prefix's
	 * {@link PrefixEntry#exportStartTime}, which the times of its plan are relative to.
	 *
	 * @param file the XES file to write
	 * @throws IOException if the file can't be written
	 */
	private void writePrefixesAsXes(File file) throws IOException {
		ZonedDateTime exportTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

		try (XesWriter xes = new XesWriter(file, "Prefixes")) {
			for (int p = 0; p < prefixes.size(); p++) {
				PrefixEntry entry = prefixes.get(p);
				xes.startTrace("Prefix " + (p + 1));

				List<ZonedDateTime> timestamps = fillMissingTimestamps(entry.prefixEvents, entry.attributeValues, exportTime);
				entry.exportStartTime = timestamps.isEmpty() ? exportTime : timestamps.get(0); //The generator times events relative to the first one, so this is what plan times are relative to

				for (int i = 0; i < entry.prefixEvents.size(); i++) {
					Map<String, String> eventAttributeValues = entry.attributeValues.get(i + 1);
					xes.startEvent(getActivityNameAsDefined(entry.prefixEvents.get(i)));
					for (Map.Entry<String, String> attributeValue : eventAttributeValues.entrySet()) {
						String value = attributeValue.getValue() == null ? "" : attributeValue.getValue().strip();
						if (!attributeValue.getKey().equals("timestamp") && !value.isEmpty()) {
							xes.writeAttribute(getXesAttributeType(value), attributeValue.getKey(), value);
						}
					}
					xes.writeTimestamp(timestamps.get(i));
					xes.endElement();
				}
				xes.endElement();
			}
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	//TODO: Warn the user when selected models spell the same activity with different casing
	/**
	 * Returns an activity's spelling in the first selected process specification that contains it (compared case-insensitively). The PDDL
	 * generator matches activities case-sensitively, while prefixes may carry other casings (e.g. lowercase from the Data-Agnostic page, or
	 * from an imported XES log).
	 *
	 * @param activityName the activity, in any casing
	 * @return the activity as spelled in the model file, or {@code activityName} itself if no selected model contains it
	 */
	private String getActivityNameAsDefined(String activityName) {
		for (AbstractModel model : getSelectedModels()) {
			String nameAsDefined = model.getActivityNameAsDefined(activityName);
			if (nameAsDefined != null) {
				return nameAsDefined;
			}
		}
		return activityName;
	}

	/**
	 * Writes an XES log laid out like the logs OpenXES produces (one element per line, tab-indented, same log header). Uses StAX rather than
	 * OpenXES' {@code XesXmlSerializer}, since the latter depends on the Spex library, which isn't bundled with this application.
	 * <p>
	 * Traces and events are opened with {@link #startTrace(String)} and {@link #startEvent(String)}, followed by their attributes, and
	 * closed with {@link #endElement()}. Closing the writer closes all elements still open, and the file.
	 */
	private static class XesWriter implements AutoCloseable {
		private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"); //Local time, e.g. "2023-08-07T02:08:50"

		private final OutputStream out;
		private final XMLStreamWriter xml;
		private int depth = 0;

		/**
		 * Creates the file and writes the log's header: the Time, Lifecycle and Concept extensions, the global trace and event attributes,
		 * the event name classifier, and the log's name.
		 *
		 * @param file the file to write; overwritten if it exists
		 * @param logName the log's {@code concept:name}
		 * @throws IOException if the file can't be created
		 * @throws XMLStreamException if writing the header fails
		 */
		XesWriter(File file, String logName) throws IOException, XMLStreamException {
			out = new FileOutputStream(file);
			xml = XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8");
			xml.writeStartDocument("UTF-8", "1.0");
			newLine();
			xml.writeStartElement("log");
			xml.writeAttribute("xes.version", "1.0");
			xml.writeAttribute("xes.features", "nested-attributes");
			xml.writeDefaultNamespace("http://www.xes-standard.org/");
			depth++;
			writeExtension("Time", "time", "http://www.xes-standard.org/time.xesext");
			writeExtension("Lifecycle", "lifecycle", "http://www.xes-standard.org/lifecycle.xesext");
			writeExtension("Concept", "concept", "http://www.xes-standard.org/concept.xesext");
			for (String scope : new String[] {"trace", "event"}) {
				startElement("global");
				xml.writeAttribute("scope", scope);
				writeAttribute("string", XConceptExtension.KEY_NAME, "__INVALID__");
				endElement();
			}
			newLine();
			xml.writeEmptyElement("classifier");
			xml.writeAttribute("name", "Event Name");
			xml.writeAttribute("keys", XConceptExtension.KEY_NAME);
			writeAttribute("string", XConceptExtension.KEY_NAME, logName);
			writeAttribute("string", "lifecycle:model", "standard");
		}

		/**
		 * Opens a trace with the given name. The caller adds its events and closes it with {@link #endElement()}.
		 *
		 * @param name the trace's {@code concept:name}
		 * @throws XMLStreamException if writing fails
		 */
		void startTrace(String name) throws XMLStreamException {
			startElement("trace");
			writeAttribute("string", XConceptExtension.KEY_NAME, name);
		}

		/**
		 * Opens an event of the given activity, with its {@code concept:name} and its {@code lifecycle:transition} ("complete"). The caller
		 * adds further attributes and closes it with {@link #endElement()}.
		 *
		 * @param activity the event's activity
		 * @throws XMLStreamException if writing fails
		 */
		void startEvent(String activity) throws XMLStreamException {
			startElement("event");
			writeAttribute("string", XConceptExtension.KEY_NAME, activity);
			writeAttribute("string", XLifecycleExtension.KEY_TRANSITION, "complete");
		}

		/**
		 * Writes the {@code time:timestamp} attribute of the open event, in local time without time zone, e.g. {@code 2023-08-07T02:08:50}.
		 *
		 * @param timestamp the timestamp
		 * @throws XMLStreamException if writing fails
		 */
		void writeTimestamp(ZonedDateTime timestamp) throws XMLStreamException {
			writeAttribute("date", XTimeExtension.KEY_TIMESTAMP, timestamp.withZoneSameInstant(ZoneId.systemDefault()).format(TIMESTAMP_FORMATTER));
		}

		/**
		 * Writes an attribute of the innermost open element.
		 *
		 * @param type the XES attribute type, i.e. the element name: {@code string}, {@code int}, {@code float}, {@code boolean},
		 *        {@code date}, ...
		 * @param key the attribute's key
		 * @param value the attribute's value
		 * @throws XMLStreamException if writing fails
		 */
		void writeAttribute(String type, String key, String value) throws XMLStreamException {
			newLine();
			xml.writeEmptyElement(type);
			xml.writeAttribute("key", key);
			xml.writeAttribute("value", value);
		}

		/**
		 * Closes the innermost open element (trace, event, ...).
		 *
		 * @throws XMLStreamException if writing fails
		 */
		void endElement() throws XMLStreamException {
			depth--;
			newLine();
			xml.writeEndElement();
		}

		/**
		 * Writes the declaration of an XES extension.
		 *
		 * @param name the extension's name, e.g. "Time"
		 * @param prefix the prefix of the extension's attribute keys, e.g. "time"
		 * @param uri the location of the extension's definition
		 * @throws XMLStreamException if writing fails
		 */
		private void writeExtension(String name, String prefix, String uri) throws XMLStreamException {
			newLine();
			xml.writeEmptyElement("extension");
			xml.writeAttribute("name", name);
			xml.writeAttribute("prefix", prefix);
			xml.writeAttribute("uri", uri);
		}

		/**
		 * Opens an element on a new line, and indents the elements within it one tab further.
		 *
		 * @param name the element's name
		 * @throws XMLStreamException if writing fails
		 */
		private void startElement(String name) throws XMLStreamException {
			newLine();
			xml.writeStartElement(name);
			depth++;
		}

		/**
		 * Starts a new line, indented by the current nesting depth.
		 *
		 * @throws XMLStreamException if writing fails
		 */
		private void newLine() throws XMLStreamException {
			xml.writeCharacters("\n" + "\t".repeat(depth));
		}

		/**
		 * Closes all elements still open (including the log), ends the document and closes the file.
		 *
		 * @throws IOException if writing or closing the file fails
		 */
		@Override
		public void close() throws IOException {
			try {
				while (depth > 0) {
					endElement();
				}
				xml.writeCharacters("\n");
				xml.writeEndDocument();
				xml.close();
			} catch (XMLStreamException e) {
				throw new IOException(e);
			} finally {
				out.close();
			}
		}
	}

	/**
	 * Gives every prefix event without a (valid) {@code timestamp} attribute one: the given time, or the preceding event's timestamp if that
	 * is later, since an event can't happen before the one preceding it. The timestamp is stored rather than only exported, so the timestamp
	 * {@link EventCell}'s editor shows for an event is the one exported to the planner, whether or not it was ever confirmed.
	 *
	 * @param prefixEvents the prefix's events
	 * @param attributeValues the prefix's attribute values, by (1-based) event number; missing timestamps are added to it
	 * @param time the timestamp given to events without one, truncated to seconds
	 * @return every event's timestamp, in prefix order
	 */
	private static List<ZonedDateTime> fillMissingTimestamps(List<String> prefixEvents, Map<Integer, Map<String, String>> attributeValues, ZonedDateTime time) {
		ZonedDateTime fallbackTime = time.truncatedTo(ChronoUnit.SECONDS); //The stored format has no fraction of a second
		List<ZonedDateTime> timestamps = new ArrayList<ZonedDateTime>();
		for (int i = 0; i < prefixEvents.size(); i++) {
			Map<String, String> eventAttributeValues = attributeValues.computeIfAbsent(i + 1, n -> new LinkedHashMap<String, String>());
			ZonedDateTime timestamp = parseTimestamp(eventAttributeValues.get("timestamp"));
			if (timestamp == null) {
				ZonedDateTime previousTimestamp = i == 0 ? null : timestamps.get(i - 1);
				timestamp = previousTimestamp != null && previousTimestamp.isAfter(fallbackTime) ? previousTimestamp : fallbackTime;
				eventAttributeValues.put("timestamp", XES_TIMESTAMP_FORMATTER.format(timestamp));
			}
			timestamps.add(timestamp);
		}
		return timestamps;
	}

	/**
	 * Parses a value in the {@code timestamp} attribute format {@link EventCell}'s editor expects, e.g. {@code 2023-08-07T12:00:00}, as a
	 * time in the system's time zone.
	 *
	 * @param value the attribute value, may be {@code null}
	 * @return the timestamp, or {@code null} if the value is missing or invalid
	 */
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

	/**
	 * Finds the prefixes whose continuation can be exported. Only plans from the data-aware planner carry the attribute values (and times) an
	 * exported event needs, unlike a plan carried over from the Data-Agnostic page.
	 *
	 * @return the (0-based) indices of these prefixes, in prefix order
	 */
	private List<Integer> getExportableContinuationIndices() {
		List<Integer> exportableIndices = new ArrayList<Integer>();
		for (int i = 0; i < prefixes.size(); i++) {
			if (prefixes.get(i).planPresent && prefixes.get(i).planAttributeValues != null) {
				exportableIndices.add(i);
			}
		}
		return exportableIndices;
	}

	/**
	 * Enables the Export button once the planner has produced at least one exportable continuation, and disables it again once there are none.
	 */
	private void updateExportButton() {
		exportContinuationButton.setDisable(getExportableContinuationIndices().isEmpty());
	}

	/**
	 * Tells whether a plan step becomes an event of an exported continuation. Only events of the models' alphabet (the Activities pane, i.e.
	 * the selected process specifications) are exported, which excludes resets: the steps the planner added and, if
	 * {@code includePrefixEvents}, the replayed prefix events (sync and skip steps).
	 *
	 * @param step the plan step ({@code action;activity}), split into its action and activity
	 * @param includePrefixEvents whether the prefix events are exported as well
	 * @return {@code true} if the step is exported
	 */
	private boolean isExportedPlanStep(String[] step, boolean includePrefixEvents) {
		if (step.length < 2 || !activityToAttributes.containsKey(step[1].toLowerCase())) {
			return false;
		}
		return step[0].equals(EnhspPlanParser.ADD_ACTION)
				|| (includePrefixEvents && (step[0].equals(EnhspPlanParser.SYNC_ACTION) || step[0].equals(EnhspPlanParser.SKIP_ACTION)));
	}

	/**
	 * Handles the Export button: lets the user pick the continuations to export, and whether to include the prefix events, then writes them
	 * to an XES log the user chooses, one trace per prefix (see {@link #writeContinuationsAsXes(File, List, boolean)}).
	 */
	@FXML
	private void onClickExportContinuations() {
		if (selectedEntry != null) {
			syncOutToEntry(selectedEntry);
		}
		List<Integer> exportableIndices = getExportableContinuationIndices();
		if (exportableIndices.isEmpty()) { //The button is disabled in this case, see updateExportButton
			return;
		}
		if (activityToAttributes.isEmpty()) {
			AlertUtils.showWarning("Select the process specifications whose activities should be exported under Process Specifications.");
			return;
		}

		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle("Export Continuations");
		dialog.setHeaderText("Select the continuations to export as XES");
		dialog.initOwner(getStage());
		dialog.getDialogPane().getStylesheets().add("/org/framedinterface/dialogPane.css");
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		CheckBox allCheckBox = new CheckBox("All continuations");
		allCheckBox.setSelected(true);
		List<CheckBox> prefixCheckBoxes = new ArrayList<CheckBox>();
		for (int index : exportableIndices) {
			long addedEvents = prefixes.get(index).plan.stream().filter(step -> isExportedPlanStep(step.split(";", 2), false)).count();
			CheckBox prefixCheckBox = new CheckBox("Prefix " + (index + 1) + " (" + addedEvents + " added event" + (addedEvents == 1 ? "" : "s") + ")");
			prefixCheckBox.setSelected(true);
			prefixCheckBox.setOnAction(event -> allCheckBox.setSelected(prefixCheckBoxes.stream().allMatch(CheckBox::isSelected)));
			prefixCheckBoxes.add(prefixCheckBox);
		}
		allCheckBox.setOnAction(event -> prefixCheckBoxes.forEach(prefixCheckBox -> prefixCheckBox.setSelected(allCheckBox.isSelected())));
		CheckBox includePrefixCheckBox = new CheckBox("Include the prefix events (complete traces), not only the added events");
		includePrefixCheckBox.setSelected(true);

		VBox prefixList = new VBox(4);
		prefixList.getChildren().addAll(prefixCheckBoxes);
		ScrollPane prefixScrollPane = new ScrollPane(prefixList);
		prefixScrollPane.setMaxHeight(300);
		prefixScrollPane.setFitToWidth(true);
		VBox content = new VBox(8, allCheckBox, new Separator(), prefixScrollPane, new Separator(), includePrefixCheckBox);
		content.setPadding(new Insets(10));
		dialog.getDialogPane().setContent(content);
		dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(Bindings.createBooleanBinding(
				() -> prefixCheckBoxes.stream().noneMatch(CheckBox::isSelected),
				prefixCheckBoxes.stream().map(CheckBox::selectedProperty).toArray(Observable[]::new)));

		if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
			return;
		}
		List<Integer> chosenIndices = new ArrayList<Integer>();
		for (int i = 0; i < exportableIndices.size(); i++) {
			if (prefixCheckBoxes.get(i).isSelected()) {
				chosenIndices.add(exportableIndices.get(i));
			}
		}

		File defaultFolder = new File(currentPath, CONTINUATION_OUTPUT_FOLDER);
		defaultFolder.mkdirs();
		File file = FileUtils.showXesSaveDialog(getStage(), defaultFolder, "continuations_" + LocalDateTime.now().format(PREFIX_FILE_TIMESTAMP_FORMATTER) + ".xes");
		if (file == null) {
			return;
		}
		try {
			writeContinuationsAsXes(file, chosenIndices, includePrefixCheckBox.isSelected());
			AlertUtils.showSuccess("Exported " + chosenIndices.size() + " continuation(s) to:\n" + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			AlertUtils.showError("Could not export the continuations to XES: " + e.getMessage());
		}
	}

	/**
	 * Writes one trace per given prefix (named {@code Prefix <i>}) to an XES log: the events its plan added and, if
	 * {@code includePrefixEvents}, the prefix events it replayed, restricted to the models' alphabet (see
	 * {@link #isExportedPlanStep(String[], boolean)}). Categorical attributes are written as strings and numeric ones as floats, e.g.
	 * {@code <string key="categorical" value="c2"/>} and {@code <float key="integer" value="8"/>}. Each event's time is its plan time.
	 *
	 * @param file the XES file to write
	 * @param prefixIndices the (0-based) indices of the prefixes to export; each must have a continuation from the data-aware planner
	 * @param includePrefixEvents whether to include the prefix events (complete traces), not only the added events
	 * @throws IOException if the file can't be written
	 */
	private void writeContinuationsAsXes(File file, List<Integer> prefixIndices, boolean includePrefixEvents) throws IOException {
		Set<String> categoricalAttributes = getCategoricalAttributes(new ArrayList<AbstractModel>(ModelRegistry.getInstance().getModels()));

		try (XesWriter xes = new XesWriter(file, "Continuations")) {
			for (int index : prefixIndices) {
				PrefixEntry entry = prefixes.get(index);
				xes.startTrace("Prefix " + (index + 1));

				for (int i = 0; i < entry.plan.size(); i++) {
					String[] step = entry.plan.get(i).split(";", 2);
					if (!isExportedPlanStep(step, includePrefixEvents)) {
						continue;
					}
					Map<String, String> eventAttributeValues = i < entry.planAttributeValues.size() ? entry.planAttributeValues.get(i) : Collections.emptyMap();

					xes.startEvent(getActivityNameAsDefined(step[1]));
					for (Map.Entry<String, String> attributeValue : eventAttributeValues.entrySet()) {
						String value = attributeValue.getValue() == null ? "" : attributeValue.getValue().strip();
						if (!attributeValue.getKey().equals("timestamp") && !value.isEmpty()) {
							String valueType = getXesAttributeType(value);
							boolean isNumeric = !categoricalAttributes.contains(attributeValue.getKey().toLowerCase()) && (valueType.equals("int") || valueType.equals("float"));
							xes.writeAttribute(isNumeric ? "float" : "string", attributeValue.getKey(), value);
						}
					}
					ZonedDateTime timestamp = parseTimestamp(eventAttributeValues.get("timestamp"));
					if (timestamp != null) {
						xes.writeTimestamp(timestamp);
					}
					xes.endElement();
				}
				xes.endElement();
			}
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Determines the XES attribute type matching how a value was typed in; the inverse of {@link #extractAttributeValueString(XAttribute)}.
	 *
	 * @param value the attribute value
	 * @return the XES attribute type (element name): {@code int}, {@code float}, {@code boolean} or {@code string}
	 */
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

	/**
	 * Lets the given pane compete for the column's leftover space only while it is expanded, so that a collapsed pane only takes up its title
	 * bar's height.
	 *
	 * @param pane the pane
	 */
	private void bindPaneGrowToExpanded(TitledPane pane) {
		VBox.setVgrow(pane, pane.isExpanded() ? Priority.ALWAYS : Priority.NEVER);
		pane.expandedProperty().addListener((observable, oldValue, expanded) -> VBox.setVgrow(pane, expanded ? Priority.ALWAYS : Priority.NEVER));
	}

	/**
	 * Handles the Expand All button: expands the specification panes on the left and every row within them (models, activities, their
	 * groups, attributes).
	 */
	@FXML
	private void onClickExpandAll() {
		setSpecificationPanesExpanded(true);
	}

	/**
	 * Handles the Collapse All button: collapses the specification panes on the left and every row within them, so that the rows stay
	 * collapsed when the panes are expanded again.
	 */
	@FXML
	private void onClickCollapseAll() {
		setSpecificationPanesExpanded(false);
	}

	/**
	 * Expands or collapses the specification panes on the left (Process Specifications, Constraints, Activities, Attributes) and every row
	 * within them.
	 *
	 * @param expanded {@code true} to expand, {@code false} to collapse
	 */
	private void setSpecificationPanesExpanded(boolean expanded) {
		for (TitledPane pane : new TitledPane[] {modelRegistryPane, constraintsPane, activitiesAttributesPane, attributesPane}) {
			pane.setExpanded(expanded);
		}
		setRowsExpanded(constraintsTreeTableView.getRoot(), expanded);
		setRowsExpanded(activitiesTreeTableView.getRoot(), expanded);
		setRowsExpanded(attributesTreeView.getRoot(), expanded);
	}

	/**
	 * Expands or collapses every row below the given root that has children, recursively. The (hidden) root itself stays expanded, otherwise
	 * none of its rows would be shown at all.
	 *
	 * @param root the tree's root, may be {@code null}
	 * @param expanded {@code true} to expand, {@code false} to collapse
	 */
	private static void setRowsExpanded(TreeItem<?> root, boolean expanded) {
		if (root == null) {
			return;
		}
		for (TreeItem<?> child : root.getChildren()) {
			if (!child.isLeaf()) {
				child.setExpanded(expanded);
				setRowsExpanded(child, expanded);
			}
		}
	}

	/**
	 * Returns whether the given process specification is checked in the Process Specifications pane, as the property its checkbox is bound
	 * to. It is created unchecked on first access; checking or unchecking it refreshes the specification panes.
	 *
	 * @param model the process specification
	 * @return the model's selection state
	 */
	private BooleanProperty getModelSelectedProperty(AbstractModel model) {
		return modelSelectionState.computeIfAbsent(model, m -> {
			BooleanProperty selected = new SimpleBooleanProperty(false);
			selected.addListener((observable, oldValue, newValue) -> updateActivitiesListView());
			return selected;
		});
	}

	/**
	 * Refreshes the Activities pane with the unique (case-insensitive) activities of the selected process specifications: each with its cost,
	 * its bound attributes (Declare models only) and the models containing it. Also refreshes {@link #activityToAttributes}, whether the
	 * attribute editor shows a timestamp field, and the Constraints and Attributes panes.
	 */
	private void updateActivitiesListView() {
		activityToAttributes.clear();
		Map<String, List<AbstractModel>> activityToModels = new HashMap<String, List<AbstractModel>>();
		timestampFieldVisible = false;
		for (AbstractModel model : getSelectedModels()) {
			for (String activity : model.getActivities()) {
				String activityLabel = activity.toLowerCase();
				activityToModels.computeIfAbsent(activityLabel, a -> new ArrayList<AbstractModel>()).add(model);
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

		TreeItem<ActivityEntry> activitiesRoot = new TreeItem<ActivityEntry>();
		for (Map.Entry<String, Set<String>> entry : activityToAttributes.entrySet()) {
			String activity = entry.getKey();
			List<AbstractModel> models = activityToModels.get(activity);
			String activityName = getActivityNameAsDefined(activity); //As spelled in the model files, which is what the planner works with
			TreeItem<ActivityEntry> activityItem = new TreeItem<ActivityEntry>(new ActivityEntry(activityName, null, getActivityCost(activity)));

			if (!entry.getValue().isEmpty()) {
				TreeItem<ActivityEntry> attributesItem = new TreeItem<ActivityEntry>(new ActivityEntry("Attributes", null, null));
				for (String attribute : entry.getValue()) {
					attributesItem.getChildren().add(new TreeItem<ActivityEntry>(new ActivityEntry(attribute, null, null)));
				}
				keepExpansionState(attributesItem, collapsedActivityRows, activity + "/attributes");
				activityItem.getChildren().add(attributesItem);
			}
			TreeItem<ActivityEntry> modelsItem = new TreeItem<ActivityEntry>(new ActivityEntry("Models", null, null));
			for (AbstractModel model : models) {
				modelsItem.getChildren().add(new TreeItem<ActivityEntry>(new ActivityEntry(model.getModelName(), model.getFilePath(), null)));
			}
			keepExpansionState(modelsItem, collapsedActivityRows, activity + "/models");
			activityItem.getChildren().add(modelsItem);

			keepExpansionState(activityItem, collapsedActivityRows, activity);
			activitiesRoot.getChildren().add(activityItem);
		}
		activitiesTreeTableView.setRoot(activitiesRoot); //An empty root shows the placeholder

		planListView.refresh(); //Already-rendered prefix events need to re-check attribute availability now that the selected models (and thus known attributes) may have changed
		updateConstraintsTableView();
		updateAttributesTreeView();
	}

	/**
	 * Refreshes the Constraints pane with the constraints of the selected process specifications: one collapsible row per model (in model
	 * order), holding its constraints in file order or, for a Petri net, a single row for the net itself.
	 */
	private void updateConstraintsTableView() {
		TreeItem<ConstraintEntry> constraintsRoot = new TreeItem<ConstraintEntry>();
		for (AbstractModel model : getSelectedModels()) {
			TreeItem<ConstraintEntry> modelItem = new TreeItem<ConstraintEntry>(ConstraintEntry.modelRow(model));
			if (model instanceof DeclareModel) {
				for (DeclareConstraint constraint : ((DeclareModel) model).getDeclareConstraints()) {
					modelItem.getChildren().add(new TreeItem<ConstraintEntry>(new ConstraintEntry(model, constraint, getViolationCost(constraint), getHardProperty(constraint))));
				}
			} else if (model instanceof PnModel) {
				modelItem.getChildren().add(new TreeItem<ConstraintEntry>(new ConstraintEntry(model, null, getViolationCost(model), getHardProperty(model))));
			}
			modelItem.setExpanded(!collapsedConstraintModels.contains(model));
			modelItem.expandedProperty().addListener((observable, oldValue, expanded) -> {
				if (expanded) {
					collapsedConstraintModels.remove(model);
				} else {
					collapsedConstraintModels.add(model);
				}
			});
			constraintsRoot.getChildren().add(modelItem);
		}
		constraintsTreeTableView.setRoot(constraintsRoot); //An empty root shows the placeholder
	}

	/**
	 * Makes the given column show and edit the cost of each row that has one. Enter confirms an edit; input that isn't a non-negative number
	 * is rejected, and the previous cost is shown again. Rows for which {@code isLocked} holds show their cost greyed out, and it can't be
	 * edited.
	 *
	 * @param <T> the type of the tree's rows
	 * @param treeTableView the tree the column belongs to
	 * @param costColumn the column
	 * @param costOf the cost of a row, or {@code null} for rows without one
	 * @param isLocked whether a row's cost is locked
	 */
	private static <T> void setupCostColumn(TreeTableView<T> treeTableView, TreeTableColumn<T, Double> costColumn, Function<T, DoubleProperty> costOf, Predicate<T> isLocked) {
		costColumn.setCellValueFactory(data -> {
			DoubleProperty cost = data.getValue().getValue() == null ? null : costOf.apply(data.getValue().getValue());
			return cost == null ? new ReadOnlyObjectWrapper<Double>(null) : cost.asObject();
		});
		costColumn.setCellFactory(column -> new TextFieldTreeTableCell<T, Double>(COST_CONVERTER) {
			@Override
			public void startEdit() {
				T rowValue = getRowValue();
				if (rowValue != null && costOf.apply(rowValue) != null && !isLocked.test(rowValue)) {
					super.startEdit();
				}
			}
			@Override
			public void updateItem(Double item, boolean empty) {
				super.updateItem(item, empty);
				T rowValue = getRowValue();
				pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, !empty && rowValue != null && isLocked.test(rowValue));
			}
			private T getRowValue() {
				TreeItem<T> treeItem = getTreeTableRow() == null ? null : getTreeTableRow().getTreeItem();
				return treeItem == null ? null : treeItem.getValue();
			}
		});
		costColumn.setOnEditCommit(event -> {
			DoubleProperty cost = costOf.apply(event.getRowValue().getValue());
			if (event.getNewValue() != null && cost != null) {
				cost.set(event.getNewValue());
			}
			treeTableView.refresh();
		});
	}

	/**
	 * Makes the Hard column of the Constraints pane show a checkbox for each constraint (and Petri net), but none for the model rows.
	 */
	private void setupHardColumn() {
		constraintHardColumn.setCellValueFactory(data -> {
			BooleanProperty hard = data.getValue().getValue() == null ? null : data.getValue().getValue().hard;
			return hard == null ? new ReadOnlyObjectWrapper<Boolean>(null) : hard;
		});
		constraintHardColumn.setCellFactory(column -> new CheckBoxTreeTableCell<ConstraintEntry, Boolean>() {
			@Override
			public void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null) {
					setGraphic(null);
				}
			}
		});
	}

	/**
	 * Adds context menu entries to the Constraints pane that mark the selected constraints as hard or soft at once (a selected model row
	 * stands for all of its constraints). Must be called after {@link #setupBulkCostEditing(TreeTableView, Function, String, String)},
	 * which creates the context menu.
	 */
	private void setupBulkHardEditing() {
		MenuItem markHardItem = new MenuItem("Mark Selected Constraints as Hard");
		markHardItem.setOnAction(event -> getRowProperties(constraintsTreeTableView, entry -> entry.hard, true).forEach(hard -> hard.set(true)));
		MenuItem markSoftItem = new MenuItem("Mark Selected Constraints as Soft");
		markSoftItem.setOnAction(event -> getRowProperties(constraintsTreeTableView, entry -> entry.hard, true).forEach(hard -> hard.set(false)));
		markHardItem.disableProperty().bind(Bindings.isEmpty(constraintsTreeTableView.getSelectionModel().getSelectedItems()));
		markSoftItem.disableProperty().bind(markHardItem.disableProperty());
		constraintsTreeTableView.getContextMenu().getItems().addAll(new SeparatorMenuItem(), markHardItem, markSoftItem);
	}

	/**
	 * Handles the Set Cost... button of the Constraints pane: sets the cost of the selected constraints, or of all constraints if none are
	 * selected (hard constraints excluded).
	 */
	@FXML
	private void onClickSetConstraintCosts() {
		promptCostsOfSelectionOrAll(constraintsTreeTableView, ConstraintEntry::getEditableViolationCost, "constraint", "constraints");
	}

	/**
	 * Handles the Set Cost... button of the Activities pane: sets the cost of the selected activities, or of all activities if none are
	 * selected.
	 */
	@FXML
	private void onClickSetActivityCosts() {
		promptCostsOfSelectionOrAll(activitiesTreeTableView, entry -> entry.cost, "activity", "activities");
	}

	/**
	 * Lets several rows of the given tree be selected, and adds a context menu to set the cost of the selected items, or of all items, at once.
	 *
	 * @param <T> the type of the tree's rows
	 * @param treeTableView the tree
	 * @param costOf the editable cost of a row, or {@code null} for rows without one
	 * @param itemName the name of one item in the menu and dialog, e.g. "constraint"
	 * @param itemsName the name of several items, e.g. "constraints"
	 */
	private <T> void setupBulkCostEditing(TreeTableView<T> treeTableView, Function<T, DoubleProperty> costOf, String itemName, String itemsName) {
		treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		String capitalizedItems = Character.toUpperCase(itemsName.charAt(0)) + itemsName.substring(1);
		MenuItem setSelectedCostsItem = new MenuItem("Set Cost of Selected " + capitalizedItems + "...");
		setSelectedCostsItem.setOnAction(event -> promptCosts(treeTableView, getRowProperties(treeTableView, costOf, true), "the selected", itemName, itemsName));
		MenuItem setAllCostsItem = new MenuItem("Set Cost of All " + capitalizedItems + "...");
		setAllCostsItem.setOnAction(event -> promptCosts(treeTableView, getRowProperties(treeTableView, costOf, false), "all", itemName, itemsName));
		ContextMenu contextMenu = new ContextMenu(setSelectedCostsItem, setAllCostsItem);
		contextMenu.setOnShowing(event -> setSelectedCostsItem.setDisable(treeTableView.getSelectionModel().getSelectedItems().isEmpty()));
		treeTableView.setContextMenu(contextMenu);
	}

	/**
	 * Asks for a cost and sets it for the selected rows, or for all rows if none are selected. This is decided by the selected rows rather
	 * than by their costs, so that selecting only rows without an editable cost (hard constraints) doesn't fall back to all rows.
	 *
	 * @param <T> the type of the tree's rows
	 * @param treeTableView the tree
	 * @param costOf the editable cost of a row, or {@code null} for rows without one
	 * @param itemName the name of one item in the dialog, e.g. "constraint"
	 * @param itemsName the name of several items, e.g. "constraints"
	 */
	private <T> void promptCostsOfSelectionOrAll(TreeTableView<T> treeTableView, Function<T, DoubleProperty> costOf, String itemName, String itemsName) {
		if (treeTableView.getSelectionModel().getSelectedItems().isEmpty()) {
			promptCosts(treeTableView, getRowProperties(treeTableView, costOf, false), "all", itemName, itemsName);
		} else {
			promptCosts(treeTableView, getRowProperties(treeTableView, costOf, true), "the selected", itemName, itemsName);
		}
	}

	/**
	 * Collects the properties (e.g. costs) of the selected rows, or of all rows, each once. A row without one of its own stands for the
	 * nearest ancestor that has one (e.g. an activity's attribute and model rows) or, failing that, for all descendants that have one (e.g. a
	 * model row for its constraints).
	 *
	 * @param <T> the type of the tree's rows
	 * @param <P> the type of the properties
	 * @param treeTableView the tree
	 * @param propertyOf the property of a row, or {@code null} for rows without one
	 * @param selectedOnly {@code true} for the selected rows, {@code false} for all rows
	 * @return the properties, in row order, without duplicates
	 */
	private static <T, P> List<P> getRowProperties(TreeTableView<T> treeTableView, Function<T, P> propertyOf, boolean selectedOnly) {
		List<TreeItem<T>> rows = new ArrayList<TreeItem<T>>();
		if (selectedOnly) {
			rows.addAll(treeTableView.getSelectionModel().getSelectedItems());
		} else if (treeTableView.getRoot() != null) {
			rows.addAll(treeTableView.getRoot().getChildren());
		}
		Set<P> properties = new LinkedHashSet<P>(); //Identity-based (properties don't override equals)
		for (TreeItem<T> row : rows) {
			P ownOrAncestorProperty = null;
			for (TreeItem<T> item = row; item != null && ownOrAncestorProperty == null; item = item.getParent()) {
				ownOrAncestorProperty = item.getValue() == null ? null : propertyOf.apply(item.getValue());
			}
			if (ownOrAncestorProperty != null) {
				properties.add(ownOrAncestorProperty);
			} else if (row != null) {
				addDescendantProperties(row, propertyOf, properties);
			}
		}
		return new ArrayList<P>(properties);
	}

	/**
	 * Adds the properties of the given row's descendants to {@code properties}: each child's own property or, for a child without one, its
	 * descendants' properties.
	 *
	 * @param <T> the type of the tree's rows
	 * @param <P> the type of the properties
	 * @param item the row
	 * @param propertyOf the property of a row, or {@code null} for rows without one
	 * @param properties the properties collected so far
	 */
	private static <T, P> void addDescendantProperties(TreeItem<T> item, Function<T, P> propertyOf, Set<P> properties) {
		for (TreeItem<T> child : item.getChildren()) {
			P property = child.getValue() == null ? null : propertyOf.apply(child.getValue());
			if (property != null) {
				properties.add(property);
			} else {
				addDescendantProperties(child, propertyOf, properties);
			}
		}
	}

	/**
	 * Asks for a cost, pre-filled if the given costs are currently equal, and sets all of them to it. Shows a warning instead if there are no
	 * costs to set.
	 *
	 * @param treeTableView the tree whose costs are set, refreshed afterwards
	 * @param costs the costs to set
	 * @param scope the scope shown in the prompt, e.g. "the selected" or "all"
	 * @param itemName the name of one item, e.g. "constraint"
	 * @param itemsName the name of several items, e.g. "constraints"
	 */
	private void promptCosts(TreeTableView<?> treeTableView, List<DoubleProperty> costs, String scope, String itemName, String itemsName) {
		if (costs.isEmpty()) {
			boolean noRows = treeTableView.getRoot() == null || treeTableView.getRoot().getChildren().isEmpty();
			AlertUtils.showWarning(noRows ? "There are no " + itemsName + " to set the cost of. Select process specifications under Process Specifications first."
					: "Hard constraints have no cost to set, and " + scope + " " + itemsName + " are hard."); //Every activity has a cost, so only constraints get here
			return;
		}
		double firstCost = costs.get(0).get();
		boolean sharedCost = costs.stream().allMatch(cost -> cost.get() == firstCost);

		TextInputDialog dialog = new TextInputDialog(sharedCost ? COST_CONVERTER.toString(firstCost) : "");
		dialog.setTitle("Set " + Character.toUpperCase(itemName.charAt(0)) + itemName.substring(1) + " Costs");
		dialog.setHeaderText("Set the cost of " + scope + " " + costs.size() + " " + (costs.size() == 1 ? itemName : itemsName)
				+ (sharedCost ? "" : System.lineSeparator() + "(their costs currently differ)"));
		dialog.setContentText("Cost:");
		dialog.initOwner(getStage());
		dialog.getDialogPane().getStylesheets().add("/org/framedinterface/dialogPane.css");
		dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(Bindings.createBooleanBinding(
				() -> COST_CONVERTER.fromString(dialog.getEditor().getText()) == null, dialog.getEditor().textProperty())); //Only non-negative numbers

		dialog.showAndWait().map(COST_CONVERTER::fromString).ifPresent(newCost -> {
			costs.forEach(cost -> cost.set(newCost));
			treeTableView.refresh();
		});
	}

	/**
	 * Builds the cost model handed to the PDDL generator: one {@code <ActivityName> <activityCost> 0} line per activity of the Activities
	 * pane (i.e. of the selected process specifications), spelled as in the model files, since the generator matches activity names
	 * case-sensitively. Every activity costs 0 unless Activity Cost is an optimization target; the costs set in the Activities pane are kept
	 * for when it is one again.
	 *
	 * @return the cost model's lines
	 */
	private List<String> buildCostModelLines() {
		boolean activityCostOptimized = optimizationTargets.contains(OptimizationTarget.ACTIVITY_COST);
		List<String> lines = new ArrayList<String>();
		for (String activity : activityToAttributes.keySet()) {
			lines.add(getActivityNameAsDefined(activity) + " " + (activityCostOptimized ? COST_CONVERTER.toString(getActivityCost(activity).get()) : "0") + " 0");
		}
		return lines;
	}

	/**
	 * Builds the violation costs handed to the PDDL generator, from the Constraints pane: {@code <model file>|<constraint name> <cost>} per
	 * Declare constraint (named as the generator names it, see {@link #getPlannerConstraintNames(List)}) and {@code <model file> <cost>} per
	 * Petri net, with {@link #HARD_CONSTRAINT_COST} as the cost of hard ones.
	 *
	 * @param plannedModels the models handed to the generator, in the order they are passed to it
	 * @return the lines of the violation cost file
	 */
	private List<String> buildViolationCostLines(List<AbstractModel> plannedModels) {
		Map<DeclareConstraint, String> constraintNames = getPlannerConstraintNames(plannedModels);
		List<String> lines = new ArrayList<String>();
		for (AbstractModel model : plannedModels) {
			String modelFileName = Paths.get(model.getFilePath()).getFileName().toString();
			if (model instanceof DeclareModel) {
				for (DeclareConstraint constraint : ((DeclareModel) model).getDeclareConstraints()) {
					lines.add(modelFileName + "|" + constraintNames.get(constraint) + " " + getViolationCostString(constraint));
				}
			} else if (model instanceof PnModel) {
				lines.add(modelFileName + " " + getViolationCostString(model));
			}
		}
		return lines;
	}

	/**
	 * Collects the violation cost of each soft Declare constraint and Petri net of the given models, as
	 * {@link #buildViolationCostLines(List)} hands them to the planner. Hard ones are left out, as they have no cost.
	 *
	 * @param models the models
	 * @return the costs, keyed by the {@link DeclareConstraint} or {@link PnModel} itself (identity-keyed)
	 */
	private Map<Object, BigDecimal> getSoftViolationCosts(List<AbstractModel> models) {
		List<Object> constraintsAndPetriNets = new ArrayList<Object>();
		for (AbstractModel model : models) {
			if (model instanceof DeclareModel) {
				constraintsAndPetriNets.addAll(((DeclareModel) model).getDeclareConstraints());
			} else if (model instanceof PnModel) {
				constraintsAndPetriNets.add(model);
			}
		}
		Map<Object, BigDecimal> costs = new IdentityHashMap<Object, BigDecimal>();
		for (Object constraintOrPetriNet : constraintsAndPetriNets) {
			if (!getHardProperty(constraintOrPetriNet).get()) {
				costs.put(constraintOrPetriNet, BigDecimal.valueOf(getViolationCost(constraintOrPetriNet).get()));
			}
		}
		return costs;
	}

	/**
	 * Describes the given prefix's plan cost split into its violation, activity and waiting cost, shown as the plan cost's tooltip. Must be
	 * called right after the models were replayed against the plan (see {@link #updateTrace(Integer)}), since the violation cost of a
	 * Discard plan is taken from that replay.
	 *
	 * @param entry the prefix
	 * @return the description, or {@code null} for a plan that wasn't loaded from the data-aware planner
	 */
	private String describePlanCost(PrefixEntry entry) {
		EnhspPlanParser.ParsedPlan plan = entry.parsedPlan;
		if (plan == null) {
			return null;
		}
		BigDecimal violationCost = plan.getViolationCost();
		if (violationCost == null && planRecoveryStrategy == RecoveryStrategy.DISCARD) {
			violationCost = getReplayedViolationCost();
		}
		if (violationCost != null && !plan.costPartsAddUp(violationCost)) {
			System.out.println("DataAwareController: the cost parts of prefix " + (prefixes.indexOf(entry) + 1) + "'s plan (violation " + EnhspPlanParser.formatCost(violationCost)
					+ ", activity " + EnhspPlanParser.formatCost(plan.getActivityCost()) + ", waiting " + EnhspPlanParser.formatCost(plan.getWaitingCost())
					+ ") don't add up to its plan cost " + entry.planCost);
		}
		return "Violation cost: " + (violationCost == null ? "not available for " + planRecoveryStrategy : EnhspPlanParser.formatCost(violationCost))
				+ System.lineSeparator() + "Activity cost: " + EnhspPlanParser.formatCost(plan.getActivityCost())
				+ System.lineSeparator() + "Waiting cost: " + EnhspPlanParser.formatCost(plan.getWaitingCost());
	}

	/**
	 * Sums the violation costs the planner was given ({@link #planViolationCosts}) of what the last replay of the models left violated at the
	 * end of the trace: Declare constraints whose final monitoring state is {@link MonitoringState#VIOL}, and Petri nets in which some
	 * activity occurred while its transition wasn't enabled. Under the Discard recovery strategy, each constraint or net is charged once,
	 * however often it is violated.
	 *
	 * @return the violation cost of the replayed trace
	 */
	private BigDecimal getReplayedViolationCost() {
		BigDecimal violationCost = BigDecimal.ZERO;
		for (AbstractModel model : ModelRegistry.getInstance().getModels()) {
			if (model instanceof DeclareModel) {
				for (DeclareConstraint constraint : ((DeclareModel) model).getViolatedConstraintsAtTraceEnd()) {
					violationCost = violationCost.add(planViolationCosts.getOrDefault(constraint, BigDecimal.ZERO)); //0 if not planned with (or hard)
				}
			} else if (model instanceof PnModel && ((PnModel) model).isViolated()) {
				violationCost = violationCost.add(planViolationCosts.getOrDefault(model, BigDecimal.ZERO));
			}
		}
		return violationCost;
	}

	/**
	 * Returns the violation cost of a Declare constraint or Petri net as written to the violation cost file.
	 *
	 * @param constraintOrPetriNet a {@link DeclareConstraint}, or the {@link PnModel} of a Petri net
	 * @return the cost, or {@link #HARD_CONSTRAINT_COST} for a hard constraint
	 */
	private String getViolationCostString(Object constraintOrPetriNet) {
		return getHardProperty(constraintOrPetriNet).get() ? HARD_CONSTRAINT_COST : COST_CONVERTER.toString(getViolationCost(constraintOrPetriNet).get());
	}

	/**
	 * Returns the cost of an activity, which is {@link #DEFAULT_ACTIVITY_COST} until edited in the Activities pane.
	 *
	 * @param activity the activity, in any casing (costs are kept case-insensitively)
	 * @return the activity's cost, as the property the Activities pane edits
	 */
	private DoubleProperty getActivityCost(String activity) {
		return activityCosts.computeIfAbsent(activity.toLowerCase(), key -> new SimpleDoubleProperty(DEFAULT_ACTIVITY_COST));
	}

	/**
	 * Restores whether the user had collapsed the given row, and keeps track of it from now on, so that it survives the tree being rebuilt.
	 *
	 * @param item the row
	 * @param collapsedKeys the keys of the collapsed rows, updated whenever the row is expanded or collapsed
	 * @param key the row's key
	 */
	private static void keepExpansionState(TreeItem<?> item, Set<String> collapsedKeys, String key) {
		item.setExpanded(!collapsedKeys.contains(key));
		item.expandedProperty().addListener((observable, oldValue, expanded) -> {
			if (expanded) {
				collapsedKeys.remove(key);
			} else {
				collapsedKeys.add(key);
			}
		});
	}

	/**
	 * Returns the violation cost of a Declare constraint or Petri net, which is {@link #DEFAULT_VIOLATION_COST} until edited in the
	 * Constraints pane.
	 *
	 * @param constraintOrPetriNet a {@link DeclareConstraint}, or the {@link PnModel} of a Petri net
	 * @return the violation cost, as the property the Constraints pane edits
	 */
	private DoubleProperty getViolationCost(Object constraintOrPetriNet) {
		return violationCosts.computeIfAbsent(constraintOrPetriNet, key -> new SimpleDoubleProperty(DEFAULT_VIOLATION_COST));
	}

	/**
	 * Returns whether a Declare constraint or Petri net is hard, i.e. may not be violated. It is soft until marked as hard in the Constraints
	 * pane.
	 *
	 * @param constraintOrPetriNet a {@link DeclareConstraint}, or the {@link PnModel} of a Petri net
	 * @return the hard flag, as the property the Constraints pane edits
	 */
	private BooleanProperty getHardProperty(Object constraintOrPetriNet) {
		return hardConstraints.computeIfAbsent(constraintOrPetriNet, key -> {
			BooleanProperty hard = new SimpleBooleanProperty(false);
			hard.addListener(observable -> constraintsTreeTableView.refresh()); //Greys out / re-enables the constraint's cost cell
			return hard;
		});
	}

	//Shows whole costs without a fraction ("1" rather than "1.0"); unparseable or negative input yields null, which rejects the edit
	private static final StringConverter<Double> COST_CONVERTER = new StringConverter<Double>() {
		@Override
		public String toString(Double cost) {
			if (cost == null) {
				return "";
			}
			return cost == Math.rint(cost) ? Long.toString(cost.longValue()) : cost.toString();
		}
		@Override
		public Double fromString(String string) {
			try {
				double cost = Double.parseDouble(string.strip());
				return cost >= 0 && Double.isFinite(cost) ? cost : null;
			} catch (NumberFormatException | NullPointerException e) {
				return null;
			}
		}
	};

	/**
	 * Refreshes the Attributes pane with the declared value range or value set of each attribute, merged across the selected Declare models
	 * (widest range, union of values).
	 */
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
			keepExpansionState(attributeItem, collapsedAttributeRows, entry.getKey().toLowerCase());
			attributeItem.getChildren().add(new TreeItem<String>(entry.getValue().describe()));
			attributesRoot.getChildren().add(attributeItem);
		}
		attributesTreeView.setRoot(attributesRoot);
	}

	/**
	 * Returns the attributes bound to the given activity by the selected Declare models, for the attribute editor of the Prefix pane.
	 *
	 * @param activityName the activity, in any casing, may be {@code null}
	 * @return the lowercased attribute names; empty if there are none
	 */
	private Set<String> getAttributesForActivity(String activityName) {
		return activityToAttributes.getOrDefault(activityName == null ? "" : activityName.toLowerCase(), Collections.emptySet());
	}

	/**
	 * Returns the process specifications checked in the Process Specifications pane, which are handed to the data-aware planner.
	 *
	 * @return the selected models, in {@link ModelRegistry} order
	 */
	public List<AbstractModel> getSelectedModels() {
		List<AbstractModel> selectedModels = new ArrayList<AbstractModel>();
		for (AbstractModel model : ModelRegistry.getInstance().getModels()) {
			if (getModelSelectedProperty(model).get()) {
				selectedModels.add(model);
			}
		}
		return selectedModels;
	}

	/**
	 * Handles the Reset button: discards the selected prefix's continuation if it has one, which makes the prefix editable again; otherwise
	 * clears the prefix itself.
	 *
	 * @param event the button's action event (unused)
	 */
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

	/**
	 * Removes a single event from the selected prefix, from the Prefix pane's event editor. Only reachable while no plan is present.
	 *
	 * @param eventNumber the (1-based) number of the event to remove
	 */
	private void deletePrefixEvent(int eventNumber) {
		PlannerSession.getInstance().removePrefixEvent(eventNumber);
		updateTrace(null);
		updateSelectedModelVisualizations();
	}

	/**
	 * Replays the trace again once an attribute value is confirmed in the Prefix pane's attribute editor popup. The constraint automata need
	 * to be re-run with the new value right away, rather than waiting for the next prefix event to trigger a replay.
	 *
	 * @param eventNumber the number of the event whose attribute value changed (unused, the whole trace is replayed)
	 */
	private void onAttributeValueChanged(int eventNumber) {
		updateTrace(currentEventIndex.get());
	}

	/**
	 * Appends the clicked activity to the selected prefix, spelled as in the model file. Called when the user clicks on an activity or
	 * transition of a visualized model (see {@link #addGraphClickHandlers(Object)}). Does nothing while a plan is present.
	 *
	 * @param modelId the id of the clicked model
	 * @param activityEncoding the clicked activity's encoding in the model's visualization
	 */
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

	/**
	 * Collects the attribute values attached to each event of the given trace, in order: the selected prefix's while no plan is present,
	 * otherwise the data-aware plan's (prefix events keep their values, added events carry the planner's). A plan carried over from the
	 * Data-Agnostic page has none.
	 *
	 * @param activeTrace the active trace, i.e. the prefix or its plan
	 * @return for each event, its attribute values (attribute name to value), empty for none
	 */
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

	/**
	 * Replays all models against the active trace (the selected prefix's plan if present, otherwise the prefix itself), and refreshes the
	 * Prefix and Continuation lists, the plan cost and the timeline. Declare models also check their data conditions against the events'
	 * attribute values; Petri nets have no data conditions. Prefix events without a timestamp get one first (see
	 * {@link #fillMissingTimestamps(List, Map, ZonedDateTime)}).
	 *
	 * @param selectIndex the timeline position to move to afterwards, or {@code null} to stay at the start of the trace
	 */
	private void updateTrace(Integer selectIndex) {
		//A newly added (or carried over / imported, timestamp-less) prefix event gets its timestamp right away, which is then shown and exported as is
		fillMissingTimestamps(PlannerSession.getInstance().getCurrentPrefix(), PlannerSession.getInstance().getPrefixAttributeValuesMap(), ZonedDateTime.now());
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
		String planCostBreakdown = PlannerSession.getInstance().isPlanPresent() && selectedEntry != null ? describePlanCost(selectedEntry) : null; //After the replay above
		labelCost.setTooltip(planCostBreakdown == null ? null : new Tooltip(planCostBreakdown));
		updateExportButton();
		updateTimelineControls(activeTrace);

		if (selectIndex != null) {
			eventSlider.setValue(selectIndex);
			currentEventIndex.setValue(selectIndex);
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
		}
	}

	/**
	 * Makes arrowing up or down to an event in the given list move the timeline to that event's replayed position. Clicking on an event is
	 * handled by the list's {@link EventCell}s.
	 *
	 * @param listView the Prefix or Continuation list
	 */
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

	/**
	 * Finds the timeline position of the event with the given number in the given list.
	 *
	 * @param listView the Prefix or Continuation list
	 * @param eventNumber the event's number
	 * @return the event's timeline position, or 0 (the start of the trace) if it isn't found
	 */
	private static int getTimelineIndex(ListView<EventData> listView, int eventNumber) {
		for (EventData eventData : listView.getItems()) {
			if (eventData.getEventNumber() == eventNumber) {
				return eventData.getTimelineIndex();
			}
		}
		return 0;
	}

	/**
	 * Selects, in both the Prefix and the Continuation list, the event replayed at the given timeline position, or clears the list's
	 * selection if it has no such event (while a plan is replayed, a prefix event may have no position of its own).
	 *
	 * @param timelineIndex the timeline position
	 */
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

	/**
	 * Moves the timeline to the given position, e.g. when an event is clicked on in the Prefix or Continuation list, and pauses the
	 * animation if it is playing.
	 *
	 * @param selectedIndex the timeline position; ignored if negative (an event without a position of its own)
	 */
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

	/**
	 * Handles the play/pause button of the timeline: starts or pauses the animation that replays the trace event by event.
	 */
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

	/**
	 * Handles the step backward button of the timeline: moves one event back, and stops the animation if it is playing.
	 */
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

	/**
	 * Handles the step forward button of the timeline: moves one event forward, and stops the animation if it is playing.
	 */
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

	/**
	 * Loads the visualization page into the given view and sets up its zoom controls: Ctrl + mouse wheel, the zoom slider and the percentage
	 * field are kept in sync with the view's zoom. Once the page has loaded, this controller is exposed to its JavaScript as {@code app}; the
	 * Petri net view is only set up after the Declare view has loaded. A visualization requested before the page has loaded is shown once
	 * it has (see {@link #updateVisualization(WebView, AbstractModel, ModelType)}).
	 *
	 * @param visualizationWebView the view
	 * @param modelType the type of models the view shows
	 * @param zoomSlider the view's zoom slider
	 * @param zoomSliderValueObject the zoom slider's value, bound to the view's zoom
	 * @param zoomValueField the field showing (and editing) the zoom as a percentage
	 * @param webViewZoomObject the view's zoom
	 */
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

	/**
	 * Sets up the timeline controls below the visualizations: the event slider, and the step buttons, which are disabled at the start and end
	 * of the trace respectively. Moving to another event updates the visualizations and selects the event in the Prefix and Continuation
	 * lists.
	 */
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

	/**
	 * Refreshes the Prefix list to match the given prefix. Without a plan, the prefix itself is replayed, and its events and their attribute
	 * values can be edited. With a plan, each prefix event points at (and is labelled with the action of) the plan step that replayed it, and
	 * the end of the trace has no position of its own.
	 *
	 * @param prefix the selected prefix's events
	 */
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

	/**
	 * Refreshes the Continuation list to show the selected prefix's plan: one entry per replayed step, each described by its action,
	 * attribute values and time (see {@link #describePlanStep(String, Map)}). The list is empty without a plan.
	 */
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

	/**
	 * Returns the name a plan step is shown with. Reset steps carry the violated constraint's PDDL name (or a Petri net's reset activity),
	 * which is shown as the constraint (without its conditions) or the Petri net instead.
	 *
	 * @param action the step's action, e.g. {@code add} or {@link DeclareModel#RESET_CONSTRAINT_ACTION}
	 * @param activity the step's activity
	 * @return the activity for all but reset steps; for a reset step, the constraint or Petri net it resets
	 */
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

	/**
	 * Describes a plan step by its action, attribute values and time, e.g. {@code add [integer=55, categorical=c3] @ 2023-08-07T12:00:00}.
	 *
	 * @param action the step's action
	 * @param attributeValues the step's attribute values, including its {@code timestamp} if it has one
	 * @return the description
	 */
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

	/**
	 * Updates the constraint statistics shown for each event in the Prefix and Continuation lists: how many constraints of the visualized
	 * Declare model are in each monitoring state at the event's timeline position.
	 */
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

	/**
	 * Resets the timeline controls to the start of a trace of the given length, and creates a matching animation of the event slider
	 * (1.5 seconds per event).
	 *
	 * @param activities the active trace
	 */
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

	/**
	 * Updates both the Declare and the Petri net visualization to the current timeline position.
	 */
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

	/**
	 * Shows the given model in the given view, in its state at the current timeline position, or clears the view if there is no model or
	 * nothing to show. If the view's page is still loading, the visualization is shown once it has loaded (see
	 * {@link #setupWebView(WebView, ModelType, Slider, ObjectProperty, TextField, ObjectProperty)}).
	 *
	 * @param visualizationWebView the view
	 * @param abstractModel the model, or {@code null} for none
	 * @param modelType the type of models the view shows
	 */
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

	/**
	 * Called from the visualization page's JavaScript whenever it has rendered a model: makes clicking on an activity (or transition) of the
	 * graph add it to the selected prefix (see {@link #addToTracePrefix(String, String)}).
	 *
	 * @param documentObject the page's DOM {@link Document}
	 */
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

	/**
	 * Handles the Show Past Violations checkbox: switches whether the visualizations show past violations, and updates them.
	 */
	@FXML
	private void switchViolationStrings() {
		PlannerSession.getInstance().setDisplayViolations(bttnDisplayViolations.isSelected());
		updateSelectedModelVisualizations();
	}

	/**
	 * Handles the Help button of the Declare viewer: creates its popup (how to extend the prefix, and the colors of the constraint states)
	 * and makes the button toggle that popup from then on. The popup isn't shown on this first click.
	 */
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

	/**
	 * Creates one entry of a color legend: a colored square followed by its description.
	 *
	 * @param color the color
	 * @param description what the color stands for
	 * @return the entry
	 */
	private HBox createLegendItem(Color color, String description) {
		Rectangle rect = new Rectangle(20, 20, color);
		rect.setStroke(Color.BLACK);
		rect.setStrokeWidth(1);
		Label label = new Label(description);
		HBox item = new HBox(10, rect, label);
		return item;
	}

	/**
	 * Handles the Help button of the Petri net viewer: creates its popup (how to extend the prefix, and the colors of the transition states)
	 * and makes the button toggle that popup from then on. The popup isn't shown on this first click.
	 */
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

	/**
	 * Handles the Help button above the Prefix pane: creates its popup (how prefixes are replayed, what the plan's actions mean, and what
	 * the planner settings and the Reset button do) and makes the button toggle that popup from then on. The popup isn't shown on this first
	 * click.
	 */
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
			new Label("\t iv) \treset constraint: The constraint (or Petri net) was violated and is reset to its initial state (Reset recovery strategy)"),
			new Label(""),
			new Label("The planner settings (gear button next to Run Planner) set what the planner optimizes, the Recovery strategy, which decides how the planner"),
			new Label("handles violated constraints, and the Waiting cost, the cost of waiting one time unit. Its ENHSP tab sets the planner's search strategy, heuristic"),
			new Label("and memory; hover over the button to see the current settings."),
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
