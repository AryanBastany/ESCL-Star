package main;

import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.api.statistic.StatisticSUL;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.util.Experiment;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.words.impl.ListAlphabet;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.ArrayList;
import java.util.List;

import de.learnlib.api.oracle.MembershipOracle;

import java.util.*;
import java.util.logging.Logger;

import java.io.IOException;  // Import the IOException class to handle errors
import java.io.FileWriter;   // Import the FileWriter class

public class SclStar {
    private Alphabet<String> alphabet;
    private EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle ;
    private EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> partialEqOracle ;
    private MembershipOracle<String, Word<Word<String>>> mqOracle;
    private List<Alphabet<String>> sigmaFamily;
    private Counter round_counter;
    private Counter eq_counter;
    private Logger logger;
    private List<Alphabet<String>> inputComponentsActs;
    private FileWriter sclWriter;


    public SclStar(Alphabet<String> alphabet,
                  MembershipOracle<String, Word<Word<String>>> mqOracle,
                  EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
                  EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> partialEqOracle,
                   List<Alphabet<String>> inputComponentsActs){

        this.eqOracle = eqOracle;
        this.partialEqOracle = partialEqOracle;
        this.alphabet = alphabet;
        this.mqOracle = mqOracle;
        this.logger = logger;

        this.round_counter = new Counter("Decomposed Learning  rounds", "#");
        this.eq_counter = new Counter("Total number of equivalence queries", "#");

        this.inputComponentsActs = inputComponentsActs;
    }

