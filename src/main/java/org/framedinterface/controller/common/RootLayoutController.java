package org.framedinterface.controller.common;

import java.io.IOException;
import java.util.EnumMap;

import org.framedinterface.util.PageType;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class RootLayoutController extends AbstractController {

	@FXML
	private NavigationSidebarController navigationSidebarController;
	@FXML
	private HBox rootRegion;

	private PageType currentPageType;
	private EnumMap<PageType, Region> loadedPages = new EnumMap<PageType, Region>(PageType.class);
	private EnumMap<PageType, AbstractController> loadedController = new EnumMap<PageType, AbstractController>(PageType.class);

	@FXML
	private void initialize() {
		navigationSidebarController.setNavigationCallback(pageType -> navigateToPage(pageType));
	}

	// Called explicitly by App.java after setStage(), so that the default page's controller
	// already has access to the stage (e.g. for file choosers) once it is loaded.
	public void showDefaultPage() {
		navigateToPage(PageType.DATA_AGNOSTIC);
		navigationSidebarController.updateHighlight(PageType.DATA_AGNOSTIC);
	}

	private void navigateToPage(PageType pageType) {
		if (currentPageType != pageType) {
			Region loadedPage = loadedPages.get(pageType);
			if (loadedPage == null) {
				try {
					loadedPage = loadPage(pageType);
				} catch (IOException | IllegalStateException e) {
					e.printStackTrace();
					return;
				}
			}
			if (rootRegion.getChildren().size() < 2) {
				rootRegion.getChildren().add(loadedPage);
			} else {
				rootRegion.getChildren().set(1, loadedPage);
			}
			HBox.setHgrow(loadedPage, Priority.ALWAYS); // Makes sure that the page fills available space
			currentPageType = pageType;
		}
	}

	private Region loadPage(PageType pageTypeToLoad) throws IOException, IllegalStateException {
		String fxmlPath = "/org/framedinterface/" + pageTypeToLoad.getPathToFxml();
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
		Region loadedPage = fxmlLoader.load();
		((AbstractController) fxmlLoader.getController()).setStage(this.getStage());

		loadedPages.put(pageTypeToLoad, loadedPage);
		loadedController.put(pageTypeToLoad, fxmlLoader.getController());
		return loadedPage;
	}

}
