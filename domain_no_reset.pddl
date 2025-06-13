  (define (domain alignment)
    (:requirements :disjunctive-preconditions :conditional-effects :universal-preconditions)
    (:types
        state
        trace_state automaton_state petrinet_state - state
        alphabet
        constraint activity - alphabet
    )
    (:predicates
        (cur_state ?s - state)
        (automaton ?s1 - state ?e - alphabet ?s2 - state)
         
        (final_state ?s - state)
        (dummy_state ?s - state)

        (is_sink ?s - state)
        (blocked ?s - state)

        (cur_trace_state ?s - state)
        (trace ?s1 - trace_state ?e - activity ?s2 - trace_state)
        (recovery_finished) 
        
    )
    

    (:functions 
        (total-cost)
    )

    (:action sync
        :parameters (?e - activity)
        :precondition (and (recovery_finished) 
                           (forall (?s1 ?s2 - petrinet_state) (imply (and (cur_state ?s1) (automaton ?s1 ?e ?s2)) (not (dummy_state ?s2))))
                           (forall (?s - state) (not (blocked ?s)))
                      )                                                                                                             
        :effect(and
                    (forall (?s1 ?s2 - state)
                        (and (when (and (cur_state ?s1) (automaton ?s1 ?e ?s2)) (and (not (cur_state ?s1)) (cur_state ?s2)))  
                             (when (and (cur_state ?s1) (automaton ?s1 ?e ?s2) (is_sink ?s2)) (blocked ?s2))
                        )         
                    )
                )
    )

    (:action violate_pn
        :parameters (?e - activity)
        :precondition (and (recovery_finished)) 
        :effect(and
                    (forall (?s1 ?s2 - state)
                        (and (when (and (cur_state ?s1) (automaton ?s1 ?e ?s2)) (and (not (cur_state ?s1)) (cur_state ?s2)))
                             (when (and (cur_state ?s1) (automaton ?s1 ?e ?s2) (is_sink ?s2)) (blocked ?s2))
                        )
                    )
                    (increase (total-cost) 3)
                )
    ) 

    (:action violate_decl
        :parameters (?s1 ?s2 - automaton_state ?e - constraint)
        :precondition (and (cur_state ?s1) (dummy_state ?s2) (automaton ?s1 ?e ?s2)) 
        :effect(and
                    (not (cur_state ?s1)) (cur_state ?s2)
                    (when (and (is_sink ?s1) (blocked ?s1)) (not (blocked ?s1)))
                    (increase (total-cost) 3)
                )
    ) 



    (:action prefix_sync
        :parameters (?s1 ?s2 - trace_state ?e - activity)
        :precondition (and (cur_trace_state ?s1) (trace ?s1 ?e ?s2)
                            (forall (?s5 ?s6 - petrinet_state) (imply (and (cur_state ?s5) (automaton ?s5 ?e ?s6)) (not (dummy_state ?s6))))
                            (forall (?s - state) (not (blocked ?s)))
                            
                      )
        :effect (and
                    (not (cur_trace_state ?s1)) (cur_trace_state ?s2)
                    (forall (?s3 ?s4 - state)
                        (and (when (and (cur_state ?s3) (automaton ?s3 ?e ?s4)) (and (not (cur_state ?s3)) (cur_state ?s4))) 
                             (when (and (cur_state ?s3) (automaton ?s3 ?e ?s4) (is_sink ?s4)) (blocked ?s4))
                        )
                    )
                    (when (final_state ?s2) (recovery_finished))
         )
    )    

    (:action prefix_violate_pn
        :parameters (?s1 ?s2 - trace_state ?e - activity)
        :precondition (and (cur_trace_state ?s1) (trace ?s1 ?e ?s2)
                           (forall (?s - state) (not (blocked ?s)))
                      )
        :effect (and
                    (not (cur_trace_state ?s1)) (cur_trace_state ?s2)
                    (forall (?s3 ?s4 - state)
                        (and (when (and (cur_state ?s3) (automaton ?s3 ?e ?s4)) (and (not (cur_state ?s3)) (cur_state ?s4)))
                             (when (and (cur_state ?s3) (automaton ?s3 ?e ?s4) (is_sink ?s4)) (blocked ?s4))         
                        )
                    )
                    (when (final_state ?s2) (recovery_finished))
                    (increase (total-cost) 3)
         )
    )

    

)

