package org.framedinterface.controller.processframeoverview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.framedinterface.controller.common.AbstractController;
import org.framedinterface.model.AbstractModel;
import org.framedinterface.model.DeclareModel;
import org.framedinterface.model.ModelRegistry;
import org.framedinterface.model.ModelType;
import org.framedinterface.model.PlannerSession;
import org.framedinterface.model.PnModel;
import org.framedinterface.utils.FileUtils;
import org.framedinterface.utils.ModelUtils;
import org.framedinterface.utils.ValidationUtils;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;
import org.processmining.datapetrinets.io.DPNIOException;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.util.StringConverter;

public class ProcessFrameOverviewController extends AbstractController {

	@FXML
	private TableView<AbstractModel> modelTabelView;
	@FXML
	private TableColumn<AbstractModel, String> modelNameColumn;
	@FXML
	private TableColumn<AbstractModel, String> modelTypeColumn;
	@FXML
	private TableColumn<AbstractModel, AbstractModel> modelRemoveColumn;
	@FXML
	private Button uploadModel;

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
	private Button toolTipButtonPN;

	private static String precentageFormat = "%.1f";
	private String initialDeclWebViewScript;
	private String initialPnWebViewScript;

	private ObjectProperty<Double> declZoomSliderValueObject;
	private ObjectProperty<Double> pnZoomSliderValueObject;
	private ObjectProperty<Double> declWebViewZoomObject;
	private ObjectProperty<Double> pnWebViewZoomObject;

	private boolean showDataConditions = false;

	@FXML
	private void initialize() {

		modelTabelView.setItems(ModelRegistry.getInstance().getModels());
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

		//Switch visualized model by selecting it in modelTabelView
		modelTabelView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				if (newValue.getModelType() == ModelType.DECLARE) {
					declModelChoice.getSelectionModel().select(newValue);
				} else if (newValue.getModelType() == ModelType.PN) {
					pnModelChoice.getSelectionModel().select(newValue);
					PnModel p_ = (PnModel) newValue;
					labelFinalMarking.setText(p_.finalMarking);
				}
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
		setupWebView(pnWebView, ModelType.PN, pnZoomSlider, pnZoomSliderValueObject, pnZoomValueField, pnWebViewZoomObject);

		//Triggering visualization update when the model selection is changed
		declModelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			updateVisualization(declWebView, newValue, ModelType.DECLARE);
		});
		pnModelChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				PnModel p_ = (PnModel) newValue;
				labelFinalMarking.setText(p_.finalMarking);
			}
			updateVisualization(pnWebView, newValue, ModelType.PN);
		});

		dataConditionsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			showDataConditions = newValue;
			updateVisualization(declWebView, declModelChoice.getSelectionModel().getSelectedItem(), ModelType.DECLARE);
		});
	}

	@FXML
	void onButtonClickedUploadModel(ActionEvent event) {
		List<File> modelFiles = FileUtils.showModelOpenDialog(getStage());
		if (modelFiles != null) {
			List<AbstractModel> abstractModels = new ArrayList<AbstractModel>();
			for (File modelFile : modelFiles) {
				String modelName = modelFile.getName();
				try {
					String modelExtension = modelName.substring(modelName.lastIndexOf(".")+1);
					if ("decl".equalsIgnoreCase(modelExtension)) {
						abstractModels.add(ModelUtils.loadDeclareModel(modelFile.toPath(), ModelRegistry.getInstance().nextModelId(), modelName));
					} else if ("pnml".equalsIgnoreCase(modelExtension)) {
						// PnModel's constructor already computes finalMarking from the parsed DPN
						abstractModels.add(ModelUtils.loadDpnModel(modelFile.toPath(), ModelRegistry.getInstance().nextModelId(), modelName));
					} else {
						System.err.println("Skipping model of unknown type: " + modelExtension);
					}
				} catch (DPNIOException | IOException | IndexOutOfBoundsException e) {
					System.err.println("Unable to load model: " + modelFile.getAbsolutePath());
					e.printStackTrace();
				}
			}

			//Evaluate newly added models against whatever prefix/plan is currently active on Data-Agnostic
			abstractModels.forEach(abstractModel -> abstractModel.updateMonitoringStates(
					PlannerSession.getInstance().getActiveTrace(), PlannerSession.getInstance().isDisplayViolations()));
			ModelRegistry.getInstance().getModels().addAll(abstractModels);
		}
	}

	private void setupWebView(WebView visualizationWebView, ModelType modelType, Slider zoomSlider, ObjectProperty<Double> zoomSliderValueObject, TextField zoomValueField, ObjectProperty<Double> webViewZoomObject) {
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

	//Always renders the model's static structure (no timeline, no violations) - this page is for viewing/selecting, not monitoring
	private void updateVisualization(WebView visualizationWebView, AbstractModel abstractModel, ModelType modelType) {
		String visualizationString;
		String script;

		if (abstractModel == null) {
			if (modelType == ModelType.DECLARE) {initialDeclWebViewScript = "";}
			if (modelType == ModelType.PN) {initialPnWebViewScript = "";}
			visualizationWebView.getEngine().executeScript("clearModel()");
		} else {
			visualizationString = abstractModel instanceof DeclareModel
					? ((DeclareModel) abstractModel).getVisualisationString(0, false, showDataConditions)
					: abstractModel.getVisualisationString(0, false);
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

	@FXML
	private void onClickToolTip() {
		Popup legendPopup = new Popup();

		VBox legendBox = new VBox(10);
		legendBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");

		legendBox.getChildren().addAll(
			new Label("Shows the structure of the selected Declare model."),
			new Label("Select a model in the table or the choice box above to view it.")
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

	@FXML
	private void onClickToolTipPN() {
		Popup legendPopup = new Popup();

		VBox legendBox = new VBox(10);
		legendBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: black;");

		legendBox.getChildren().addAll(
			new Label("Shows the structure of the selected Petri net model."),
			new Label("Select a model in the table or the choice box above to view it.")
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

}
