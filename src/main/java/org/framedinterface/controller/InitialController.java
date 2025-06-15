package org.framedinterface.controller;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.framedinterface.model.AbstractModel;
import org.framedinterface.model.DpnModel;
import org.framedinterface.utils.AutomatonUtils;
import org.framedinterface.utils.FileUtils;
import org.framedinterface.utils.LogUtils;
import org.framedinterface.utils.ModelUtils;
import org.framedinterface.utils.RunnerUtils;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.datapetrinets.io.DPNIOException;
import org.processmining.datapetrinets.io.DataPetriNetImporter;
//import ProcessBuilder;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;

public class InitialController {

    @FXML
    private Button buttonPrefix;
	@FXML
    private Button buttonResetNo;

    @FXML
    private Button buttonResetYes;

	@FXML
	private TableView<AbstractModel> modelTabelView;
	@FXML
	private TableColumn<AbstractModel, String> modelNameColumn;
	@FXML
	private TableColumn<AbstractModel, String> modelTypeColumn;
	@FXML
	private TableColumn<AbstractModel, AbstractModel> modelRemoveColumn;

	@FXML
    private ListView<String> planListView;

	@FXML
	private ScrollPane resultsPane;

    @FXML
    private Button buttonRunPlanner;

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

	private static String precentageFormat = "%.1f";
	//Storing object references here
	private ObjectProperty<Double> declZoomSliderValueObject;
	private ObjectProperty<Double> pnZoomSliderValueObject;
	private ObjectProperty<Double> declWebViewZoomObject;
	private ObjectProperty<Double> pnWebViewZoomObject;
	
	private int modelCounter = 0; //For unique model identifiers, needed to reliably determine which model was clicked in the visualization (not an ideal solution)

	private String initialDeclWebViewScript;
	private String initialPnWebViewScript;

	List<VBox> resultsList;
    private Stage stage;
    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private int defaultViolationCost = 3;
	private String declPath;
	private String petrinetPath;
	private String currentPath;
	private String finMarking;
	private boolean resetDomain;
	//private static String framedAutonomyJar = "C:\\Users\\paulw\\Desktop\\framedAutonomy\\FramedAutonomyTool.jar"; 
	private static String framedAutonomyJar = "FramedAutonomyTool.jar"; 
	private static String pythonPath = "C:/Users/paulw/anaconda3/python.exe";

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@FXML
	private void initialize() {
		resetDomain = true;
		currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
		
		modelTabelView.setPlaceholder(new Label("No input models selected"));
		modelNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		modelNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelName()));
		modelTypeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		modelTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelType().toString()));
		modelRemoveColumn.setCellValueFactory(
				param -> new ReadOnlyObjectWrapper<AbstractModel>(param.getValue())
				);
		modelRemoveColumn.setCellFactory(param -> new TableCell<AbstractModel, AbstractModel>() {
			private final Button removeButton = new Button("Remove");

			@Override
			protected void updateItem(AbstractModel item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null) {
					setGraphic(null);
					return;
				}

				setGraphic(removeButton);
				removeButton.setOnAction(
						event -> getTableView().getItems().remove(item)
						);
			}
		});

		planListView.getItems().clear();
		
		planListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
			if (newIndex.intValue() >= 0) {
				// Select the visualization that corresponds to the correct index
				//resultsPane.setContent(resultsList.get(newIndex.intValue()));
				resultsPane.setContent(null);
			} else {
				resultsPane.setContent(null);
			}
		});
		
	

	}

    @FXML
    void onButtonClickedUploadModel(ActionEvent event) {
		List<File> modelFiles = FileUtils.showModelOpenDialog(stage);
		if (modelFiles != null) {
			List<AbstractModel> abstractModels = new ArrayList<AbstractModel>();
			for (File modelFile : modelFiles) {
				String modelName = modelFile.getName();
				try {
					String modelExtension = modelName.substring(modelName.lastIndexOf(".")+1);
					if ("decl".equalsIgnoreCase(modelExtension)) {
						abstractModels.add(ModelUtils.loadDeclareModel(modelFile.toPath(), modelName, defaultViolationCost));
						declPath = modelFile.toPath().toAbsolutePath().toString();
					} else if ("ltl".equalsIgnoreCase(modelExtension)) {
						abstractModels.add(ModelUtils.loadLtlModel(modelFile.toPath(), modelName, defaultViolationCost));
					} else if ("pnml".equalsIgnoreCase(modelExtension)) {
						AbstractModel abstractModel = ModelUtils.loadDpnModel(modelFile.toPath(), modelName, defaultViolationCost);
						abstractModels.add(abstractModel);
						//DataPetriNet dpn = 
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
				} catch (DPNIOException | IOException | IndexOutOfBoundsException e) {
					System.err.println("Unable to load model: " + modelFile.getAbsolutePath());
					e.printStackTrace();
				}
			}
			modelTabelView.getItems().addAll(abstractModels);
		}
    }

    @FXML
    void onClickPrefix(ActionEvent event) {
		// Set the Prefix and Clear the text field
		//System.out.println(textFieldPrefix.getText());
		curPrefix.setText(textFieldPrefix.getText());
		textFieldPrefix.clear();
    }

	@FXML
    void onClickPlanner(ActionEvent event) throws IOException, InterruptedException {

		planListView.getItems().clear();

		//Write prefix to file and then pass it

		BufferedWriter writer = new BufferedWriter(new FileWriter(currentPath + "/prefix.txt"));

		if (curPrefix.getText().contains("<None>")) {
			writer.write("");
		}	
		else {
			writer.write(curPrefix.getText());
		}
				
		writer.close();

		ArrayList<String> commandStrings = new ArrayList<String>();
		commandStrings.add("java");
		commandStrings.add("-jar");
		commandStrings.add(currentPath+"/"+framedAutonomyJar);
		
		if (modelTabelView.getItems().size() != 2){
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

		int exitCode = RunnerUtils.generatePDDL(commandStrings, finMarking, resetDomain);

		System.out.println("Process exited with code: " + exitCode);
		int plannerExit = 1;
		if (exitCode == 0){
			// Run downward planner
			plannerExit = RunnerUtils.runPlanner(currentPath, resetDomain);
			System.out.println("Planning Done");
			
		}

		if (plannerExit == 0){
			ArrayList<String> generatedPlan =  FileUtils.parsePlan(currentPath+"/results.txt");

			for (String action : generatedPlan) {
				//resultsList.add(monitoringTask.getValue());
				planListView.getItems().add(action);

			}
		}


	}

    @FXML
    void onClickResetNo(ActionEvent event) {
		resetDomain = false;
		labelCurrentDomain.setText("Domain_no_reset.pddl");
    }

    @FXML
    void onClickResetYes(ActionEvent event) {
		resetDomain = true;
		labelCurrentDomain.setText("Domain_with_reset.pddl");
    }

	@FXML
    void onClickFinalMarking(ActionEvent event) {
		finMarking = textFieldFinalMarking.getText();
		labelFinalMarking.setText(finMarking);
		textFieldFinalMarking.clear();
    }


}
