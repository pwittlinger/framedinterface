package org.framedinterface.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * A named item with a number of overlaps and a flag for whether it has global scope. Both are held in JavaFX properties, so that controls
 * can be bound to them.
 * <p>
 * This class isn't used anywhere in FrAIm: nothing refers to it, and it has been unchanged since the project's first commit.
 */
public class ScopeSelection {
	private String itemName;
	private IntegerProperty overlapsCount = new SimpleIntegerProperty();
	private BooleanProperty isGlobalScopeProperty = new SimpleBooleanProperty();
	
	/**
	 * Creates an item with no overlaps and without global scope.
	 *
	 * @param itemName the item's name
	 */
	public ScopeSelection (String itemName) {
		this.itemName = itemName;
		this.overlapsCount.set(0);
		isGlobalScopeProperty.set(false);
	}
	
	/**
	 * Returns the item's name.
	 *
	 * @return the name
	 */
	public String getItemName() {
		return itemName;
	}
	
	/**
	 * Returns the property holding the number of overlaps. Its name is missing the first "o" of {@code overlapsCountProperty()}, so JavaFX
	 * doesn't find it by its naming convention, e.g. in a {@code PropertyValueFactory}.
	 *
	 * @return the number of overlaps, as a property
	 */
	public IntegerProperty verlapsCountProperty() {
		return this.overlapsCount;
	}
	/**
	 * Returns the number of overlaps.
	 *
	 * @return the number of overlaps
	 */
	public Integer getOverlapsCount() {
		return overlapsCount.get();
	}
	/**
	 * Sets the number of overlaps.
	 *
	 * @param overlapsCount the number of overlaps; must not be {@code null}
	 */
	public void setOverlapsCount(Integer overlapsCount) {
		this.overlapsCount.set(overlapsCount);
	}
	
	/**
	 * Returns the property holding whether the item has global scope.
	 *
	 * @return whether the item has global scope, as a property
	 */
	public BooleanProperty isGlobalScopeProperty() {
		return this.isGlobalScopeProperty;
	}
	/**
	 * Tells whether the item has global scope.
	 *
	 * @return {@code true} if it has global scope
	 */
	public Boolean getIsGlobalScope() {
		return isGlobalScopeProperty.get();
	}
	/**
	 * Sets whether the item has global scope.
	 *
	 * @param isGlobalScope {@code true} to give the item global scope
	 */
	public void setIsGlobalScope(boolean isGlobalScope) {
		this.isGlobalScopeProperty.set(isGlobalScope);
	}
}