    public CompactMealy<String, Word<String>> run(CompactMealy<String, Word<String>> mealyss, StatisticSUL<String, Word<String>> eq_sym_counter,
                                                  EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> testEqOracle,
                                                  int runCounter, String sclFileName){

    //Initialize starts:
        List<Alphabet<String>> initialSimaF = new ArrayList<>();
        for (String action : this.alphabet) {
            System.out.println(action);
            Alphabet<String> sigmai = new ListAlphabet<String>(Arrays.asList(action));
            initialSimaF.add(sigmai);
        }
        sigmaFamily = initialSimaF;
        round_counter.increment();
        List<CompactMealy<String, Word<String>>> learnedParts = new ArrayList<>();
        ProductMealy productMealy = null;
    //Initialize ends!

    //Sync starts:
        ArrayList<String> sync = new ArrayList<>();
        for (Alphabet<String> sigmai : initialSimaF){
            sync.addAll(sigmai);
        }
    //Sync ends!

    //LearnInParts starts:
        for(Alphabet<String> sigmai : sigmaFamily ){
            ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
            builder.setAlphabet(sigmai);
            builder.setOracle(mqOracle);
            ExtensibleLStarMealy<String, Word<String>> learner = builder.create();
            // The experiment will execute the main loop of active learning
            Experiment.MealyExperiment<String, Word<String>> experiment = new Experiment.MealyExperiment<String, Word<String>>(
                    learner, partialEqOracle, sigmai);
            Long pre_eq_sym = Long.parseLong(Utils.ExtractValue(eq_sym_counter.getStatisticalData().getSummary()));
            experiment.run();

            // get learned model
            CompactMealy<String, Word<String>> partialH = (CompactMealy<String, Word<String>>) experiment.getFinalHypothesis();

            eq_counter.increment(experiment.getRounds().getCount());
            Long post_eq_sym = Long.parseLong(Utils.ExtractValue(eq_sym_counter.getStatisticalData().getSummary()));

            learnedParts.add(partialH);
            if (productMealy== null){
                productMealy = new ProductMealy(partialH);
            }
            else productMealy.mergeFSMs(partialH, 1);
        }

        assert productMealy != null;
        CompactMealy<String, Word<String>> hypothesis = productMealy.getMachine();
    //LearnInParts ends!

    //UpdateSync starts:
//        Map<String,Word<String>> outSync = new HashMap<String, Word<String>>();
//        Collection<Integer> states = hypothesis.getStates();
//        Word<String> output_1 = null;
//        Word<String> output_2 = null;
//        ArrayList<String> syncToRemove = new ArrayList<>();
//        for (String m : sync){
//            System.out.println("For sync = " + m);
//            boolean isSync = true;
//            for(Integer si : states){
//                for(Integer sj : states){
//                    output_1 = hypothesis.getTransition(si, m).getOutput();
//                    output_2 = hypothesis.getTransition(sj, m).getOutput();
//                    if(!output_1.equals(output_2)){
//                        syncToRemove.add(m);
//                        isSync = false;
//                        break;
//                    }
//                }
//                if(!isSync){
//                    break;
//                }
//            }
//            if(isSync) {
//                boolean hasIt = false;
//                for (Map.Entry<String, Word<String>> current_map : outSync.entrySet()) {
//                    if (current_map.getKey().equals(m) && current_map.getValue().equals(output_1)) {
//                        hasIt = true;
//                        break;
//                    }
//                }
//                if (!hasIt) {
//                    outSync.put(m, output_1);
//                }
//            }
//        }
//        System.out.println("ToREmOVE : " + syncToRemove + "\n");
//        for(String toRemove : syncToRemove){
//            sync.remove(toRemove);
//        }
    //UpdateSync ends!

        @Nullable DefaultQuery<String, Word<Word<String>>> ce;
        @Nullable DefaultQuery<String, Word<Word<String>>> ce2;

        Long pre_eq_sym = Long.parseLong(Utils.ExtractValue(eq_sym_counter.getStatisticalData().getSummary()));
        Long post_eq_sym;

    //Equivalence-Query starts:
        ce = eqOracle.findCounterExample(hypothesis,alphabet);
        eq_counter.increment();
    //Equivalence-Query ends!
    //MainLoop starts:
        try {
            FileWriter myWriter = new FileWriter("Log/Counter Example/Run for the " + runCounter + "st time.txt");
            int counter = 1;
            while (ce != null) {
                try {
                    FileWriter componentsWriter = new FileWriter( "Log/Learning.txt", true);
                    componentsWriter.write(String.valueOf(counter) + "st in whileLoop\n");
                    componentsWriter.close();
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                System.out.println("***NEW TRY IN WHILE LOOP***\n");
                myWriter.write("Calling CE-Distilation for the " + counter + "st time:\n");
                Word<String> minimalCe = ceDistillation(ce.getInput(), sigmaFamily, hypothesis, sync, myWriter);
                counter++;
                round_counter.increment();
                post_eq_sym = Long.parseLong(Utils.ExtractValue(eq_sym_counter.getStatisticalData().getSummary()));

                //ProcessCE starts:
                //Building outCe:
//                List<String> ceList = minimalCe.asList();
//                int state = 0;
//                int nextState;
//                CompactMealyTransition<Word<String>> transition = null;
//                List<Word<String>> outCe = new ArrayList<>();
//                Word<String> output;
//                for (String currenntAlpha : ceList) {
//                    transition = mealyss.getTransition(state, currenntAlpha);
//                    output = transition.getOutput();
//                    nextState = transition.getSuccId();
//                    outCe.add(output);
//                    state = nextState;
//                }
//                System.out.println("MinimalCE: " + minimalCe);
//                System.out.println("Output of minimalCE : " + outCe + "\n");

                List<Alphabet<String>> Id = new ArrayList<>();
                Alphabet<String> ICe = makeSet(minimalCe);



                //Implementing the for loop:
                ArrayList<String> toRemoveSync = new ArrayList<>();
                System.out.println("THE SYNC : " + sync);
                System.out.println("OUTSYNC : " + outSync + "\n");
                for (String syncAlpha : sync) {
                    boolean isInCe = false;
                    for (String ceSync : ceList) {
                        if (ceSync.equals(syncAlpha)) {
                            isInCe = true;
                            break;
                        }
                    }
                    if (isInCe) {
                        for (Map.Entry<String, Word<String>> current_map : outSync.entrySet()) {
                            for (int i = 0; i < ceList.size(); i++) {
                                if (!ceList.get(i).equals(syncAlpha)) {
                                    continue;
                                }
                                if (current_map.getKey().equals(syncAlpha) && !current_map.getValue().equals(outCe.get(i))) {
                                    System.out.println("Removing SYNC : " + syncAlpha + "  because a different output in the ce");
                                    System.out.println("OUTPUT for SYNC : " + current_map.getValue());
                                    System.out.println("OUTPUT for Ce : " + outCe.get(i) + "\n");
                                    toRemoveSync.add(syncAlpha);
                                    List<Alphabet<String>> iStar = this.findSetsIncluding(sigmaFamily, syncAlpha);

                                    ArrayList<String> mergedSet = new ArrayList<>();
                                    ArrayList<CompactMealy<String, Word<String>>> trashParts = new ArrayList<>();
                                    for (Alphabet<String> sigmai : iStar) {
                                        sigmaFamily.remove(sigmai);
                                        ArrayList<String> cleaned = this.cleanSet(mergedSet, sigmai);
                                        mergedSet.addAll(cleaned);
                                    }
                                    Alphabet<String> mergedAlphabet = Alphabets.fromList(mergedSet);
                                    sigmaFamily.add(mergedAlphabet);
                                    break;
                                }
                            }
                        }
                    }
                }
                for (String toRemove : toRemoveSync) {
                    sync.remove(toRemove);
                    outSync.remove(toRemove);
                }
                System.out.println("syn: c" + sync + "\n");
                List<Alphabet<String>> iD = dependSets(minimalCe, sigmaFamily, sync);
                System.out.println("iD without syncs: " + iD);
                ArrayList<String> mergedSet = new ArrayList<>();
                ArrayList<CompactMealy<String, Word<String>>> trashParts = new ArrayList<>();
                for (Alphabet<String> sigmai : iD) {
                    if (exist(sigmai)) {
                        sigmaFamily.remove(sigmai);
                    }

                    ArrayList<String> cleaned = this.cleanSet(mergedSet, sigmai);
                    mergedSet.addAll(cleaned);
                }
                for (String syncAlpha : sync) {
                    for (String ceAlpha : ceList) {
                        if (ceAlpha.equals(syncAlpha)) {
                            Alphabet<String> sigmai = new ListAlphabet<String>(Arrays.asList(syncAlpha));
                            ArrayList<String> cleaned = this.cleanSet(mergedSet, sigmai);
                            if (isNew(mergedSet, ceAlpha) && exist(sigmai)) {
                                sigmaFamily.remove(sigmai);
                            }
                            mergedSet.addAll(cleaned);
                            break;
                        }
                    }
                }
                System.out.println("iD with sync:(merged) " + mergedSet + "\n");
                Alphabet<String> mergedAlphabet = Alphabets.fromList(mergedSet);
                sigmaFamily.add(mergedAlphabet);
                System.out.println("sigmaFamily after merging: " + sigmaFamily + "\n");
                //ProcessCE ends!

                //LearnInParts starts(Learn the single merged):
                productMealy = null;
                learnedParts.clear();
                for (Alphabet<String> sigmai : sigmaFamily) {
                    pre_eq_sym = Long.parseLong(Utils.ExtractValue(eq_sym_counter.getStatisticalData().getSummary()));
                    ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
                    builder.setAlphabet(sigmai);
                    builder.setOracle(mqOracle);
                    ExtensibleLStarMealy<String, Word<String>> learner = builder.create();
                    // The experiment will execute the main loop of active learning
                    Experiment.MealyExperiment<String, Word<String>> experiment =
                            new Experiment.MealyExperiment<String, Word<String>>(learner, partialEqOracle, sigmai);
                    try {
                        FileWriter componentsWriter = new FileWriter( "Log/Learning.txt", true);
                        componentsWriter.write("\tLearn the component " + sigmai.toString() + " using L-Star\n");
                        componentsWriter.write("WhileCounter: "+ counter + "\n");

                        if(sigmai.toString().equals("[h]") && counter == 2){
                            componentsWriter.write("WhileCounter: "+ counter + "\n");
                            componentsWriter.write("WhileCounter: "+ counter + "\n");
                        }
                        componentsWriter.close();
                    } catch (IOException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }

                    experiment.run();

                    // get learned model
                    CompactMealy<String, Word<String>> partialH = (CompactMealy<String, Word<String>>) experiment.getFinalHypothesis();

                    eq_counter.increment(experiment.getRounds().getCount());
                    post_eq_sym = Long.parseLong(Utils.ExtractValue(eq_sym_counter.getStatisticalData().getSummary()));

                    learnedParts.add(partialH);
                    if (productMealy == null) {
                        productMealy = new ProductMealy(partialH);
                    } else productMealy.mergeFSMs(partialH, 1);
                }

                hypothesis = productMealy.getMachine();
                //LearnInParts ends(Learn the single merged)!

                //Equivalence-Query starts:
                ce = eqOracle.findCounterExample(hypothesis, alphabet);
                eq_counter.increment();
                //Equivalence-Query ends!

                if (ce == null && testEqOracle != null) {
                    for (CompactMealy<String, Word<String>> comp : learnedParts) {
                        ce2 = testEqOracle.findCounterExample(comp, comp.getInputAlphabet());
                        if (ce2 != null) {
                            return null;
                        }
                    }
                }
                pre_eq_sym = Long.parseLong(Utils.ExtractValue(eq_sym_counter.getStatisticalData().getSummary()));

            }


    //MainLoop ends!

        CompactMealy final_H = productMealy.getMachine();
        String result = "";

        result += "\tThe input components:\n";
        for(Alphabet act: inputComponentsActs){
            result += "\t\t  - components with " + act.size() + " inputs: " + act + "\n";
        }

        result += "\n\n\tThe result:\n";
        for (Alphabet s: sigmaFamily){
            result += "\t\t  - component with " + s.size() + " inputs: " + s + "\n";
        }
        result += "Sync Actions: " + sync;
//        if (inputComponentsActs.size() != sigmaFamily.size())
//            throw new RuntimeException();

        sclFileName += ".txt";
        sclWriter =  new FileWriter(sclFileName);
        sclWriter.write(result + "\n\n");
        myWriter.write(result);
        myWriter.close();
        return final_H;
        }

        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }
    }

