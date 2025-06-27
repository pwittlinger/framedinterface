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

	@Override
	public void updateMonitoringStates(List<String> activities, boolean displayViolations) {
		// TODO Auto-generated method stub

		//Marking curState = this.petrinetSemantics.getCurrentState();
		if (this.visStrings.isEmpty()){
			this.visStrings.add(createVisualisationString());
			this.visStringsViolation.add(createVisualisationStringViolation());
		} else {
			// The prefix sync is part of the plan, so in order to keep indexing correct I don't want to recompute the prefix
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

			if (getTransitionViaLabel(this.dataPetriNet.getTransitions(), act) == null) {
				// Transition is not in PetriNet, cannot be fired.
				if (act.startsWith("reset-petrinet")){
					
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
			//if (this.dataPetriNet.getTransitions().)
			if (!currentlyEnabledTransitions.toString().toLowerCase().contains(act)){
				// Transition is not enabled, set the Marking
				// Update the colour of the previously enabled transition
				// Note the colour of the new transition + place
				// Update Violation somewhere
				System.out.println(currentlyEnabledTransitions.toString() + " does not contain " + act);
				this.violatedFirings.add(act);
				this.violatedFiringsKeep.add(act);
				this.violationCount.put(act, this.violationCount.get(act)+1);

				
			}
			else {			try {
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

	public void setFinalMarking() {

		try {
			Marking[] finalMarking = dataPetriNet.getFinalMarkings();
			this.finalMarking = finalMarking[0].toString();
		} catch (Exception e) {
			// TODO: handle exception
			this.finalMarking = "";
		}

	}

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
		    		System.out.println("Final Marking: " + finalMarkingLabel);
		    		
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

	@Override
	public String getVisualisationString(int activityIndex, boolean displayViolations) {
				if (this.visStrings.isEmpty()) return createVisualisationString();
				if (displayViolations) return this.visStringsViolation.get(activityIndex);
	    		return this.visStrings.get(activityIndex);
	}

	public static Place getPlaceViaLabel(Collection<Place> allPlaces, String pLabel) {
		for (Place p : allPlaces) {
			if(p.getLabel().equalsIgnoreCase(pLabel)) {
				return p;
			}
		}
		return null;
	}

		public static Transition getTransitionViaLabel(Collection<Transition> allTransitions, String tLabel) {
		for (Transition t : allTransitions) {
			if(t.getLabel().equalsIgnoreCase(tLabel)) {
				return t;
			}
		}
		return null;
	}
	
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

	public void resetVisualizationStrings(){
		this.visStrings.clear();
		this.visStringsViolation.clear();
	}

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

	private void initializeViolationCounts(){
		for (Transition t : this.dataPetriNet.getTransitions()) {
			this.violationCount.put(t.getLabel().toLowerCase(), 0);
		}
	}


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
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", style=\"filled,dashed\", fillcolor=lightblue, color=black, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
	    					}
	    					else if((this.violatedFiringsKeep.contains(t.getLabel().toLowerCase()))) {
	    						sb.append(activityEncoding+" [label=\""+t.getLabel()+"\", style=\"filled,dashed\", fillcolor=red, color=black, tooltip=\"violationCount=" +this.violationCount.get(t.getLabel().toLowerCase()) +"\"]; ");
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
		    		System.out.println("Final Marking: " + finalMarkingLabel);
		    		
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
