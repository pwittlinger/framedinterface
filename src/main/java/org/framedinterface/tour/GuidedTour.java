package org.framedinterface.tour;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.VLineTo;
import javafx.util.Duration;

/**
 * Guided tour of the application: an overlay that dims the window, with an animated spotlight on one element per
 * step and a card that explains it. The application can't be used during the tour (mouse and keyboard are blocked).
 *
 * The steps find their elements through the fx:id in the FXML files, so the pages don't know about the tour. If a
 * target is renamed or hidden, its step is skipped and logged.
 */
public class GuidedTour {

	private record Step(String pageButton, String target, String title, String text) {}

	private static final String OVERVIEW = "processFrameOverviewButton";
	private static final String DATA_AWARE = "dataAwareButton";

	private static final List<Step> STEPS = List.of(
			new Step(OVERVIEW, "uploadModel", "Load your models",
					"Select the DECLARE and Petri net models of the process frame. They stay available on all pages."),
			new Step(DATA_AWARE, "modelRegistryPane", "Choose the process specifications",
					"Tick the loaded models the planner has to respect."),
			new Step(DATA_AWARE, "constraintsPane", "Constraints",
					"The constraints of the selected models. Set their violation cost, or mark them as hard so they can't be violated."),
			new Step(DATA_AWARE, "activitiesAttributesPane", "Activities",
					"The activities of the selected models and the cost of adding them to a plan."),
			new Step(DATA_AWARE, "declWebView", "DECLARE view",
					"The selected DECLARE model. Colours show the state of each constraint during the replay; the Help button has the legend. "
							+ "Clicking an activity adds it to the prefix."),
			new Step(DATA_AWARE, "prefixPane", "Prefix",
					"The events that already happened. Build it by clicking on the models, and use + and - to manage several prefixes."),
			new Step(DATA_AWARE, "importPrefixButton", "Import a prefix",
					"Or import an event log: every trace becomes a prefix."),
			new Step(DATA_AWARE, "plannerSettingsButton", "Planner settings",
					"What the planner optimizes, the recovery strategy for violated constraints, the waiting cost and the search options."),
			new Step(DATA_AWARE, "buttonRunPlanner", "Run the planner",
					"Finds the best continuation for every prefix."),
			new Step(DATA_AWARE, "continuationPane", "Continuation",
					"The plan of the selected prefix: events replayed, skipped or added, and constraints reset. "
							+ "It stays empty until the planner has run."),
			new Step(DATA_AWARE, "timelineControls", "Replay",
					"Step through the plan or play it, and follow it on the models above."));

	private static final double HOLE_PADDING = 6;
	private static final double CARD_GAP = 16;

	private final DoubleProperty holeX = new SimpleDoubleProperty();
	private final DoubleProperty holeY = new SimpleDoubleProperty();
	private final DoubleProperty holeWidth = new SimpleDoubleProperty();
	private final DoubleProperty holeHeight = new SimpleDoubleProperty();

	private final Pane overlay = new Pane();
	private final Path dim = new Path();
	private final Rectangle ring = new Rectangle();
	private final VBox card = new VBox(8);
	private final Label progressLabel = new Label();
	private final Label titleLabel = new Label();
	private final Label textLabel = new Label();
	private final Button backButton = new Button("Back");
	private final Button nextButton = new Button("Next");
	private final Button skipButton = new Button("Skip");

	private final Timeline holeAnimation = new Timeline();
	private final ScaleTransition ringPulse = new ScaleTransition(Duration.millis(600), ring);
	private final FadeTransition cardFade = new FadeTransition(Duration.millis(300), card);
	private final FadeTransition overlayFade = new FadeTransition(Duration.millis(200), overlay);

	private Scene scene;
	private boolean active;
	private int currentStep;

	public GuidedTour() {
		buildOverlay();
	}

	// To put on top of the application, in a StackPane
	public Node getOverlay() {
		return overlay;
	}

