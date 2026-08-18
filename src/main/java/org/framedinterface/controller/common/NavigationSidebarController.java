package org.framedinterface.controller.common;

import java.util.function.Consumer;

import org.framedinterface.util.PageType;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.VBox;

public class NavigationSidebarController extends AbstractController {

	private final PseudoClass highlightedClass = PseudoClass.getPseudoClass("highlighted");

	@FXML
	private VBox rootRegion;
	@FXML
	private Button dataAgnosticButton;
	@FXML
	private Button dataAwareButton;
	@FXML
	private Button processFrameOverviewButton;
	@FXML
	private FontIcon minimizeIcon;

	private boolean isMinimized;
	private Button currentlyHighlighted;

	private Consumer<PageType> navigationCallback;

	public void setNavigationCallback(Consumer<PageType> navigationCallback) {
		this.navigationCallback = navigationCallback;
	}

	@FXML
	private void openDataAgnostic() {
		handleNavigation(PageType.DATA_AGNOSTIC);
	}

	@FXML
	private void openDataAware() {
		handleNavigation(PageType.DATA_AWARE);
	}

	@FXML
	private void openProcessFrameOverview() {
		handleNavigation(PageType.PROCESS_FRAME_OVERVIEW);
	}

	private void handleNavigation(PageType pageType) {
		if (navigationCallback != null) {
			navigationCallback.accept(pageType);
		}
		updateHighlight(pageType);
	}

	// Made public so that highlights can be updated even if navigation happens outside this class
	public void updateHighlight(PageType pageType) {
		if (currentlyHighlighted != null) {
			currentlyHighlighted.pseudoClassStateChanged(highlightedClass, false);
		}
		switch (pageType) {
		case DATA_AGNOSTIC:
			dataAgnosticButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = dataAgnosticButton;
			break;
		case DATA_AWARE:
			dataAwareButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = dataAwareButton;
			break;
		case PROCESS_FRAME_OVERVIEW:
			processFrameOverviewButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = processFrameOverviewButton;
			break;
		default:
			break;
		}
	}

	@FXML
	private void toggleMinimize() {
		if (isMinimized) {
			for (Node node : rootRegion.getChildren()) {
				if (node instanceof Button) {
					((Button) node).setContentDisplay(ContentDisplay.LEFT);
				}
			}
			minimizeIcon.setIconLiteral("fa-angle-double-left");
			isMinimized = false;
		} else {
			for (Node node : rootRegion.getChildren()) {
				if (node instanceof Button) {
					((Button) node).setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				}
			}
			minimizeIcon.setIconLiteral("fa-angle-double-right");
			isMinimized = true;
		}
	}

}
