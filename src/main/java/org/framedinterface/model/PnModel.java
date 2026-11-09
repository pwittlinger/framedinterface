package org.framedinterface.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.models.graphbased.NodeID;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;

/**
 * A Petri net loaded from a {@code .pnml} file (read as a data Petri net, see
 * {@link org.framedinterface.utils.ModelUtils#loadDpnModel(java.nio.file.Path, String, String)}), with its final marking. Its
 * activities are the labels of its visible transitions; all transition labels are lowercased when the net is loaded.
 * <p>
 * Replaying a trace (see {@link #updateMonitoringStates(List, boolean)}) plays the token game: an event whose transition is enabled fires
 * it, and an event whose transition isn't enabled is recorded as a violation and leaves the marking as it is. Two visualizations are
 * recorded for each position of the trace: one showing the violations since the net was last reset, and one showing all violations of the
 * trace ({@link #getVisualisationString(int, boolean)} picks one).
 */
public class PnModel extends AbstractModel  {

	public DataPetriNetsWithMarkings dataPetriNet;
	public String finalMarking; 
	public PetrinetSemantics petrinetSemantics;
	public ArrayList<String> visStrings;
	public ArrayList<String> visStringsViolation;
	private ArrayList<String> firedTransitions;
	private ArrayList<String> violatedFirings;
	private ArrayList<String> violatedFiringsKeep;
	private Map<String, Integer> violationCount;

	/**
	 * A trace entry that resets every Petri net to its initial marking when replayed. Followed by {@code #<model id>}, it only resets that
	 * net (see {@link #getResetActivity()}).
	 */
	public static final String RESET_PETRINET_ACTIVITY = "reset-petrinet";

	/**
	 * Creates a Petri net model in its initial marking; normally done by
	 * {@link org.framedinterface.utils.ModelUtils#loadDpnModel(java.nio.file.Path, String, String)}.
	 *
	 * @param modelId the model's id, unique among the loaded models
	 * @param modelName the model's name (its file name)
	 * @param activities the labels of the net's visible transitions, lowercased
	 * @param activityToEncodingMap each activity's encoding in the visualization, e.g. {@code n0}
	 * @param dataPetriNet the net, with its initial and final markings
	 */
	public PnModel(String modelId, String modelName, LinkedHashSet<String> activities, BidiMap<String, String> activityToEncodingMap, DataPetriNetsWithMarkings dataPetriNet) {
		super(modelId, modelName, activities, activityToEncodingMap, ModelType.PN); //TODO
		this.dataPetriNet = dataPetriNet;
		this.setFinalMarking();
		this.visStrings = new ArrayList<>();
		this.visStringsViolation = new ArrayList<>();
		this.firedTransitions = new ArrayList<>();
		this.violatedFirings = new ArrayList<>();
		this.violatedFiringsKeep = new ArrayList<>();
		this.violationCount = new HashMap<String, Integer>();

		initializeViolationCounts();

		this.petrinetSemantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		this.petrinetSemantics.initialize(dataPetriNet.getTransitions(), dataPetriNet.getInitialMarking());
	}

	/**
	 * Returns the trace entry that resets only this net when replayed (see {@link #updateMonitoringStates(List, boolean)}). The Data-Aware
	 * page uses it as the net's reset target in plans.
	 *
	 * @return the entry, e.g. {@code reset-petrinet#m1}
	 */
	public String getResetActivity() {
		return RESET_PETRINET_ACTIVITY + "#" + getModelId();
	}

	/**
	 * Tells whether, in the traces replayed since the last {@link #resetModel()}, some activity of the net occurred while its transition
	 * wasn't enabled. Violations before a reset entry are included.
	 *
	 * @return {@code true} if the net was violated
	 */
	public boolean isViolated() {
		return !violatedFiringsKeep.isEmpty();
	}