	public void install(Scene scene) {
		this.scene = scene;
		scene.addEventFilter(KeyEvent.ANY, event -> {
			if (!active) {
				return;
			}
			event.consume();
			if (event.getEventType() == KeyEvent.KEY_PRESSED) {
				handleKey(event.getCode());
			}
		});
	}

	public void start() {
		overlayFade.stop();
		overlay.setOpacity(1);
		overlay.setVisible(true);
		active = true;
		currentStep = -1;
		move(1, true);
	}

	private void handleKey(KeyCode key) {
		switch (key) {
		case ESCAPE -> finish();
		case ENTER, RIGHT -> move(1, false);
		case LEFT -> move(-1, false);
		default -> { }
		}
	}

	private void finish() {
		active = false;
		holeAnimation.stop();
		ringPulse.stop();
		overlayFade.setFromValue(1);
		overlayFade.setToValue(0);
		overlayFade.setOnFinished(event -> overlay.setVisible(false));
		overlayFade.play();
	}

	// To the next step in this direction that can be shown. Past the last one the tour ends
	private void move(int direction, boolean jump) {
		for (int i = currentStep + direction; i >= 0 && i < STEPS.size(); i += direction) {
			if (show(i, jump)) {
				return;
			}
		}
		if (direction > 0) {
			finish();
		} else {
			show(currentStep, jump); // Nothing before this step: back on its page, if the search left it
		}
	}

	private boolean show(int index, boolean jump) {
		Step step = STEPS.get(index);
		Node pageButton = scene.lookup("#" + step.pageButton());
		if (pageButton instanceof Button button) {
			button.fire();
		}
		// A page that was just opened has no layout yet, and so no bounds
		Parent root = scene.getRoot();
		root.applyCss();
		root.layout();

		Node target = scene.lookup("#" + step.target());
		if (!isShowing(target)) {
			System.out.println("GuidedTour: skipping step \"" + step.title() + "\", can't show #" + step.target());
			return false;
		}
		currentStep = index;
		progressLabel.setText((index + 1) + " / " + STEPS.size());
		titleLabel.setText(step.title());
		textLabel.setText(step.text());
		backButton.setDisable(index == 0);
		nextButton.setText(index == STEPS.size() - 1 ? "Finish" : "Next");
		spotlight(target, jump);

		cardFade.setFromValue(0);
		cardFade.setToValue(1);
		cardFade.playFromStart();
		ringPulse.stop();
		ring.setScaleX(1);
		ring.setScaleY(1);
		ringPulse.playFromStart();
		return true;
	}

	private boolean isShowing(Node target) {
		if (target == null || target.getBoundsInLocal().getWidth() <= 0 || target.getBoundsInLocal().getHeight() <= 0) {
			return false;
		}
		for (Node node = target; node != null; node = node.getParent()) {
			if (!node.isVisible()) {
				return false;
			}
		}
		return true;
	}

	private void spotlight(Node target, boolean jump) {
		Bounds bounds = overlay.sceneToLocal(target.localToScene(target.getBoundsInLocal()));
		double x = bounds.getMinX() - HOLE_PADDING;
		double y = bounds.getMinY() - HOLE_PADDING;
		double width = bounds.getWidth() + 2 * HOLE_PADDING;
		double height = bounds.getHeight() + 2 * HOLE_PADDING;
		holeAnimation.stop();
		if (jump) {
			holeX.set(x);
			holeY.set(y);
			holeWidth.set(width);
			holeHeight.set(height);
		} else {
			holeAnimation.getKeyFrames().setAll(new KeyFrame(Duration.millis(450),
					new KeyValue(holeX, x, Interpolator.EASE_BOTH),
					new KeyValue(holeY, y, Interpolator.EASE_BOTH),
					new KeyValue(holeWidth, width, Interpolator.EASE_BOTH),
					new KeyValue(holeHeight, height, Interpolator.EASE_BOTH)));
			holeAnimation.playFromStart();
		}
		card.applyCss();
		card.autosize();
		placeCard(bounds);
	}