    private boolean exist(Alphabet<String> sigmai){
        for(Alphabet<String> sigmaj : sigmaFamily){
            if(sigmai.equals(sigmaj)){
                return(true);
            }
        }
        return (false);
    }

    private Word<String> ceDistillation(Word<String> ce, List<Alphabet<String>> sigmaFamily, CompactMealy hypothesis, ArrayList<String> sync, FileWriter myWriter){
        try {
            myWriter.write("\tCE before minimizing:\n\t\t" + ce + "\n");

            ce = this.cut_ce(ce, hypothesis);
            myWriter.write("\tCE after cut_ce:\n\t\t" + ce + "\n");

            List<Alphabet<String>>  iD = dependSets(ce, sigmaFamily, sync);
            for(String synci : sync){
                for(String cii : ce){
                    if(synci.equals(cii)){
                        boolean isNew = true;
                        Alphabet<String> synciString = new ListAlphabet<String>(Arrays.asList(synci));
                        for(Alphabet<String> iDi : iD){
                            if(synciString.equals(iDi)){
                                isNew = false;
                            }
                        }
                        if(isNew){
                            iD.add(synciString);
                        }
                        break;
                    }
                }
            }

            List<ArrayList<Alphabet<String>>> subsets = new ArrayList();
            for (int k = 2; k < iD.size(); k++) {
                subsets = k_combinations(k, iD);
                System.out.println("Subsets before sort:\n\t" + subsets + "\n");
                subsets = sortSubsets(subsets);
                System.out.println("Subsets after sort:\n\t" + subsets + "\n");
                for (ArrayList list : subsets) {
                    Alphabet<String> merged_list = merge_parts(list);
                    Word<String> ce_prime = projection(ce, merged_list);
                    if (check_for_ce(ce_prime, hypothesis)) {
                        myWriter.write("\tCE after minimizing:\n\t\t" + ce_prime + "\n\n");
                        return ce_prime;
                    }
                }
            }
            myWriter.write("\tCE after minimizing:\n\t\t" + ce + "\n\n");
            return ce;
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return ce;
    }

    private List<ArrayList<Alphabet<String>>> sortSubsets(List<ArrayList<Alphabet<String>>> subsets){
        List<ArrayList<Alphabet<String>>> sortedSubsets = new ArrayList<>();
        List<Integer> subsetsSizes = new ArrayList<Integer>();
        for(int i = 0; i < subsets.size(); i++){
            int len = 0;
            for(int j = 0; j < subsets.get(i).size(); j++){
                len += subsets.get(i).get(j).size();
            }
            subsetsSizes.add(len);

        }

        for(int i = 0; i < subsets.size(); i++){
            int minValue = Integer.MAX_VALUE;
            int minIndex = 0;
            for(int j = 0; j < subsets.size(); j++){
                if(subsetsSizes.get(j) < minValue){
                    minValue = subsetsSizes.get(j);
                    minIndex = j;
                }
            }
            subsetsSizes.set(minIndex, Integer.MAX_VALUE);
            sortedSubsets.add(subsets.get(minIndex));
        }
        return(sortedSubsets);
    }

    private List<Alphabet<String>> involved_sets(Word<String> ce, List<Alphabet<String>> sigmaFamily){
        List<Alphabet<String>> dependentSets = new ArrayList<>();

        List<String> ceList = ce.asList();
        for (Alphabet<String> sigmai: sigmaFamily){
            for (String action : sigmai){
                if (ceList.contains(action)){
                    dependentSets.add(sigmai);
                    break;
                }
            }
        }
        return dependentSets;
    }

    private boolean isNew(ArrayList<String> mergedSet, String alpha){
        for(String alphaj : mergedSet){
            if(alpha.equals(alphaj)){
                return(false);
            }
        }
        return(true);
    }

    private ArrayList<String> cleanSet(ArrayList<String> mergedSet, Alphabet<String> sigma) {
        ArrayList<String> cleanedSet = new ArrayList<>();
        for(String alphai : sigma){
            boolean isClean = true;
            if(isNew(mergedSet, alphai)){
                cleanedSet.add(alphai);
            }
        }
        return(cleanedSet);
    }

    private List<Alphabet<String>> findSetsIncluding(List<Alphabet<String>> sigmaFamily, String currentAlpha){
        List<Alphabet<String>> SetsIncluding = new ArrayList<>();
        for (Alphabet<String> sigmai: sigmaFamily){
            for (String action : sigmai){
                if (action.equals(currentAlpha)){
                    SetsIncluding.add(sigmai);
                    break;
                }
            }
        }
        return SetsIncluding;
    }

    private List<Alphabet<String>> dependSets(Word<String> ce, List<Alphabet<String>> sigmaFamily, ArrayList<String> sync){
        List<Alphabet<String>> dependSetsList = new ArrayList<>();
        for (Alphabet<String> sigmai : sigmaFamily) {
            for(String alphai :sigmai){
                boolean isDepend = false;
                for(String ceAlpha : ce){
                    if(alphai.equals(ceAlpha)){
                        isDepend = true;
                        for(String syncAlpha : sync){
                            if(syncAlpha.equals(alphai)){
                                isDepend = false;
                                break;
                            }
                        }
                        break;
                    }
                }
                if(isDepend){
                    dependSetsList.add(sigmai);
                    break;
                }
            }
        }
        return(dependSetsList);
    }

    private Word<String> cut_ce(Word<String> ce, CompactMealy hypothesis){
        for(Word<String> prefix: ce.prefixes(false) ){
            if (check_for_ce(prefix, hypothesis)){
                return prefix;
            }
        }
        return ce;
    }

    private Word<String> projection(Word<String> word, Alphabet<String> alphabet){
        ArrayList<String> input = new ArrayList<>();
        for (String action: word ){
            if (alphabet.contains(action)){
                input.add(action);
            }
        }
        return Word.fromList(input);
    }

    private Boolean check_for_ce(Word<String> ce, CompactMealy hypothesis){
        Word<Word<String>> sul_answer = this.mqOracle.answerQuery(ce);
        Word<Word<String>> hypothesis_answer = hypothesis.computeStateOutput(hypothesis.getInitialState(), ce);
        if (!sul_answer.equals(hypothesis_answer)){
            return true;
        }
        return false;
    }

    private Alphabet<String> merge_parts(List<Alphabet<String>> list){
        ArrayList<String> mergedSet = new ArrayList<>();
        for (Alphabet<String> sigmai : list){
            mergedSet.addAll(sigmai);
        }
        return Alphabets.fromList(mergedSet);
    }

    private List<ArrayList<Alphabet<String>>> k_combinations(int k, List<Alphabet<String>> input){
        List<ArrayList<Alphabet<String>>> subsets = new ArrayList<>();

        int[] s = new int[k];                  // here we'll keep indices
        // pointing to elements in input array

        if (k <= input.size()) {
            // first index sequence: 0, 1, 2, ...
            for (int i = 0; (s[i] = i) < k - 1; i++);
            subsets.add(getSubset(input, s));
            for(;;) {
                int i;
                // find position of item that can be incremented
                for (i = k - 1; i >= 0 && s[i] == input.size() - k + i; i--);
                if (i < 0) {
                    break;
                }
                s[i]++;                    // increment this item
                for (++i; i < k; i++) {    // fill up remaining items
                    s[i] = s[i - 1] + 1;
                }
                subsets.add(getSubset(input, s));
            }
        }
        return subsets;
    }

    private ArrayList<Alphabet<String>> getSubset(List<Alphabet<String>> input, int[] subset) {
        ArrayList<Alphabet<String>> result = new ArrayList();
        for (int i = 0; i < subset.length; i++)
            result.add(input.get(subset[i])) ;
        return result;
    }

    private Alphabet<String> makeSet(Word<String> ce) {
        ArrayList<String> theSet = new ArrayList<>();
        for(String action : ce) {
            if(!theSet.contains(action))
                theSet.add(action);
        }
        return Alphabets.fromList(theSet);
    }

    public Alphabet<String> getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(Alphabet<String> alphabet) {
        this.alphabet = alphabet;
    }

    public void setEqOracle(EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle) {
        this.eqOracle = eqOracle;
    }

    public EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> getEqOracle() {
        return eqOracle;
    }

    public void setPartialEqOracle(EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle) {
        this.partialEqOracle = eqOracle;
    }

    public EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> getPartialEqOracle() {
        return partialEqOracle;
    }

    public MembershipOracle<String, Word<Word<String>>> getMqOracle() {
        return mqOracle;
    }

    public void setMqOracle(MembershipOracle<String, Word<Word<String>>> mqOracle) {
        this.mqOracle = mqOracle;
    }

    public List<Alphabet<String>> getSigmaFamily() {
        return sigmaFamily;
    }
    public FileWriter getSclWriter(){
        return sclWriter;
    }
    public void closeSclWriter() throws IOException{
        sclWriter.close();
    }
    public void setSigmaFamily(List<Alphabet<String>> sigmaFamily) {
        this.sigmaFamily = sigmaFamily;
    }

    private Counter mq_counter;

    public Counter getMq_counter() {
        return mq_counter;
    }

    public void setMq_counter(Counter mq_counter) {
        this.mq_counter = mq_counter;
    }

    public Counter getRound_counter() {
        return round_counter;
    }

    public void setRound_counter(Counter round_counter) {
        this.round_counter = round_counter;
    }

    public Counter getEq_counter() {
        return eq_counter;
    }

    public void setEq_counter(Counter eq_counter) {
        this.eq_counter = eq_counter;
    }

}
