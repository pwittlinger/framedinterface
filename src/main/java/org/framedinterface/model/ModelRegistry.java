package org.framedinterface.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ModelRegistry {

	private static final ModelRegistry INSTANCE = new ModelRegistry();

	private final ObservableList<AbstractModel> models = FXCollections.observableArrayList();
	private int modelCounter = 0;

	private ModelRegistry() {
	}

	public static ModelRegistry getInstance() {
		return INSTANCE;
	}

	public ObservableList<AbstractModel> getModels() {
		return models;
	}

	public String nextModelId() {
		return "m" + (modelCounter++);
	}

}
