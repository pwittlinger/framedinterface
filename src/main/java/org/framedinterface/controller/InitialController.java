package org.framedinterface.controller;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.framedinterface.model.AbstractModel;
import org.framedinterface.model.DeclareModel;
import org.framedinterface.model.ModelType;
import org.framedinterface.model.PnModel;
import org.framedinterface.task.GeneratePDDLTask;
import org.framedinterface.task.RunPlannerTask;
import org.framedinterface.utils.AlertUtils;
import org.framedinterface.utils.FileUtils;
import org.framedinterface.utils.ModelUtils;
import org.framedinterface.utils.ValidationUtils;
import org.framedinterface.utils.enums.MonitoringState;
import org.kordamp.ikonli.javafx.FontIcon;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.datapetrinets.io.DPNIOException;
import org.processmining.datapetrinets.io.DataPetriNetImporter;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.animation.Animation.Status;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;

import org.framedinterface.event.EventCell;
import org.framedinterface.event.EventData;

import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class InitialController {
	
	@FXML 
	private StackPane rootElement;

    @FXML
    private Button buttonPrefix;
	@FXML
    private Button buttonResetNo;

    @FXML
    private Label labelCost;

	@FXML
	private TableView<AbstractModel> modelTabelView;
	@FXML
	private TableColumn<AbstractModel, String> modelNameColumn;
	@FXML
	private TableColumn<AbstractModel, String> modelTypeColumn;
	@FXML
	private TableColumn<AbstractModel, AbstractModel> modelRemoveColumn;

	@FXML
    //private ListView<String> planListView;
	private ListView<EventData> planListView;

	@FXML
	private ScrollPane resultsPane;

    @FXML
    private Button buttonRunPlanner;

    @FXML
    private SplitPane resultsSplitPane;
    
    @FXML
    private Label curPrefix;

    @FXML
    private Label curPrefix1;

	@FXML
    private Label labelCurrentDomain;

    @FXML
    private TextField textFieldPrefix;

    @FXML
    private Button uploadModel;

    @FXML
    private Font x1;

    @FXML
    private Color x2;

    @FXML
    private Font x3;

    @FXML
    private Color x4;
	@FXML
    private TextField textFieldFinalMarking;
	
    @FXML
    private Label labelFinalMarking;

	@FXML
	private CheckBox initialStatesCheckBox;
	@FXML
	private Slider declZoomSlider;
	@FXML
	private TextField declZoomValueField;
	@FXML
	private ChoiceBox<AbstractModel> declModelChoice;
	@FXML
	private WebView declWebView;
	@FXML
	private Slider pnZoomSlider;
	@FXML
	private TextField pnZoomValueField;
	@FXML
	private ChoiceBox<AbstractModel> pnModelChoice;
	@FXML
	private WebView pnWebView;
	@FXML
	private Label prefixOnlyLabel;
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
	    @FXML
    private CheckBox bttnDisplayViolations;
	    @FXML
    private Label selectedDecl;

    @FXML
    private Label selectedPN;
    @FXML
    private CheckBox buttonResetYes;

    @FXML
    private Pane paneStatus;
	@FXML
    private VBox mainContents;


	private static String precentageFormat = "%.1f";
	private String initialDeclWebViewScript;
	private String initialPnWebViewScript;

	//Storing object references here
	private ObjectProperty<Double> declZoomSliderValueObject;
	private ObjectProperty<Double> pnZoomSliderValueObject;
	private ObjectProperty<Double> declWebViewZoomObject;
	private ObjectProperty<Double> pnWebViewZoomObject;

	private int modelCounter = 0; //For unique model identifiers, needed to reliably determine which model was clicked in the visualization (not an ideal solution)
	private List<String> tracePrefix = new ArrayList<String>(); //Trace prefix entered by the user, starting out with an empty trace

	private Timeline animationTimeline; //Controls eventSlider movement during animation
	private boolean animationInProgress; //Used to allow navigation of events during animation
	private SimpleIntegerProperty currentEventIndex = new SimpleIntegerProperty(0); //Various ui elements listen to this value for updates, also used to determine the model state to visualize
	private FontIcon pauseFontIcon = new FontIcon("fa-pause"); //Icon to be displayed when animation is paused
	private javafx.scene.Node progressLayer;
	boolean progressLayerLoadAttempted = false;


	List<VBox> resultsList;
    private Stage stage;
	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private int defaultViolationCost = 3;
	private String declPath;
	private String petrinetPath;
	private String currentPath;
	private String finMarking;
	private boolean resetDomain;
	private boolean displayViolations;
	private static String framedAutonomyJar = "FramedAutonomyTool.jar";
	private ArrayList<String> currentPlan;
	private ArrayList<String> currentPrefix;
	private boolean planPresent;

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@FXML
	private void initialize() {

		// Initialize variables
		resetDomain = true;
		displayViolations = false;
		planPresent = false;
		currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
		currentPlan = new ArrayList<>();
		currentPrefix = new ArrayList<>();
		
		selectedDecl.setVisible(false);
		selectedDecl.setManaged(false);
		selectedPN.setVisible(false);
		selectedPN.setManaged(false);
		
		modelTabelView.setPlaceholder(new Label("No process specifications selected"));
		modelNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		modelNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelName()));
		modelTypeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		modelTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelType().toString()));
		
		modelRemoveColumn.setCellValueFactory(
				param -> new ReadOnlyObjectWrapper<AbstractModel>(param.getValue())
				);
		modelRemoveColumn.setCellFactory(param -> new TableCell<AbstractModel, AbstractModel>() {
			private final Button removeButton = new Button();
			private FontIcon deleteFontIcon = new FontIcon("fa-trash");
			
			@Override
			protected void updateItem(AbstractModel item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null) {
					setGraphic(null);
					return;
				}
				
				if (!removeButton.getStyleClass().contains("action-cell__button")) {
					removeButton.getStyleClass().add("action-cell__button");
					deleteFontIcon.getStyleClass().add("action-cell__delete-icon");
					removeButton.setGraphic(deleteFontIcon);
				}

				setGraphic(removeButton);
				removeButton.setOnAction(
						event -> getTableView().getItems().remove(item)
						);
			}
		});

		//Switch visualized model by selecting it in modelTableView
		modelTabelView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				if (newValue.getModelType() == ModelType.DECLARE) {
					declModelChoice.getSelectionModel().select(newValue);
					declPath = newValue.getFilePath();
				} else if (newValue.getModelType() == ModelType.PN) {
					pnModelChoice.getSelectionModel().select(newValue);
					petrinetPath = newValue.getFilePath();
					PnModel p_ = (PnModel) newValue;
					finMarking = p_.finalMarking;
					labelFinalMarking.setText(finMarking);
				}
				//modelTableView.getSelectionModel().clearSelection(); //Causes a weird IndexOutOfBoundsException exception
			}
		});

		//Enable and disable timelineControls and resultsSplitPane based on if there are input models or not
		resultsSplitPane.setDisable(true);
		timelineControls.setDisable(true);
		modelTabelView.getItems().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				resultsSplitPane.setDisable(modelTabelView.getItems().isEmpty());
				timelineControls.setDisable(modelTabelView.getItems().isEmpty());
			}
		});


		//Setting up model ChoiceBoxes
		declModelChoice.setItems(new FilteredList<AbstractModel>(modelTabelView.getItems(), item -> item.getModelType()==ModelType.DECLARE));
		pnModelChoice.setItems(new FilteredList<AbstractModel>(modelTabelView.getItems(), item -> item.getModelType()==ModelType.PN));
		StringConverter<AbstractModel> modelStringConverter = new StringConverter<AbstractModel>() { //For displaying the model name in model choice boxes
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
			public void invalidated(Observable observable) { //Behavior when new model is added while there is no selection and behavior when a selected model is removed
				if (declModelChoice.getSelectionModel().getSelectedIndex()==-1 || !declModelChoice.getItems().contains(declModelChoice.getSelectionModel().getSelectedItem())) {
					declModelChoice.getSelectionModel().selectFirst();
				}
			}
		});
		pnModelChoice.getItems().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) { //Behavior when new model is added while there is no selection and behavior when a selected model is removed
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
		//setupWebView(pnWebView, ModelType.PN, pnZoomSlider, pnZoomSliderValueObject, pnZoomValueField, pnWebViewZoomObject);

		
		//Triggering visualization update when the model selection is changed
		declModelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			declPath = newValue.getFilePath();

			if (!currentPlan.isEmpty()){
				newValue.resetModel();
				newValue.updateMonitoringStates(currentPlan, displayViolations);
			}
			updateVisualization(declWebView, newValue, ModelType.DECLARE);
			updateplanListViewStatistics(newValue, planListView.getItems());
			
		});
		pnModelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			petrinetPath = newValue.getFilePath();
			PnModel p_ = (PnModel) newValue;
			finMarking = p_.finalMarking;
			labelFinalMarking.setText(finMarking);
			
			//if (!currentPlan.isEmpty()){
			//	p_.resetModel();
			//	p_.updateMonitoringStates(currentPlan, displayViolations);
			//}
			updateVisualization(pnWebView, newValue, ModelType.PN);


			
			
		});

		//Timeline setup
		setupTimelineControls();
		updateTimelineControls(tracePrefix);
		
		//Callback to handle clicking on events in planListView (a bidirectional binding between currentEventIndex and planListView.getSelectionModel().selectedIndexProperty() would probably be better)
		Consumer<Integer> selectionCallback = new Consumer<Integer>() {
			@Override
			public void accept(Integer selectedIndex) {
				handleplanListViewSelection(selectedIndex);
			}
		};
		//Handling trace selection by arrow keys (has the unintended side effect that focus traversal into table by up/down arrow will stop the animation)
		planListView.setOnKeyReleased((event) -> {
			if(event.getCode() == KeyCode.UP || event.getCode() == KeyCode.KP_UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.KP_DOWN) {
				handleplanListViewSelection(planListView.getSelectionModel().selectedIndexProperty().intValue());
			}
		});
		
		//All cells in planListView are handled by EventCell class
		planListView.setCellFactory(value -> new EventCell(selectionCallback));	

	}

    @FXML
    void onButtonClickedUploadModel(ActionEvent event) {
		// When uploading a model I immediately save the path to the model.
		// In this way I can later pass it to the Framed Autonomy Tool easier.
		List<File> modelFiles = FileUtils.showModelOpenDialog(stage);
		if (modelFiles != null) {
			List<AbstractModel> abstractModels = new ArrayList<AbstractModel>();
			for (File modelFile : modelFiles) {
				String modelName = modelFile.getName();
				try {
					String modelExtension = modelName.substring(modelName.lastIndexOf(".")+1);
					if ("decl".equalsIgnoreCase(modelExtension)) {
						abstractModels.add(ModelUtils.loadDeclareModel(modelFile.toPath(), "m"+modelCounter, modelName));
						declPath = modelFile.toPath().toAbsolutePath().toString();
					} else if ("pnml".equalsIgnoreCase(modelExtension)) {
						AbstractModel abstractModel = ModelUtils.loadDpnModel(modelFile.toPath(), "m"+modelCounter, modelName);
						abstractModels.add(abstractModel);
						petrinetPath = modelFile.toPath().toAbsolutePath().toString();


						try {
						DataPetriNetImporter dataPetriNetImporter = new DataPetriNetImporter();
						InputStream inputStream = new BufferedInputStream(new FileInputStream(modelFile.toPath().toString()));
						DataPetriNetsWithMarkings dataPetriNet = dataPetriNetImporter.importFromStream(inputStream).getDPN();
		

						PetrinetSemantics sem = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
						sem.initialize(dataPetriNet.getTransitions(), dataPetriNet.getInitialMarking());
						finMarking = dataPetriNet.getFinalMarkings()[0].toString();
						labelFinalMarking.setText(finMarking);
						System.out.println(finMarking);
						} catch (Exception e) {
							// TODO: handle exception
							System.out.println("Use Petri Net with Final Marking!");
						}

					} else {
						System.err.println("Skipping model of unknown type: " + modelExtension);
					}
					modelCounter++;
				} catch (DPNIOException | IOException | IndexOutOfBoundsException e) {
					System.err.println("Unable to load model: " + modelFile.getAbsolutePath());
					e.printStackTrace();
				}
			}

			//If the plan has not been executed then show the Trace Prefix
			if (!planPresent) {
				abstractModels.forEach(abstractModel -> abstractModel.updateMonitoringStates(tracePrefix, displayViolations)); //Monitoring states for an empty prefix
				modelTabelView.getItems().addAll(abstractModels);
			} else { // Otherwise show the last generated plan
				abstractModels.forEach(abstractModel -> abstractModel.updateMonitoringStates(currentPlan, displayViolations)); //Monitoring states for an empty prefix
				modelTabelView.getItems().addAll(abstractModels);
			}

		}
    }

    @FXML
    void onClickPrefix(ActionEvent event) {
		//TODO: Rename function
		// Now denotes the reset of the plan.

		planPresent = false;
	
		// The FramedAutonomy Tool casts everything to lowerCase, so I do the same here. Otherwise there could be an issue when running the planner
    	modelTabelView.getItems().forEach(abstractModel -> abstractModel.resetModel());
		currentPrefix.clear();

		prefixOnlyLabel.setVisible(true);
		prefixOnlyLabel.setManaged(true);
		selectedDecl.setVisible(false);
		selectedDecl.setManaged(false);
		selectedPN.setVisible(false);
		selectedPN.setManaged(false);

		updatePrefix(null);
		updateSelectedModelVisualizations();
		labelCost.setText("");
		

    }

	@FXML
    void onClickPlanner(ActionEvent event) throws IOException, InterruptedException {

//		try {
		if (progressLayer == null && !progressLayerLoadAttempted) {
			try {
				// Load the progress layer
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/framedinterface/ProgressLayer.fxml"));
				progressLayer = loader.load();
				ProgressLayerController progressLayerController = loader.getController();
				progressLayerController.getProgressTextLabel().setText("Running planner...");
				
				progressLayerController.getCancelButton().setOnAction(e -> {
					executorService.shutdownNow(); //TODO: Terminate the currently executing task instead
					executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //Didn't see a way to restart after shutdown
					setUiBusy(false);
				});
				
			} catch (Exception e) {
				System.out.println("Cannot load progress layer");
				e.printStackTrace();
			} finally {
				progressLayerLoadAttempted = true;
			}
		}
		
		
		
		
		setUiBusy(true);

		planPresent = true;

		planListView.getItems().clear();

		modelTabelView.getItems().forEach(abstractModel -> abstractModel.resetModel());

		if ((modelTabelView.getItems().size() < 2)|| (declPath == null) || (petrinetPath == null)){
			// This check alone could cause issues if someone uploaded multiple petri nets
			// Then deleted all of them
			// then uploaded multiple declare models
			// then tried to run the planner.
			System.out.println("Error: Number of input files not matching");
			setUiBusy(false);
			return;
		}

		//Write prefix to file and then pass it

		BufferedWriter writer = new BufferedWriter(new FileWriter(currentPath + "/prefix.txt"));

		if (currentPrefix.isEmpty()) {
			writer.write("");
		}	
		else {
			
			String prefixString = currentPrefix.toString();
			prefixString = prefixString.replace(",", "");
			prefixString = prefixString.replace("[", "");
			prefixString = prefixString.replace("]", "");
			System.out.println(prefixString);
			writer.write(prefixString);
		}

				
		writer.close();

		ArrayList<String> commandStrings = new ArrayList<String>();
		commandStrings.add("java");
		commandStrings.add("-jar");
		//commandStrings.add(currentPath+"/"+framedAutonomyJar);
		commandStrings.add(currentPath+"/"+framedAutonomyJar);
		
		if (modelTabelView.getItems().size() < 2){
			System.out.println("Error: Number of input files not matching");
			return;
		}
		else{
			commandStrings.add(declPath);
			commandStrings.add("\""+currentPath+"/prefix.txt"+"\"");
			commandStrings.add(petrinetPath);
		}


		//Run the Framed Autonomy Tool Jar
		// Assumed to be located in the project repository
		GeneratePDDLTask generatePDDLTask = new GeneratePDDLTask(commandStrings, finMarking, resetDomain);
		generatePDDLTask.setOnFailed(taskEvent -> {
			// Ensure that any errors do not lead to the system crashing
			AlertUtils.showError("Generating PDDL failed");
			setUiBusy(false);
		});
		generatePDDLTask.setOnSucceeded(pddlTaskEvent -> {
			
			File f = new File(currentPath+"/fast-downward/fast-downward.py");
			
			RunPlannerTask runPlannerTask = new RunPlannerTask(currentPath, resetDomain, f.exists());
			runPlannerTask.setOnFailed(plannerTaskEvent -> {
				// Ensure that any errors do not lead to the system crashing
				AlertUtils.showError("Running the planner failed");
				setUiBusy(false);
			});
			runPlannerTask.setOnSucceeded(plannerTaskEvent -> {
				ArrayList<String> generatedPlan =  FileUtils.parsePlan(currentPath+"/results.txt");
				
				ArrayList<String> onlyActions = new ArrayList<>();
				planListView.getItems().clear();
				
				for (String action : generatedPlan) {
					
					String[] steps = action.split(" ");
					System.out.println(steps[0] + " "+ " " + steps[steps.length-2]);
					
					int actInd = steps.length-2;
					if (steps[0].contains("sync")) {
						onlyActions.add(steps[0]+";"+steps[actInd]);
					}
					if (steps[0].contains("prefix_violate")) {
						onlyActions.add(steps[0]+";"+steps[actInd]);
					}
					if (steps[0].contains("reset")) {
						onlyActions.add(steps[0]+";"+steps[0]+"-"+steps[actInd]);
					}
					
					
				}
				System.out.println(onlyActions);
				modelTabelView.getItems().forEach(abstractModel -> abstractModel.updateMonitoringStates(onlyActions, displayViolations));
				updateplanListView(onlyActions);
				updateTimelineControls(onlyActions);
				this.currentPlan = onlyActions;
				labelCost.setText(FileUtils.parsePlanCost(currentPath+"/results.txt"));
				
				setUiBusy(false);
			});
			
			executorService.execute(runPlannerTask);
			
			
			
//			int plannerExit = 1;
//			
//			try {
//				// Run downward planner
//				f = new File(currentPath+"/fast-downward/fast-downward.py");
//				if (f.exists()){
//					plannerExit = RunnerUtils.runPlanner(currentPath, resetDomain);
//				}
//				else {
//					plannerExit = RunnerUtils.runPlanner2(currentPath, resetDomain);
//				}
//				System.out.println("Planning Done");
//				
//				if (plannerExit == 0){
//					ArrayList<String> generatedPlan =  FileUtils.parsePlan(currentPath+"/results.txt");
//		
//					ArrayList<String> onlyActions = new ArrayList<>();
//					planListView.getItems().clear();
//		
//					for (String action : generatedPlan) {
//		
//						String[] steps = action.split(" ");
//						System.out.println(steps[0] + " "+ " " + steps[steps.length-2]);
//		
//						int actInd = steps.length-2;
//						if (steps[0].contains("sync")) {
//							onlyActions.add(steps[0]+";"+steps[actInd]);
//						}
//						if (steps[0].contains("prefix_violate")) {
//							onlyActions.add(steps[0]+";"+steps[actInd]);
//						}
//						if (steps[0].contains("reset")) {
//							onlyActions.add(steps[0]+";"+steps[0]+"-"+steps[actInd]);
//						}
//						
//		
//					}
//					System.out.println(onlyActions);
//					modelTabelView.getItems().forEach(abstractModel -> abstractModel.updateMonitoringStates(onlyActions, displayViolations));
//					updateplanListView(onlyActions);
//					updateTimelineControls(onlyActions);
//					this.currentPlan = onlyActions;
//					labelCost.setText(FileUtils.parsePlanCost(currentPath+"/results.txt"));
//				}
//				
//			} catch (Exception e) {
//				// Ensure that any errors do not lead to the system crashing
//				setUiBusy(false);
//			}
//			
//			setUiBusy(false);
		});
		
		executorService.execute(generatePDDLTask);
			
	
//			int exitCode = RunnerUtils.generatePDDL(commandStrings, finMarking, resetDomain);
//	
//			System.out.println("Process exited with code: " + exitCode);
//			int plannerExit = 1;
//			if (exitCode == 0){
//				// Run downward planner
//				File f = new File(currentPath+"/fast-downward/fast-downward.py");
//				if (f.exists()){
//					plannerExit = RunnerUtils.runPlanner(currentPath, resetDomain);
//				}
//				else {
//				plannerExit = RunnerUtils.runPlanner2(currentPath, resetDomain);
//				}
//				System.out.println("Planning Done");
//				
//			}
//	
//			if (plannerExit == 0){
//				ArrayList<String> generatedPlan =  FileUtils.parsePlan(currentPath+"/results.txt");
//	
//				ArrayList<String> onlyActions = new ArrayList<>();
//				planListView.getItems().clear();
//	
//				for (String action : generatedPlan) {
//	
//					String[] steps = action.split(" ");
//					System.out.println(steps[0] + " "+ " " + steps[steps.length-2]);
//	
//					int actInd = steps.length-2;
//					if (steps[0].contains("sync")) {
//						onlyActions.add(steps[0]+";"+steps[actInd]);
//					}
//					if (steps[0].contains("prefix_violate")) {
//						onlyActions.add(steps[0]+";"+steps[actInd]);
//					}
//					if (steps[0].contains("reset")) {
//						onlyActions.add(steps[0]+";"+steps[0]+"-"+steps[actInd]);
//					}
//					
//	
//				}
//				System.out.println(onlyActions);
//				modelTabelView.getItems().forEach(abstractModel -> abstractModel.updateMonitoringStates(onlyActions, displayViolations));
//				updateplanListView(onlyActions);
//				updateTimelineControls(onlyActions);
//				this.currentPlan = onlyActions;
//				labelCost.setText(FileUtils.parsePlanCost(currentPath+"/results.txt"));
//			}
//
//		} catch (Exception e) {
//			// Ensure that any errors do not lead to the system crashing
//			mainContents.setDisable(false);
//		}
		
		prefixOnlyLabel.setVisible(false);
		prefixOnlyLabel.setManaged(false);
		selectedDecl.setText(declModelChoice.getSelectionModel().getSelectedItem().getModelName().toString());
		selectedDecl.setVisible(true);
		selectedDecl.setManaged(true);
		selectedPN.setText(pnModelChoice.getSelectionModel().getSelectedItem().getModelName().toString());
		selectedPN.setVisible(true);
		selectedPN.setManaged(true);
	}
	
	private void setUiBusy(boolean disable) { 
		//Not 100% sure why mainContents.setDisable(disable) does not work when called directly (at least without busy layer)
		//...I guess Platform.runLater has the side effect of ensuring that the view is refreshed (adding busy layer probably has the same effect)
		Platform.runLater(() -> mainContents.setDisable(disable));
		
		if (progressLayer != null) {
			if (disable) {
				rootElement.getChildren().add(progressLayer);
			} else {
				rootElement.getChildren().remove(progressLayer);
			}
		}
	}

    @FXML
    void onClickResetYes(ActionEvent event) {
		//resetDomain = true;
		resetDomain = buttonResetYes.isSelected();
		/*
		if (resetDomain) {
			labelCurrentDomain.setText("Domain_with_reset.pddl");
		}
		else{
			labelCurrentDomain.setText("Domain_no_reset.pddl");
		}
		*/
		
    }

	@FXML
    void onClickFinalMarking(ActionEvent event) {
		finMarking = textFieldFinalMarking.getText();
		labelFinalMarking.setText(finMarking);
		textFieldFinalMarking.clear();
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

	
	private void updatePrefix(Integer selectIndex) { //TODO: Allow building prefix event-by-event, without resetting after each event
		tracePrefix = new ArrayList<String>(); //Resetting to empty trace

		if (!currentPrefix.isEmpty()) {
			tracePrefix = currentPrefix;
			//modelTabelView.getItems().forEach(abstractModel -> abstractModel.resetModel());
			//modelTabelView.getItems().forEach(abstractModel -> abstractModel.updateMonitoringStates(tracePrefix, displayViolations));
			//updateplanListView(tracePrefix);
			//updateTimelineControls(tracePrefix);
		}

		if ((!planPresent)){
			// Very stupid fix but i didn't have time to find a better solution
			modelTabelView.getItems().forEach(abstractModel -> abstractModel.resetModel());
			modelTabelView.getItems().forEach(abstractModel -> abstractModel.updateMonitoringStates(tracePrefix, displayViolations));
			updateplanListView(tracePrefix);
			updateTimelineControls(tracePrefix);
		}
		
		if (selectIndex != null) { //TODO: Not sure if this is the best place for scrolling to the added activity
			eventSlider.setValue(selectIndex);
			currentEventIndex.setValue(selectIndex);
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
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
					((JSObject)visualizationWebView.getEngine().executeScript("window")).setMember("app", InitialController.this);
					if (modelType == ModelType.DECLARE) { //A hack to load the pnWebView only after declWebView is already loaded
						setupWebView(pnWebView, ModelType.PN, pnZoomSlider, pnZoomSliderValueObject, pnZoomValueField, pnWebViewZoomObject);
					}
					visualizationWebView.getEngine().getLoadWorker().stateProperty().removeListener(this);
				}
			}
		};
		
		visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener(initialLoadListener);
		
		
		visualizationWebView.getEngine().load((getClass().getClassLoader().getResource("visPage.html")).toString());
		visualizationWebView.setContextMenuEnabled(false); //Setting it in FXML causes an IllegalArgumentException
		

		if (modelType == ModelType.DECLARE) { //This part is a bit annoying, but there is no true "pass by reference" in Java 
			visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
				if(newValue == Worker.State.SUCCEEDED && initialDeclWebViewScript != null) { //If initialDeclWebViewScript != null, then a visualization update was postponed due to the LoadWorker being busy
					if (initialDeclWebViewScript.equals("")) {visualizationWebView.getEngine().executeScript("clearModel()");} //Using empty string to mark that visualization should be cleared
					else {visualizationWebView.getEngine().executeScript(initialDeclWebViewScript);}
					initialDeclWebViewScript = null;
				}
			});
		} else if (modelType == ModelType.PN) {
			visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
				if(newValue == Worker.State.SUCCEEDED && initialPnWebViewScript != null) { //If initialDeclWebViewScript != null, then a visualization update was postponed due to the LoadWorker being busy
					if (initialPnWebViewScript.equals("")) {visualizationWebView.getEngine().executeScript("clearModel()");} //Using empty string to mark that visualization should be cleared
					else {visualizationWebView.getEngine().executeScript(initialPnWebViewScript);}
					initialPnWebViewScript = null;
				}
			});
		}

		visualizationWebView.addEventFilter(ScrollEvent.SCROLL, e -> {
			if (e.isControlDown()) {
				double deltaY = e.getDeltaY();
				//Setting the value of zoom slider (instead of WebView), because then the slider also defines min and max zoom levels
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
					//logger.debug("Invalid zoom value: {}", string, e);
					return 1d; //Defaulting to 100% zoom level
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
		
		List<EventData> eventDataList = new ArrayList<EventData>();
		eventDataList.add(EventData.createStartEvent());
		for (int i = 0; i < activities.size(); i++) {
			//eventDataList.add(new EventData(i+1, activities.get(i)));
			String[] planAction = activities.get(i).split(";");
			System.out.println("planAction"+activities.get(i));
			if (planAction.length == 1) {
				eventDataList.add(new EventData(i+1, activities.get(i)));
			} else{
				eventDataList.add(new EventData(i+1, planAction[1], planAction[0]));
			}
			
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

		eventSlider.setValue(0d); //The initial state before any events have occurred
		currentEventIndex.setValue(0);
		totalEventsNumber.setText(Integer.toString(activities.size()+1));
		eventSlider.setMax(activities.size()+1); //The final state after the trace has ended

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



	//Called when the user clicks on graph elements
	private void addToTracePrefix(String modelId, String activityEncoding) {
		String activityName = null;
		for (AbstractModel abstractModel : modelTabelView.getItems()) {
			if (abstractModel.getModelId().equals(modelId)) {
				activityName = abstractModel.getActivityByEncoding(activityEncoding);
				break;
			}
		}
		if (activityName != null) {
			
			currentPrefix.add(activityName);
			updatePrefix(currentPrefix.size());

			/*
			if (textFieldPrefix.getText().isEmpty()) {
				textFieldPrefix.setText(activityName);
			} else {
				if (textFieldPrefix.getText().stripTrailing().endsWith(",")) {
					textFieldPrefix.setText(textFieldPrefix.getText() + activityName);
				} else {
					textFieldPrefix.setText(textFieldPrefix.getText() + ", " + activityName);
				}
			}
			*/
		}
	}

	//Convenience method for when both visualizations need to be updated
	private void updateSelectedModelVisualizations() {
		updateVisualization(declWebView, declModelChoice.getSelectionModel().getSelectedItem(), ModelType.DECLARE);
		updateVisualization(pnWebView, pnModelChoice.getSelectionModel().getSelectedItem(), ModelType.PN);

	}


	//TODO: There have been a few rare instances where webView remained empty after selecting a set of Declare models, haven't been able to reproduce
	private void updateVisualization(WebView visualizationWebView, AbstractModel abstractModel, ModelType modelType) {
		String visualizationString;
		String script;

		if (abstractModel == null) {
			//Reloading the page in case a previous visualization script is still executing, initialWebViewScript is set to null because it will otherwise be executed after reload
			//TODO: Should instead track if a visualization script is still executing and stop it (if it is possible)
			if (modelType == ModelType.DECLARE) {initialDeclWebViewScript = "";}
			if (modelType == ModelType.PN) {initialPnWebViewScript = "";}
			//visualizationWebView.getEngine().reload();
			//((JSObject)visualizationWebView.getEngine().executeScript("window")).setMember("app", this); //Does not work after reload for some reason
			visualizationWebView.getEngine().executeScript("clearModel()");
		} else {
			visualizationString = abstractModel.getVisualisationString(currentEventIndex.get(), displayViolations);
			if (visualizationString != null) {
				script = "setModel('" + visualizationString + "')";
				if (visualizationWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) { //If load worker is not busy then execute current script
					visualizationWebView.getEngine().executeScript(script);
					if (abstractModel.getModelType() == ModelType.DECLARE) {initialDeclWebViewScript = null;}
					if (abstractModel.getModelType() == ModelType.PN) {initialPnWebViewScript = null;}
				} else { //...otherwise place the script in a variable, from which it will be executed when load worker finishes the previous task
					if (abstractModel.getModelType() == ModelType.DECLARE) {initialDeclWebViewScript = script;}
					if (abstractModel.getModelType() == ModelType.PN) {initialPnWebViewScript = script;}
				}
			} else {
				//Might be better to show an error message to the user instead
				if (abstractModel.getModelType() == ModelType.DECLARE) {initialDeclWebViewScript = "";}
				if (abstractModel.getModelType() == ModelType.PN) {initialPnWebViewScript = "";}
				//visualizationWebView.getEngine().reload();
				//((JSObject)visualizationWebView.getEngine().executeScript("window")).setMember("app", this); //Does not work after reload for some reason
				visualizationWebView.getEngine().executeScript("clearModel()");
			}
		}
	}

	//Called from JavaScript, adds click listeners to all graph nodes
	//TODO: Could be simplified perhaps
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
											//System.out.println("Clicked on visualization graph element: " + node_j.getAttributes().getNamedItem("id").getNodeValue());
											//System.out.println("\tEncoded activity is: " + node_k.getTextContent() + " (model: " + modelId + ")");
										}, false);
									}
								}
							}
						}
						return; //Assuming there is only one graph
					}
				}
				System.err.println("Cannot find graphRoot"); //If graph root element is found then this line will not be reached due to the return above
			} else {
				System.err.println("Cannot find rootDiv element");
			}
		} else {
			System.err.println("documentObject must be instance of Document class");
		}
	}

	@FXML
	private void switchViolationStrings() {

		displayViolations = bttnDisplayViolations.isSelected();

		updateSelectedModelVisualizations();
		
	}

}