	/**
	 * Replays the given trace on the net, continuing from its current marking (callers reset the net first, see {@link #resetModel()}).
	 * Two visualizations are recorded per position, as described for the class: before the first entry, after each entry, and once more
	 * at the trace's end.
	 * <ul>
	 *   <li>An entry whose transition is enabled fires it.</li>
	 *   <li>An entry whose transition isn't enabled is recorded as a violation, and the marking is left as it is.</li>
	 *   <li>An entry that isn't a transition of the net is skipped. If it starts with {@link #RESET_PETRINET_ACTIVITY} and has no
	 *       {@code #<model id>}, or is this net's {@link #getResetActivity()}, the net first returns to its initial marking.</li>
	 * </ul>
	 * Silent (invisible) transitions are never fired. A transition counts as enabled if its name is part of an enabled transition's name,
	 * so e.g. {@code activitya} counts as enabled while only {@code activityab} is; firing it then fails, and the entry is neither fired
	 * nor recorded as a violation.
	 *
	 * @param activities the trace: activities, or plan steps {@code action;activity}; compared case-insensitively
	 * @param displayViolations unused, as both visualizations are recorded
	 */
	@Override
	public void updateMonitoringStates(List<String> activities, boolean displayViolations) {
		// TODO Auto-generated method stub

		// Initial set up when FrAIm first loads.
		if (this.visStrings.isEmpty()){
			this.visStrings.add(createVisualisationString());
			this.visStringsViolation.add(createVisualisationStringViolation());
		} else {
			// The prefix sync is part of the plan, so in order to keep indexing correct i reset everything
			// since I don't want to recompute the prefix
			resetVisualizationStrings();
			this.visStrings.add(createVisualisationString());
			this.visStringsViolation.add(createVisualisationStringViolation());
		}


		//Colour of the current state?
		// Or move token directly to the outgoing state?

		for (String act : activities) {
			
			String[] planAction = act.split(";");
			
			if (planAction.length == 2) {
				act = planAction[1];
			}
			act = act.toLowerCase(); //firedTransitions/violatedFirings/violationCount are all keyed lowercase (see resetModel()); activities coming from outside the graph-click flow (e.g. an imported XES trace) aren't pre-lowercased

			if (getTransitionViaLabel(this.dataPetriNet.getTransitions(), act) == null) {
				// Transition is not in PetriNet, cannot be fired.
				//"reset-petrinet..." resets every net, except "reset-petrinet#<modelId>" (Data-Aware plans, which name each net's constraint separately) only resets that one
				if (act.startsWith(RESET_PETRINET_ACTIVITY) && (!act.contains("#") || act.equals(getResetActivity().toLowerCase()))){
					
					this.petrinetSemantics.setCurrentState(this.dataPetriNet.getInitialMarking());
					this.violatedFirings.clear();
					this.firedTransitions.clear();
					//visStrings.add(createVisualisationString());
				}
				this.visStrings.add(createVisualisationString());
				this.visStringsViolation.add(createVisualisationStringViolation());
				continue;
			}
			
			Collection<Transition> currentlyEnabledTransitions = this.petrinetSemantics.getExecutableTransitions();
			
			if (!currentlyEnabledTransitions.toString().toLowerCase().contains(act)){
				/* TODO: Check that the transitions name have to match exactly, so that "ActivityA" will not be considered enabled if "ActivityAB" is enabled.
				*/
				// Transition is not enabled, set the Marking
				// Update the colour of the previously enabled transition
				// Note the colour of the new transition + place
				// Update Violation somewhere
				System.out.println("Enabled Transitions:"+ currentlyEnabledTransitions.toString() + " does not contain " + act);
				this.violatedFirings.add(act);
				this.violatedFiringsKeep.add(act);
				this.violationCount.put(act, this.violationCount.get(act)+1);

				
			}
			else {			
				try {
				// Always fire the transition
				this.petrinetSemantics.executeExecutableTransition(getTransitionViaLabel(this.dataPetriNet.getTransitions(), act));
				this.firedTransitions.add(act);
			} catch (IllegalTransitionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}

			this.visStrings.add(createVisualisationString());
			this.visStringsViolation.add(createVisualisationStringViolation());

		}
		// Duplicate final marking to not run into an index out of bounds exception
		this.visStrings.add(createVisualisationString());
		this.visStringsViolation.add(createVisualisationStringViolation());
		
	}

	/**
	 * Sets {@link #finalMarking} to the net's first final marking as text, e.g. {@code [(155,1)]}, or to an empty text if it can't be
	 * determined.
	 */
	public void setFinalMarking() {

		try {
			Marking[] finalMarking = dataPetriNet.getFinalMarkings();
			this.finalMarking = finalMarking[0].toString();
		} catch (Exception e) {
			// TODO: handle exception
			this.finalMarking = "";
		}

	}

	/**
	 * Visualizes the net in its current state, in Graphviz DOT. Transitions that fired are blue, transitions with a violation since the net
	 * was last reset are red, enabled transitions are white and the others grey; each transition's tooltip shows how often it was
	 * violated. Places show their token, if they have one, and the final marking's place is drawn as a double circle.
	 *
	 * @return the visualization, with {@code '} escaped for the visualization page's JavaScript. If building it fails, the error is
	 *         printed and the text built so far is returned.
	 */
	public String createVisualisationString() {
		StringBuilder sb = new StringBuilder();
		//Initialize Graph Root
	    		sb.append("digraph \"\" {");
	    		sb.append("rankdir=LR ");
				sb.append("id = \"graphRoot_" + getModelId() + "\" ");

	    		try {
	    			
	    			Collection<Place> allPlaces = this.dataPetriNet.getPlaces();
	    			Collection<Transition> allTransitions = this.dataPetriNet.getTransitions();
					Collection<Transition> allEnabledTransitions = this.petrinetSemantics.getExecutableTransitions();
		    		
		    		sb.append("node [shape=box];");
	    			for(Transition t : allTransitions) {
	    				if (!t.getLabel().isBlank()) {
	    					String activityEncoding = this.getActivityEncoding(t.getLabel());
	    					
	    					// Setting all regular transitions
	    					if ((this.firedTransitions.contains(t.getLabel().toLowerCase())) && (!this.violatedFirings.contains(t.getLabel().toLowerCase()))){
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", style=\"filled,dashed\", fillcolor=\"#66ccff\", color=black, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
	    					else if((this.violatedFirings.contains(t.getLabel().toLowerCase()))) {
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", style=\"filled,dashed\", fillcolor=\"#d44942\", color=black, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
	    					// Regular Transitions that are enabled
	    					else if ((allEnabledTransitions.contains(t))) {
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
							// Grey out the transition if it is not enabled
	    					else if (!((allEnabledTransitions.contains(t)) || this.firedTransitions.contains(t.getLabel().toLowerCase()))) {
	    						//sb.append(t.getLabel()+" [label=\""+t.getLabel()+"\", style=filled, fillcolor=blue]; ");
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\" , style=filled, fillcolour=lightgrey, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
							
						}
	    				
	    				
	    			}

	    			
		    		sb.append("node [shape=rect, style=filled, fillcolor=black; width=0.15, label=\"\"]; ");
	    			HashMap<NodeID, String> allSilentTransitions = new HashMap<NodeID, String>();
	    			int invCount = 0;
	    			for(Transition t :allTransitions) {
	    				if(t.isInvisible()) {
	    					String s_ = "t"+String.valueOf(invCount);
	    					allSilentTransitions.put(t.getId(),  s_);
	    					sb.append(s_+"; ");
	    					invCount++;
	    				}
	    			}
	    			
		    		
		    		//Marking initialMarking = dataPetriNet.getInitialMarking();

					// Potentially an issue if multiple places have a marking
					Marking currentMarking = petrinetSemantics.getCurrentState();
					List<Place> markingList = currentMarking.toList();
					

		    		//String initialPlaceLabel = initialMarking.toArray()[0].toString();
		    		Marking[] finalMarking = dataPetriNet.getFinalMarkings();
		    		String finalMarkingLabel = finalMarking[0].toArray()[0].toString();
		    		//System.out.println(initialMarking.toArray()[0].toString());
		    		
		    		//System.out.println("Initial Marking: " + initialPlaceLabel);
		    		//System.out.println("Final Marking: " + finalMarkingLabel);
		    		
		    		sb.append("node [shape=doublecircle, fillcolor=white]; ");
		    		sb.append(finalMarkingLabel+" [label=\""+finalMarkingLabel+"\"]; ");
		    		
		    		
		    		//sb.append("# Define places as small circles");
		    		sb.append("node [shape=circle, fillcolor=white]; ");
		    		
		    		for (Place p : allPlaces) {
						// Set a Token for each Place with a Token
						if (markingList.contains(p)) {
							sb.append(p.getLabel()+" [label=\"&#x2022;\", fontsize=\"40pt\", width=0.69, fixedsize=true, fillcolor=white]; ");
							
						}

		    			else if (p.getLabel() == finalMarkingLabel) {
		    				// Do nothing because handled above
		    			}
		    			else {
			    			sb.append(p.getLabel()+" [label=\""+p.getLabel()+"\"]; ");
		    			}
		    			

		    		}
	    			
	    			Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> allEdges = dataPetriNet.getEdges();
	    			
	    			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : allEdges) {
	    				String source = edge.getSource().getLabel().toString();
	    				String target = edge.getTarget().getLabel().toString();

	    				NodeID sourceNodeID = edge.getSource().getId();
	    				NodeID targetNodeID = edge.getTarget().getId();
	    				
	    				if ((allSilentTransitions.containsKey(sourceNodeID)) || (allSilentTransitions.containsKey(targetNodeID))) {
	    					if (allSilentTransitions.containsKey(sourceNodeID)) {
	    						sb.append(allSilentTransitions.get(sourceNodeID) + " -> " + target + ";");
	    					}
	    					else {
	    						sb.append(source + " -> " + allSilentTransitions.get(targetNodeID) + ";");
	    					}
	    				} else if ((allPlaces.contains(edge.getSource())) && (allTransitions.contains(edge.getTarget()))) {
							//System.out.println(source + " -> " + target + ";");
		    				sb.append(source + " -> " + this.getActivityEncoding(target) + ";");
	    				} else if ((allTransitions.contains(edge.getSource())) && (allPlaces.contains(edge.getTarget()))) {
							sb.append(this.getActivityEncoding(source) + " -> " + target + ";");
						}
	    				
	    			}
	    			sb.append("}");
	    			//System.out.println(sb.toString());
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	        
		
		return sb.toString().replace("'", "\\'");
	}

	/**
	 * Returns the net's visualization at the given position of the last replayed trace.
	 *
	 * @param activityIndex the position: 0 before the first entry, then one per entry, and the last one at the trace's end
	 * @param displayViolations {@code true} to show all violations of the trace, {@code false} only those since the net was last reset
	 * @return the visualization, in Graphviz DOT; the net's current state if no trace was replayed yet
	 */
	@Override
	public String getVisualisationString(int activityIndex, boolean displayViolations) {
				if (this.visStrings.isEmpty()) return createVisualisationString();
				if (displayViolations) return this.visStringsViolation.get(activityIndex);
	    		return this.visStrings.get(activityIndex);
	}

	/**
	 * Finds a place by its label, compared case-insensitively.
	 *
	 * @param allPlaces the places to search
	 * @param pLabel the label
	 * @return the first place with that label, or {@code null} if there's none
	 */
	public static Place getPlaceViaLabel(Collection<Place> allPlaces, String pLabel) {
		for (Place p : allPlaces) {
			if(p.getLabel().equalsIgnoreCase(pLabel)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Finds a transition by its label, compared case-insensitively.
	 *
	 * @param allTransitions the transitions to search
	 * @param tLabel the label
	 * @return the first transition with that label, or {@code null} if there's none
	 */
		public static Transition getTransitionViaLabel(Collection<Transition> allTransitions, String tLabel) {
		for (Transition t : allTransitions) {
			if(t.getLabel().equalsIgnoreCase(tLabel)) {
				return t;
			}
		}
		return null;
	}
	
	/**
	 * Returns a marking with a token in each input place of the transition with the given label. Not used anywhere in FrAIm.
	 *
	 * @param tLabel the transition's label, compared case-insensitively
	 * @return the marking
	 */
	public Marking getAllIncomingMarkings(String tLabel) {
		Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> allEdges = this.dataPetriNet.getEdges();
		Collection<Place> allPlaces_ = this.dataPetriNet.getPlaces();
		Marking m_ = new Marking();
		ArrayList<String> inEdges = new ArrayList<String>();
		
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : allEdges) {
			PetrinetNode source = edge.getSource();
			PetrinetNode target = edge.getTarget();
			
			if (target.getLabel().strip().equalsIgnoreCase(tLabel)) {
				m_.add(getPlaceViaLabel(allPlaces_, source.getLabel()));
				inEdges.add(source.getLabel());
			}
			
		}
		return m_;
	}

	/**
	 * Clears the recorded visualizations, e.g. before a trace is replayed again.
	 */
	public void resetVisualizationStrings(){
		this.visStrings.clear();
		this.visStringsViolation.clear();
	}

	/**
	 * Returns the input places of the transition with the given label. Not used anywhere in FrAIm.
	 *
	 * @param tLabel the transition's label, compared case-insensitively
	 * @return the input places
	 */
	public ArrayList<Place> getAllIncomingPlaces(String tLabel) {
		Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> allEdges = this.dataPetriNet.getEdges();
		Collection<Place> allPlaces_ = this.dataPetriNet.getPlaces();
		//ArrayList<String> inEdges = new ArrayList<String>();

		ArrayList<Place> incomingPlaces = new ArrayList<>();
		
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : allEdges) {
			PetrinetNode source = edge.getSource();
			PetrinetNode target = edge.getTarget();
			
			if (target.getLabel().strip().equalsIgnoreCase(tLabel)) {
				incomingPlaces.add(getPlaceViaLabel(allPlaces_, source.getLabel()));
				//inEdges.add(source.getLabel());
			}
			
		}
		return incomingPlaces;
	}

	/**
	 * Returns the net to its initial marking and forgets the replayed trace: its fired transitions, violations and violation counts, and
	 * the recorded visualizations.
	 */
	@Override
	public void resetModel(){
		resetVisualizationStrings();
		this.firedTransitions.clear();
		this.violatedFirings.clear();
		this.violatedFiringsKeep.clear();
		initializeViolationCounts();

		this.petrinetSemantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		this.petrinetSemantics.initialize(dataPetriNet.getTransitions(), dataPetriNet.getInitialMarking());
	}

	/**
	 * Sets every transition's violation count to 0, keyed by its lowercased label.
	 */
	private void initializeViolationCounts(){
		for (Transition t : this.dataPetriNet.getTransitions()) {
			this.violationCount.put(t.getLabel().toLowerCase(), 0);
		}
	}


	/**
	 * Like {@link #createVisualisationString()}, but shows every transition with a violation since the last {@link #resetModel()} as red,
	 * including violations before a reset entry.
	 *
	 * @return the visualization, as for {@link #createVisualisationString()}
	 */
	public String createVisualisationStringViolation() {
		StringBuilder sb = new StringBuilder();
		//Initialize Graph Root
	    		sb.append("digraph \"\" {");
	    		sb.append("rankdir=LR ");
				sb.append("id = \"graphRoot_" + getModelId() + "\" ");

	    		try {
	    			
	    			Collection<Place> allPlaces = this.dataPetriNet.getPlaces();
	    			Collection<Transition> allTransitions = this.dataPetriNet.getTransitions();
					Collection<Transition> allEnabledTransitions = this.petrinetSemantics.getExecutableTransitions();
		    		
		    		sb.append("node [shape=box];");
	    			for(Transition t : allTransitions) {
	    				if (!t.getLabel().isBlank()) {
	    					String activityEncoding = this.getActivityEncoding(t.getLabel());
	    					
	    					// Setting all regular transitions
	    					if ((this.firedTransitions.contains(t.getLabel().toLowerCase())) && (!this.violatedFiringsKeep.contains(t.getLabel().toLowerCase()))){
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", style=\"filled,dashed\", fillcolor=\"#66ccff\", color=black, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
	    					else if((this.violatedFiringsKeep.contains(t.getLabel().toLowerCase()))) {
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", style=\"filled,dashed\", fillcolor=\"#d44942\", color=black, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
	    					// Regular Transitions that are enabled
	    					else if ((allEnabledTransitions.contains(t))) {
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
							// Grey out the transition if it is not enabled
	    					else if (!((allEnabledTransitions.contains(t)) || this.firedTransitions.contains(t.getLabel().toLowerCase()))) {
	    						//sb.append(t.getLabel()+" [label=\""+t.getLabel()+"\", style=filled, fillcolor=blue]; ");
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\" , style=filled, fillcolour=lightgrey, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
							
						}
	    				
	    				
	    			}

	    			
		    		sb.append("node [shape=rect, style=filled, fillcolor=black; width=0.15, label=\"\"]; ");
	    			HashMap<NodeID, String> allSilentTransitions = new HashMap<NodeID, String>();
	    			int invCount = 0;
	    			for(Transition t :allTransitions) {
	    				if(t.isInvisible()) {
	    					String s_ = "t"+String.valueOf(invCount);
	    					allSilentTransitions.put(t.getId(),  s_);
	    					sb.append(s_+"; ");
	    					invCount++;
	    				}
	    			}
	    			
		    		
		    		//Marking initialMarking = dataPetriNet.getInitialMarking();

					// Potentially an issue if multiple places have a marking
					Marking currentMarking = petrinetSemantics.getCurrentState();
					List<Place> markingList = currentMarking.toList();
					

		    		//String initialPlaceLabel = initialMarking.toArray()[0].toString();
		    		Marking[] finalMarking = dataPetriNet.getFinalMarkings();
		    		String finalMarkingLabel = finalMarking[0].toArray()[0].toString();
		    		//System.out.println(initialMarking.toArray()[0].toString());
		    		
		    		//System.out.println("Initial Marking: " + initialPlaceLabel);
		    		//System.out.println("Final Marking: " + finalMarkingLabel);
		    		
		    		sb.append("node [shape=doublecircle, fillcolor=white]; ");
		    		sb.append(finalMarkingLabel+" [label=\""+finalMarkingLabel+"\"]; ");
		    		
		    		
		    		//sb.append("# Define places as small circles");
		    		sb.append("node [shape=circle, fillcolor=white]; ");
		    		
		    		for (Place p : allPlaces) {
						// Set a Token for each Place with a Token
						if (markingList.contains(p)) {
							sb.append(p.getLabel()+" [label=\"&#x2022;\", fontsize=\"40pt\", width=0.69, fixedsize=true, fillcolor=white]; ");
							
						}

		    			else if (p.getLabel() == finalMarkingLabel) {
		    				// Do nothing because handled above
		    			}
		    			else {
			    			sb.append(p.getLabel()+" [label=\""+p.getLabel()+"\"]; ");
		    			}
		    			

		    		}
	    			
	    			Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> allEdges = dataPetriNet.getEdges();
	    			
	    			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : allEdges) {
	    				String source = edge.getSource().getLabel().toString();
	    				String target = edge.getTarget().getLabel().toString();

	    				NodeID sourceNodeID = edge.getSource().getId();
	    				NodeID targetNodeID = edge.getTarget().getId();
	    				
	    				if ((allSilentTransitions.containsKey(sourceNodeID)) || (allSilentTransitions.containsKey(targetNodeID))) {
	    					if (allSilentTransitions.containsKey(sourceNodeID)) {
	    						sb.append(allSilentTransitions.get(sourceNodeID) + " -> " + target + ";");
	    					}
	    					else {
	    						sb.append(source + " -> " + allSilentTransitions.get(targetNodeID) + ";");
	    					}
	    				} else if ((allPlaces.contains(edge.getSource())) && (allTransitions.contains(edge.getTarget()))) {
							//System.out.println(source + " -> " + target + ";");
		    				sb.append(source + " -> " + this.getActivityEncoding(target) + ";");
	    				} else if ((allTransitions.contains(edge.getSource())) && (allPlaces.contains(edge.getTarget()))) {
							sb.append(this.getActivityEncoding(source) + " -> " + target + ";");
						}
	    				
	    			}
	    			sb.append("}");
	    			//System.out.println(sb.toString());
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	        
		
		return sb.toString().replace("'", "\\'");
	}
}
