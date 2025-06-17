package org.framedinterface.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.processmining.datapetrinets.DataPetriNetsWithMarkings;
import org.processmining.models.graphbased.NodeID;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

public class PnModel extends AbstractModel  {

	public DataPetriNetsWithMarkings dataPetriNet;
	public String finalMarking; 

	public PnModel(String modelId, String modelName, Set<String> activityNames, DataPetriNetsWithMarkings dataPetriNet) {
		super(modelId, modelName, null, null, ModelType.PN); //TODO
		this.dataPetriNet = dataPetriNet;
		this.setFinalMarking();
	}

	@Override
	public void updateMonitoringStates(List<String> activities) {
		// TODO Auto-generated method stub
		
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

	@Override
	public String getVisualisationString(int activityIndex) {
	    		StringBuilder sb = new StringBuilder();
	    		sb.append("digraph \"\" {");
	    		sb.append("rankdir=LR ");
				sb.append("id = \"graphRoot_" + getModelId() + "\" ");


	    		try {
	    			
	    			Collection<Place> allPlaces = this.dataPetriNet.getPlaces();
	    			Collection<Transition> allTransitions = this.dataPetriNet.getTransitions();
	    			
		    		//sb.append("# Define transitions\n");
		    		sb.append("node [shape=box];");
	    			for(Transition t : allTransitions) {
	    				if (!t.getLabel().isBlank()) {
	    					sb.append(t.getLabel()+"; ");
	    				}
	    			}
	    			
	    			//sb.append("\n");
		    		//sb.append("# Define silent transitions \n");
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
	    			
	    			//sb.append("\n");
		    		
		    		Marking initialMarking = dataPetriNet.getInitialMarking();
		    		String initialPlaceLabel = initialMarking.toArray()[0].toString();
		    		Marking[] finalMarking = dataPetriNet.getFinalMarkings();
		    		String finalMarkingLabel = finalMarking[0].toArray()[0].toString();
		    		//System.out.println(initialMarking.toArray()[0].toString());
		    		
		    		System.out.println("Initial Marking: " + initialPlaceLabel);
		    		System.out.println("Final Marking: " + finalMarkingLabel);
		    		
		    		sb.append("node [shape=doublecircle, fillcolor=white]; ");
		    		sb.append(finalMarkingLabel+" [label=\""+finalMarkingLabel+"\"]; ");
		    		
		    		
		    		//sb.append("# Define places as small circles");
		    		sb.append("node [shape=circle, fillcolor=white]; ");
		    		
		    		for (Place p : allPlaces) {
		    			//sb.append("p"+p.getLabel()+"; ");
		    			if (p.getLabel() == initialPlaceLabel) {
		    				//sb.append(p.getLabel()+" [label=\""+p.getLabel()+"\", fillcolor=green]; ");
							sb.append(p.getLabel()+" [label=\"&#x2022;\", fontsize=\"40pt\", width=0.55, fixedsize=true, fillcolor=white]; ");
							
		    			}
		    			else if (p.getLabel() == finalMarkingLabel) {
		    				// Do nothing
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
	    					sb.append(source + " -> " + target + ";");
	    				} else if ((allTransitions.contains(edge.getSource())) && (allPlaces.contains(edge.getTarget()))) {
							sb.append(source + " -> " + target + ";");
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
