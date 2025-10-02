package main;

import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.visualization.Visualization;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProductMealy{

    CompactMealy<String, Word<String>> fsm;
    int components_count;
    List<String> wrongSyncs;
    public ProductMealy( CompactMealy<String, Word<String>> m1) {
        this.fsm = m1;
        wrongSyncs = new ArrayList<>();
    }

    public ProductMealy mergeFSMs(CompactMealy<String, Word<String>> mealy_2, int componentsCount){
        // TODO Auto-generated method stub

        CompactMealy<String, Word<String>> mealy_1 = fsm;
        Collection<Integer> states_1 = mealy_1.getStates();
        Collection<Integer> states_2 = mealy_2.getStates();

        Alphabet<String> alphabet_1 = mealy_1.getInputAlphabet();
        Alphabet<String> alphabet_2 = mealy_2.getInputAlphabet();

        // Creating the alphabet of the merged FSM
        Alphabet<String> alphabet = Alphabets.fromCollection(MergeAlphabet(alphabet_1, alphabet_2));
//		System.out.println("alphabet: " + alphabet.toString());
        CompactMealy<String, Word<String>> mealy = new CompactMealy(alphabet);
//		System.out.println(mealy.getStates());

        int states_num = states_1.size() * states_2.size();
        int states_map[][] = new int[states_num][3];
        for (int[] array : states_map) {
            Arrays.fill(array, -1);
        }
        int merged_state = 0;
        states_map[merged_state][0] = states_map[merged_state][1] = states_map[merged_state][2] = 0;

        Queue<Integer> states_queue = new LinkedList<>();
        states_queue.add(merged_state);

        while (states_queue.size() != 0) {
            int current_state = states_queue.remove();
            mealy.addState();
//            if(componentsCount < 8 && mealy.getStates().size() > 16000)
//                break;
            if(mealy.getStates().size() > 30000)
                break;
//			System.out.println("\ncurrent state: " + current_state);
            int s_1 = states_map[current_state][1];
            int s_2 = states_map[current_state][2];
//			System.out.println("s_1: " + s_1);
//			System.out.println("s_2: " + s_2);

            for (String a : alphabet) {
//				System.out.println(a);

                int new_s_1 = -10;
                int new_s_2 = -10;

                Word<String> output_1 = null;
                Word<String> output_2 = null;

                @Nullable
                CompactMealyTransition<Word<String>> transition_1 = null;
                @Nullable
                CompactMealyTransition<Word<String>> transition_2 = null;

                if (alphabet_1.contains(a)) {
                    transition_1 = mealy_1.getTransition(s_1, a);
                }
                if (alphabet_2.contains(a)) {
                    transition_2 = mealy_2.getTransition(s_2, a);
                }
                if (transition_1 != null || transition_2 != null) {
                    if (transition_1 != null && transition_2 != null) {
                        output_1 = transition_1.getOutput();
                        new_s_1 = transition_1.getSuccId();

                        output_2 = transition_2.getOutput();
                        new_s_2 = transition_2.getSuccId();

                    } else if (transition_1 != null && transition_2 == null) {
                        output_1 = transition_1.getOutput();
                        new_s_1 = transition_1.getSuccId();

                        new_s_2 = s_2;

                    } else if (transition_1 == null && transition_2 != null) {
                        new_s_1 = s_1;

                        output_2 = transition_2.getOutput();
                        new_s_2 = transition_2.getSuccId();

                    }

                    int equivalent_state = EquivalentState(new_s_1, new_s_2, states_map);
//					System.out.println("equivalent state:" + equivalent_state);

                    String output_1_string = "";
                    String output_2_string = "";
                    if (output_1 != null) {
                        output_1_string = output_1.toString();
                    }
                    if (output_2 != null) {
                        output_2_string = output_2.toString();
                    }

                    if(output_1 != null && output_2 != null) {
                        if(!output_1.equals(output_2)) {
                            if(!wrongSyncs.contains(a))
                                wrongSyncs.add(a);
                        }
                    }

                    List<String> output_1_list = new ArrayList<String>(Arrays.asList(output_1_string.split(",")));
                    List<String> output_2_list = new ArrayList<String>(Arrays.asList(output_2_string.split(",")));
                    List<String> output_list = new ArrayList<>();
                    for (String string_1 : output_1_list) {
                        if (!string_1.equals("")) {
                            output_list.add(string_1);
                        }
                    }

                    for (String string_1 : output_2_list) {
                        if (!string_1.equals("") && !output_list.contains(string_1)) {
                            output_list.add(string_1);
                        }
                    }

//					System.out.println("output list:" + output_list);
                    String output_string = String.join(",", output_list);
                    Word<String> output = Word.fromSymbols(output_string);
//					System.out.println("output:" + output);

                    if (equivalent_state == -1) {
                        merged_state += 1;
                        mealy.setTransition(current_state, a, merged_state, output);
                        states_queue.add(merged_state);
                        states_map[merged_state][0] = merged_state;
                        states_map[merged_state][1] = new_s_1;
                        states_map[merged_state][2] = new_s_2;
                    } else {
                        mealy.setTransition(current_state, a, equivalent_state, output);
                    }

//					System.out
//							.println(current_state + " " + a + "/ " + mealy.getTransition(current_state, a).getOutput()
//									+ " " + mealy.getTransition(current_state, a).getSuccId() + "\n");
                }

            }

        }

        mealy.setInitialState(0);
//		System.out.println("\n" + Arrays.deepToString(states_map));
        this.fsm = mealy;
        return this;
    }

    private Collection MergeAlphabet(Alphabet<String> a_1, Alphabet<String> a_2){
        // TODO Auto-generated method stub
        Set<String> set_1 = new LinkedHashSet<>();
        set_1.addAll(a_2);
        set_1.addAll(a_1);
        List<String> list_1 = new ArrayList<>(set_1);
        return list_1;
    }

    private static int EquivalentState ( int s1, int s2, int[][] states_map_1){
        // TODO Auto-generated method stub
        int length_1 = states_map_1.length;
        for (int i = 0; i < length_1; i++) {
            if (states_map_1[i][0] != (-1)) {
                if (states_map_1[i][1] == s1 && states_map_1[i][2] == s2) {
                    return i;
                }
            }
        }
        return -1;
    }


    public CompactMealy<String, Word<String>> getMachine () {
        return fsm;
    }

    public void setMachine (CompactMealy <String, Word<String>> machine){
        this.fsm = machine;
    }
    public List<String> getWrongSyncs() { return wrongSyncs; }

    public int getComponents_count() {
        return components_count;
    }

    public void setComponents_count(int components_count) {
        this.components_count = components_count;
    }
}