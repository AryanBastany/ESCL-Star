package main;

import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.SUL;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.api.statistic.StatisticSUL;
import de.learnlib.driver.util.MealySimulatorSUL;
import de.learnlib.filter.cache.sul.SULCache;
import de.learnlib.filter.statistic.oracle.CounterOracle;
import de.learnlib.filter.statistic.sul.ResetCounterSUL;
import de.learnlib.filter.statistic.sul.SymbolCounterSUL;
import de.learnlib.oracle.equivalence.RandomWMethodEQOracle;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.equivalence.WMethodEQOracle;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.Experiment;
import de.learnlib.util.statistics.SimpleProfiler;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.Pair;
import net.automatalib.incremental.ConflictException;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.visualization.VisualizationHelper;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Logger;

public class FsmLearner {
    public static String FILE_NAME = "FILE_NAME";
    public static String STATES = "STATES";
    public static String INPUTS = "INPUTS";
    public static String LSTAR = "LSTAR";
    public static String LIP = "LIP";
    public static String SCLSTAR = "SCLSTAR";
    public static String MQ_SYM = "_MQ_SYM";
    public static String MQ_RST = "_MQ_RST";
    public static String EQ_SYM = "_EQ_SYM";
    public static String EQ_RST = "_EQ_RST";
    public static String TOTAL_SYM = "_TOTAL_SYM";
    public static String TOTAL_RST = "_TOTAL_RST";
    public static String EQs = "_EQs";
    public static String COMPONENTS = "_COMPONENTS" ;
    public static String ROUNDS = "_ROUNDS";
    public static String MEM_SHIP_COUNT = "_MQs";
    public static String EQ_QUERY_COUNT = "_EQ_QUERY_COUNT";
    public static String CACHE = "CACHE";
    public static String[] data;

    public static String [] benchmarks;
    private static Boolean CACHE_ENABLE = true;

    private static String RESULTS_PATH;
    //    private static Logger logger;
    private static CSVProperties csvProperties;
    private static Experimentproperties experimentProperties ;
    private static String benchmarks_base_dir;
    private static Logger logger;

    public static final String SRC_DIR = "src_dir";
    public static final String EQUIVALENCE_METHOD = "eq";
    public static final String EXPERIMENT_REPEAT = "repeat";

    private String filePath = "";
    private static String equivalenceMethod;
    private int repeat;
    private int minNumOfStates;
    private int maxNumOfStates;
    private static boolean finalCheckMode;
    private boolean initializeResults;
    private int numberOfTests;

    public FsmLearner(ArrayList<String> args) throws IOException{
        if(args.get(3).equals("Real"))
            filePath = "src/test/Real Tests/data/Reals.txt";
        else if(args.get(3).equals("P2P"))
            filePath += "src/test/Generated Tests/data/P2P-All-Tests.txt";
        else if(args.get(3).equals("Mesh"))
            filePath += "src/test/Generated Tests/data/Mesh-All-Tests.txt";
        else if(args.get(3).equals("Star"))
            filePath += "src/test/Generated Tests/data/Star-All-Tests.txt";
        else if(args.get(3).equals("Ring"))
            filePath += "src/test/Generated Tests/data/Ring-All-Tests.txt";
        else if(args.get(3).equals("Bus"))
            filePath += "src/test/Generated Tests/data/Bus-All-Tests.txt";
        else if(args.get(3).equals("Bipartite"))
            filePath += "src/test/Generated Tests/data/Bipartite-All-Tests.txt";
        else
            filePath += "src/test/Manual Tests/data/Manuals.txt";

        equivalenceMethod = args.get(0);
        repeat = Integer.parseInt(args.get(2));
        minNumOfStates = Integer.parseInt(args.get(args.size() - 4));
        maxNumOfStates = Integer.parseInt(args.get(args.size() - 3));
        finalCheckMode = Boolean.parseBoolean(args.get(1));
        numberOfTests = Integer.parseInt(args.get(args.size() - 2));
        initializeResults = Boolean.parseBoolean(args.getLast());

        RESULTS_PATH = "Results/Parameters/";
        if(args.get(3).equals("Real"))
            RESULTS_PATH += "Real Tests/";
        else
            RESULTS_PATH += "Generated Tests/";

        if(args.get(3).equals("P2P"))
            RESULTS_PATH += "Point-To-Point/";
        if(args.get(3).equals("Mesh"))
            RESULTS_PATH += "Mesh/";
        if(args.get(3).equals("Star"))
            RESULTS_PATH += "Star/";
        if(args.get(3).equals("Ring"))
            RESULTS_PATH += "Ring/";
        if(args.get(3).equals("Bus"))
            RESULTS_PATH += "Bus/";
        if(args.get(3).equals("Bipartite"))
            RESULTS_PATH += "Bipartite/";

        RESULTS_PATH += "Results.csv";
    }