	// Next to the target where there is room, otherwise over it. Always inside the window
	private void placeCard(Bounds target) {
		double cardWidth = card.getWidth();
		double cardHeight = card.getHeight();
		double x;
		double y = target.getMinY();
		if (target.getMaxX() + CARD_GAP + cardWidth <= overlay.getWidth()) {
			x = target.getMaxX() + CARD_GAP;
		} else if (target.getMinX() - CARD_GAP - cardWidth >= 0) {
			x = target.getMinX() - CARD_GAP - cardWidth;
		} else if (target.getMaxY() + CARD_GAP + cardHeight <= overlay.getHeight()) {
			x = target.getMinX();
			y = target.getMaxY() + CARD_GAP;
		} else {
			x = target.getMinX();
			y = target.getMinY() - CARD_GAP - cardHeight;
		}
		card.relocate(clamp(x, overlay.getWidth() - cardWidth), clamp(y, overlay.getHeight() - cardHeight));
	}

	private static double clamp(double value, double max) {
		return Math.max(CARD_GAP, Math.min(value, max - CARD_GAP));
	}

	private void onResize() {
		redrawDim();
		if (active && currentStep >= 0) {
			// Later: the page below the overlay isn't laid out for the new size yet
			Platform.runLater(() -> {
				Node target = scene.lookup("#" + STEPS.get(currentStep).target());
				if (active && isShowing(target)) {
					spotlight(target, true);
				}
			});
		}
	}

	// The window with a hole around the target: two rectangles and the even-odd fill rule
	private void redrawDim() {
		double width = overlay.getWidth();
		double height = overlay.getHeight();
		double left = holeX.get();
		double top = holeY.get();
		double right = left + holeWidth.get();
		double bottom = top + holeHeight.get();
		dim.getElements().setAll(
				new MoveTo(0, 0), new HLineTo(width), new VLineTo(height), new HLineTo(0), new ClosePath(),
				new MoveTo(left, top), new HLineTo(right), new VLineTo(bottom), new HLineTo(left), new ClosePath());
	}

	private void buildOverlay() {
		overlay.setVisible(false);
		overlay.setPickOnBounds(true); // So that clicks anywhere stop here, also in the hole
		overlay.widthProperty().addListener((observable, oldValue, newValue) -> onResize());
		overlay.heightProperty().addListener((observable, oldValue, newValue) -> onResize());

		dim.setFill(Color.rgb(20, 28, 40, 0.62));
		dim.setStroke(null);
		dim.setFillRule(FillRule.EVEN_ODD);
		dim.setManaged(false);
		for (DoubleProperty property : List.of(holeX, holeY, holeWidth, holeHeight)) {
			property.addListener((observable, oldValue, newValue) -> redrawDim());
		}

		ring.getStyleClass().add("tour-ring");
		ring.setManaged(false);
		ring.xProperty().bind(holeX);
		ring.yProperty().bind(holeY);
		ring.widthProperty().bind(holeWidth);
		ring.heightProperty().bind(holeHeight);
		ringPulse.setFromX(1);
		ringPulse.setFromY(1);
		ringPulse.setToX(1.01);
		ringPulse.setToY(1.02);
		ringPulse.setAutoReverse(true);
		ringPulse.setCycleCount(4);

		progressLabel.getStyleClass().add("tour-card__progress");
		titleLabel.getStyleClass().add("tour-card__title");
		titleLabel.setWrapText(true);
		textLabel.setWrapText(true);
		skipButton.setOnAction(event -> finish());
		backButton.setOnAction(event -> move(-1, false));
		nextButton.setOnAction(event -> move(1, false));
		for (Button button : List.of(skipButton, backButton, nextButton)) {
			button.getStyleClass().add("standard-button");
			button.setFocusTraversable(false);
		}
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox buttons = new HBox(6, skipButton, spacer, backButton, nextButton);
		buttons.setAlignment(Pos.CENTER_LEFT);
		card.getChildren().addAll(progressLabel, titleLabel, textLabel, buttons);
		card.getStyleClass().add("tour-card");
		card.setPrefWidth(360);
		card.setManaged(false);

		overlay.getChildren().addAll(dim, ring, card);
	}
}