    public void learn() throws IOException {
        csvProperties = CSVProperties.getInstance();
        experimentProperties = Experimentproperties.getInstance();
        try {
            // create the command line parser
            CommandLineParser parser = new DefaultParser();

            // create the Options
            Options options = createOptions();

            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();

//        initial the experiment properties
            benchmarks_base_dir = experimentProperties.getProp("benchmarks_base_dir");
            File f = new File(filePath);
            BufferedReader br = new BufferedReader(new FileReader(f));

            // initial a results file
            if(initializeResults)
                Utils.writeDataHeader(RESULTS_PATH, csvProperties.getResults_header());

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime now = LocalDateTime.now();

            int dataLen = csvProperties.getIndex("DATA_LEN");

            String c;
            ProductMealy productMealy = null;

            File inputFolder = new File("Log/FSMs/INPUTs");
            Utils.clearFolder(inputFolder);
            new File("Log/FSMs/INPUTs/Components Log").mkdirs();

            int inputCounter = 0;

            File sclFolder = new File("Log/FSMs/SCL-Star");
            Utils.clearFolder(sclFolder);
            new File("Log/FSMs/SCL-Star").mkdirs();

            File lFolder = new File("Log/FSMs/L-Star");
            Utils.clearFolder(lFolder);

            int componentsCount = 0;
                int testsCounter = 1;
                while (br.ready() && testsCounter <= numberOfTests) {
                    c = br.readLine();
                    File f2 = new File(c);
                    BufferedReader br2 = new BufferedReader(new FileReader(f2));
                    data = new String[dataLen];
                    data[csvProperties.getIndex(FILE_NAME)] = c;
                    productMealy = null;
                    int size = 0;
                    componentsCount = 0;
                    List<Alphabet<String>> inputComponentsActs = new ArrayList<>();
                    System.out.println("\n\nMerging the components");
                    while (br2.ready()) {
                        componentsCount++;
                        String c2 = br2.readLine();
                        CompactMealy<String, Word<String>> currentTarget;
                        File file = new File(c2);
                        try {
                            currentTarget = Utils.getInstance().loadMealyMachineFromDot(file);
                            inputComponentsActs.add(currentTarget.getInputAlphabet());
                        } catch (Exception e) {
                            System.out.println(file);
                            System.out.println("problem in loading file");
                            System.out.println(e.toString());
                            System.out.println(c2);
                            continue;
                        }
                        if (productMealy == null) {
                            productMealy = new ProductMealy(currentTarget);
                        } else productMealy.mergeFSMs(currentTarget, componentsCount);
                        size = productMealy.getMachine().getStates().size();
                        if(size > maxNumOfStates)
                            break;

                    }
                    if(size < minNumOfStates) {
                        System.out.println("This one is too small (" + size + " States)");
                        br2.close();
                        continue;
                    }
                    if(size > maxNumOfStates) {
                        System.out.println("This one is too big (" + size + " States)");
                        System.out.println( componentsCount + " Cmpnss");
                        br2.close();
                        continue;
                    }
                    System.out.println(" (" + size + " States)");
                    testsCounter++;
                    assert productMealy != null;
                    CompactMealy<String, Word<String>> target = productMealy.getMachine();

                    inputCounter++;
                    new File("Log/FSMs/SCL-Star/For input" + inputCounter).mkdirs();
                    new File("Log/FSMs/L-Star/For input" + inputCounter).mkdirs();

                    try {
                        FileWriter inputWriter = new FileWriter("Log/FSMs/INPUTs/input" + inputCounter + "txt");
                        Utils.printMachine(target, false, inputWriter);
                        inputWriter.close();

                        FileWriter componentsWriter = new FileWriter( "Log/FSMs/INPUTs/Components Log/" + inputCounter + ".txt");
                        componentsWriter.write(c);
                        componentsWriter.close();
                    } catch (IOException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }


                    data[csvProperties.getIndex(STATES)] = Integer.toString(target.size());
                    data[csvProperties.getIndex(INPUTS)] = Integer.toString(target.numInputs());
                    Alphabet<String> alphabet = target.getInputAlphabet();
                    try {
                        for (int currRep = 0; currRep < repeat; currRep++) {
                            //   Shuffle the alphabet
                            String[] alphArr = alphabet.toArray(new String[alphabet.size()]);
                            Collections.shuffle(Arrays.asList(alphArr));
                            alphabet = Alphabets.fromArray(alphArr);
                            data[csvProperties.getIndex(CACHE)] = CACHE_ENABLE.toString();

                            //             RUN L*
                            learnProductMealy(target, alphabet, equivalenceMethod, finalCheckMode, inputCounter, currRep + 1);

                            //             RUN SCL*
                            @Nullable AtomicReference<CompactMealy> result = new AtomicReference<>(null);
                            data[csvProperties.getIndex(LIP + COMPONENTS)] = String.valueOf(componentsCount);

                            long timeout = 3000 * 1000; // 3000 min

                            // Thread for the called method
                            Alphabet<String> finalAlphabet = alphabet;
                            int finalRep = currRep;
                            int finalInputCounter = inputCounter;
                            Thread methodThread = new Thread(() -> {
                                CompactMealy learnedResult = learnMealyInParts(target, finalAlphabet, equivalenceMethod, "rndWords", finalCheckMode, finalRep + 1, finalInputCounter, inputComponentsActs);
                                result.set(learnedResult);
                            });

                            // Start the method thread
                            methodThread.start();

                            // Monitor the thread's execution time
                            long startTime = System.currentTimeMillis();
                            while (methodThread.isAlive()) {
                                long elapsedTime = System.currentTimeMillis() - startTime;
                                if (elapsedTime > timeout) {
                                    methodThread.interrupt(); // Interrupt the thread
                                    System.out.println("the learning took more than 3000 minutes. So it will be skipped");
                                    throw new TimeoutException("Operation timed out after the specified duration.");
                                }
                                Thread.sleep(100); // Check every 100ms
                            }

                            // Wait for the method thread to finish (if it hasn't already)
                            methodThread.join();

                            if (result.get() == null) {
                                System.out.println("the  SUL is not learned completely (SCL-Star)");
                            } else {
                                Utils.writeDataLineByLine(RESULTS_PATH, data);
                            }
                        }
                    }
                    catch (OutOfMemoryError | ConflictException | ArrayIndexOutOfBoundsException | TimeoutException e){
                        testsCounter--;
                        inputCounter--;
                    }
                    br2.close();
                }
                br.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getStackTrace()[0].getLineNumber());
        }
    }

    public static CompactMealy learnMealyInParts(CompactMealy mealyss, Alphabet<String> alphabet, String eq_method, String partial_eq_method, boolean test_mode, int rep, int inCounter, List<Alphabet<String>> inputComponentsActs){

        Utils.getInstance();
        // SUL simulator
        SUL<String, Word<String>> sulSim = new MealySimulatorSUL<>(mealyss, Utils.OMEGA_SYMBOL);

        //////////////////////////////////
        // Setup objects related to MQs //
        //////////////////////////////////

        // Counters for MQs
        StatisticSUL<String, Word<String>> mq_sym = new SymbolCounterSUL<>("MQ", sulSim);
        StatisticSUL<String, Word<String>> mq_rst = new ResetCounterSUL<>("MQ", mq_sym);

        // SUL for counting queries wraps sul
        SUL<String, Word<String>> mq_sul = mq_rst;

        // use caching to avoid duplicate queries
        // SULs for associating the IncrementalMealyBuilder 'mq_builder' to MQs
        if(CACHE_ENABLE){
            mq_sul = SULCache.createDAGCache(alphabet, mq_rst);
        }

        MembershipOracle<String, Word<Word<String>>> mqOracle = new SULOracle<String, Word<String>>(mq_sul);
        CounterOracle membShipCounter = new CounterOracle(mqOracle, "MQ");

        //////////////////////////////////
        // Setup objects related to EQs //
        //////////////////////////////////

        // Counters for EQs
        StatisticSUL<String, Word<String>> eq_sym = new SymbolCounterSUL<>("EQ", sulSim);
        StatisticSUL<String, Word<String>> eq_rst = new ResetCounterSUL<>("EQ", eq_sym);

        // SUL for counting queries wraps sul
        SUL<String, Word<String>> eq_sul = eq_rst;

        // SULs for associating the IncrementalMealyBuilder 'builder' to EQs
        if (CACHE_ENABLE){
            eq_sul = SULCache.createDAGCache(alphabet, eq_rst);
        }

        EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle = null;
        EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> partialEqOracle = null;
        partialEqOracle = buildEqOracle(eq_sul, partial_eq_method);
        eqOracle = buildEqOracle(eq_sul, eq_method);

        EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> testEqOracle = null;
        testEqOracle = buildEqOracle(eq_sul, "wp");
        @Nullable CompactMealy sclResult;
        try {
            String sclFileName = "Log/FSMs/SCL-Star/For input" + inCounter + "/Run for the " + rep + "st time";
            SclStar sclStar = new SclStar(alphabet, membShipCounter, eqOracle, partialEqOracle, inputComponentsActs);
            if (!test_mode ){
                sclResult = sclStar.run(mealyss, eq_sym, null, rep, sclFileName);
            }
            else{
                sclResult = sclStar.run(mealyss, eq_sym, testEqOracle, rep, sclFileName);
            }

            Utils.printMachine(sclResult, true, sclStar.getSclWriter());
            sclStar.closeSclWriter();

            data[csvProperties.getIndex(LIP+ROUNDS)] = String.valueOf(sclStar.getRound_counter().getCount());
            data[csvProperties.getIndex(LIP+MQ_RST)] = Utils.ExtractValue(mq_rst.getStatisticalData().getSummary());
            data[csvProperties.getIndex(LIP+MQ_SYM)] = Utils.ExtractValue(mq_sym.getStatisticalData().getSummary());
            data[csvProperties.getIndex(LIP+EQ_RST)] = Utils.ExtractValue(eq_rst.getStatisticalData().getSummary());
            data[csvProperties.getIndex(LIP+EQ_SYM)] = Utils.ExtractValue(eq_sym.getStatisticalData().getSummary());
            data[csvProperties.getIndex(LIP+EQs)] = String.valueOf(sclStar.getEq_counter().getCount());
            data[csvProperties.getIndex(LIP+TOTAL_RST)] = String.valueOf(Long.parseLong(Utils.ExtractValue(mq_rst.getStatisticalData().getSummary()))+ Long.parseLong(Utils.ExtractValue(eq_rst.getStatisticalData().getSummary())));
            data[csvProperties.getIndex(LIP+TOTAL_SYM)] = String.valueOf(Long.parseLong(Utils.ExtractValue(mq_sym.getStatisticalData().getSummary()))+ Long.parseLong(Utils.ExtractValue(eq_sym.getStatisticalData().getSummary())));
            data[csvProperties.getIndex(LIP+MEM_SHIP_COUNT)] = String.valueOf(membShipCounter.getCount());
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return null;
        }

        return sclResult;
    }

    public static void learnProductMealy(CompactMealy mealyss, Alphabet<String> alphabet, String eq_method, boolean test_mode, int inCounter, int rep){

        Utils.getInstance();
        // SUL simulator
        SUL<String, Word<String>> sulSim = new MealySimulatorSUL<>(mealyss, Utils.OMEGA_SYMBOL);

        //////////////////////////////////
        // Setup objects related to MQs //
        //////////////////////////////////

        // Counters for MQs
        StatisticSUL<String, Word<String>> mq_sym = new SymbolCounterSUL<>("MQ", sulSim);
        StatisticSUL<String, Word<String>> mq_rst = new ResetCounterSUL<>("MQ", mq_sym);

        // SUL for counting queries wraps sul
        SUL<String, Word<String>> mq_sul = mq_rst;

        // use caching to avoid duplicate queries

        // SULs for associating the IncrementalMealyBuilder 'mq_builder' to MQs
        if (CACHE_ENABLE){
            mq_sul = SULCache.createDAGCache(alphabet, mq_rst);
        }

        MembershipOracle<String, Word<Word<String>>> mqOracle = new SULOracle<String, Word<String>>(mq_sul);
        CounterOracle membShipCounter = new CounterOracle(mqOracle, "MQ");


        //////////////////////////////////
        // Setup objects related to EQs //
        //////////////////////////////////

        // Counters for EQs
        StatisticSUL<String, Word<String>> eq_sym = new SymbolCounterSUL<>("EQ", sulSim);
        StatisticSUL<String, Word<String>> eq_rst = new ResetCounterSUL<>("EQ", eq_sym);

        // SUL for counting queries wraps sul
        SUL<String, Word<String>> eq_sul = eq_rst;

        // SULs for associating the IncrementalMealyBuilder 'cbuilder' to EQs
        if(CACHE_ENABLE){
            eq_sul = SULCache.createDAGCache(alphabet, eq_rst);
        }

        EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle = null;
        EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> testEqOracle = null;
        eqOracle = buildEqOracle(eq_sul, eq_method);
        testEqOracle = buildEqOracle(eq_sul, "wp");


        Experiment experiment = learningLStarM(alphabet, mealyss, membShipCounter, eqOracle);
        CompactMealy<String, Word<String>> h = (CompactMealy<String, Word<String>>) experiment.getFinalHypothesis();
        if (test_mode){
            @Nullable DefaultQuery<String, Word<Word<String>>> ce = testEqOracle.findCounterExample(h,alphabet);
            if (ce!=null){
                System.out.println();
                System.out.println("************  incomplete lstar learning  **********");
                System.out.println(data[csvProperties.getIndex(FILE_NAME)]);
                System.out.println();
            }
        }
        try {
            FileWriter lWriter = new FileWriter("Log/FSMs/L-Star/For input" + inCounter + "/Run for the " + rep + "st time.txt");
            Utils.printMachine(h, false, lWriter);
            lWriter.close();
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


//        // statistics array
        data[csvProperties.getIndex(LSTAR+MQ_RST)] = Utils.ExtractValue(mq_rst.getStatisticalData().getSummary());
        data[csvProperties.getIndex(LSTAR+MQ_SYM)] = Utils.ExtractValue(mq_sym.getStatisticalData().getSummary());
        data[csvProperties.getIndex(LSTAR+EQ_RST)] = Utils.ExtractValue(eq_rst.getStatisticalData().getSummary());
        data[csvProperties.getIndex(LSTAR+EQ_SYM)] = Utils.ExtractValue(eq_sym.getStatisticalData().getSummary());
        data[csvProperties.getIndex(LSTAR+EQs)] = String.valueOf(experiment.getRounds().getCount());
        data[csvProperties.getIndex(LSTAR+TOTAL_RST)] = String.valueOf(Long.parseLong(Utils.ExtractValue(mq_rst.getStatisticalData().getSummary()))+ Long.parseLong(Utils.ExtractValue(eq_rst.getStatisticalData().getSummary())));
        data[csvProperties.getIndex(LSTAR+TOTAL_SYM)] = String.valueOf(Long.parseLong(Utils.ExtractValue(mq_sym.getStatisticalData().getSummary()))+ Long.parseLong(Utils.ExtractValue(eq_sym.getStatisticalData().getSummary())));
        data[csvProperties.getIndex(LSTAR+MEM_SHIP_COUNT)] = String.valueOf(membShipCounter.getCount());


        // profiling
        SimpleProfiler.logResults();
    }

    private static Experiment<MealyMachine<?, String,?, Word<String>>> learningLStarM(Alphabet<String> alphabet,
                                                                                      CompactMealy<String, Word<String>> mealyss,
                                                                                      MembershipOracle<String, Word<Word<String>>> mqOracle,
                                                                                      EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle){

        ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
        builder.setAlphabet(alphabet);
        builder.setOracle(mqOracle);


        ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

        // The experiment will execute the main loop of active learning
        Experiment.MealyExperiment<String, Word<String>> experiment = new Experiment.MealyExperiment<String, Word<String>>(learner, eqOracle,
                alphabet);

        experiment.run();
        return experiment;
    }


    private static EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> buildEqOracle(
            SUL<String, Word<String>> eq_sul, String eq_method) {
        MembershipOracle<String, Word<Word<String>>> oracleForEQoracle = new SULOracle<>(eq_sul);

        EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle;
        double restartProbability;
        int maxSteps, maxTests, maxLength, minLength, maxDepth, minimalSize, rndLength, bound;
        long rnd_long;
        boolean resetStepCount;
        long tstamp = System.currentTimeMillis();
        Random rnd_seed = new Random(tstamp);

        LearnLibProperties learn_props = LearnLibProperties.getInstance();

        switch (eq_method) {
            case "rndWalk":
                // create RandomWalkEQOracle
                restartProbability = learn_props.getRndWalk_restartProbability();
                maxSteps = learn_props.getRndWalk_maxSteps();
                resetStepCount = learn_props.getRndWalk_resetStepsCount();

                eqOracle = new RandomWalkEQOracle<String, Word<String>>(eq_sul, // sul
                        restartProbability, // reset SUL w/ this probability before a step
                        maxSteps, // max steps (overall)
                        resetStepCount, // reset step count after counterexample
                        rnd_seed // make results reproducible
                );
                break;
            case "rndWords":
                // create RandomWordsEQOracle
                maxTests = learn_props.getRndWords_maxTests();
                maxLength = learn_props.getRndWords_maxLength();
                minLength = learn_props.getRndWords_minLength();
                rnd_long = rnd_seed.nextLong();
                rnd_seed.setSeed(rnd_long);

                eqOracle = new RandomWordsEQOracle<>(oracleForEQoracle, minLength, maxLength, maxTests, rnd_seed);
                break;

            case "rndWordsBig":
                // create RandomWordsEQOracle
                maxTests = 2000;
                maxLength = 200;
                minLength = learn_props.getRndWords_minLength();
                rnd_long = rnd_seed.nextLong();
                rnd_seed.setSeed(rnd_long);

                eqOracle = new RandomWordsEQOracle<>(oracleForEQoracle, minLength, maxLength, maxTests, rnd_seed);
                break;
            case "wp":
                maxDepth = learn_props.getW_maxDepth();
                eqOracle = new WpMethodEQOracle<>(oracleForEQoracle, maxDepth);
                break;
            case "w":
                maxDepth = learn_props.getW_maxDepth();
                eqOracle = new WMethodEQOracle<>(oracleForEQoracle, maxDepth);
                break;
            case "wrnd":
                minimalSize = learn_props.getWhyp_minLen();
                rndLength = learn_props.getWhyp_rndLen();
                bound = learn_props.getWhyp_bound();
                rnd_long = rnd_seed.nextLong();
                rnd_seed.setSeed(rnd_long);

                eqOracle = new RandomWMethodEQOracle<>(oracleForEQoracle, minimalSize, rndLength, bound, rnd_seed, 1);;
                break;
            default:
                maxDepth = 2;
                eqOracle = new WMethodEQOracle<>(oracleForEQoracle, maxDepth);
                break;
        }
        return eqOracle;
    }


    private static CompactMealy<String, Word<String>> LoadMealy(File fsm_file) {
        // TODO Auto-generated method stub
        InputModelDeserializer<String, CompactMealy<String, Word<String>>> parser_1 = DOTParsers
                .mealy(MEALY_EDGE_WORD_STR_PARSER);
        CompactMealy<String, Word<String>> mealy = null;
        String file_name = fsm_file.getName();
        if (file_name.endsWith("txt")) {
            try {
                mealy = Utils.getInstance().loadMealyMachine(fsm_file);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return mealy;
        } else if (file_name.endsWith("dot")) {
            try {
                mealy = parser_1.readModel(fsm_file).model;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return mealy;
        }

        return null;
    }

    public static final Function<Map<String, String>, Pair<@Nullable String, @Nullable Word<String>>> MEALY_EDGE_WORD_STR_PARSER = attr -> {
        final String label = attr.get(VisualizationHelper.EdgeAttrs.LABEL);
        if (label == null) {
            return Pair.of(null, null);
        }

        final String[] tokens = label.split("/");

        if (tokens.length != 2) {
            return Pair.of(null, null);
        }

        Word<String> token2 = Word.epsilon();
        token2 = token2.append(tokens[1]);
        return Pair.of(tokens[0], token2);
    };

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(SRC_DIR, true, "Input directory");
        options.addOption(EQUIVALENCE_METHOD, true, "Equivalence method, options: wp, w, wrnd, rndWords, rndWordsBig, rndWalk");
        options.addOption(EXPERIMENT_REPEAT, true, "Number of repeating the experiment");
        return options;
    }

}
