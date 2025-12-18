package main;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Experiment {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<String> infoPageInfo = new ArrayList<>();

        // Prompt the user for input
        System.out.println("Choose Equivalence Query (rndWords recommended): [wp, w, wrnd, rndWords, rndWordsBig, rndWalk]");
        infoPageInfo.add(scanner.nextLine());

        System.out.println("Enable Final Check Mode (disabled recommended): [true/false]");
        infoPageInfo.add(scanner.nextLine());

        System.out.println("Enter Number of Repetitions (3 recommended):");
        infoPageInfo.add(scanner.nextLine());

        System.out.println("Enter Test Type [Real, P2P, Ring, Star, Bus, Bipartite, Mesh]:");
        infoPageInfo.add(scanner.nextLine());

        System.out.println("Enter Minimum Number of States (100 recommended):");
        infoPageInfo.add(scanner.nextLine());

        System.out.println("Enter Maximum Number of States (30000 recommended):");
        infoPageInfo.add(scanner.nextLine());

        System.out.println("Enter Number of Tests for each component-number:");
        infoPageInfo.add(scanner.nextLine());

        System.out.println("Enter Minimum Number of Components (3 recommended):");
        int minNumOfComponents = Integer.parseInt(scanner.nextLine());
        infoPageInfo.add(String.valueOf(minNumOfComponents));

        System.out.println("Enter Maximum Number of Components (7 recommended for Bipartite and Mesh and 9 for others):");
        int maxNumOfComponents = Integer.parseInt(scanner.nextLine());
        infoPageInfo.add(String.valueOf(maxNumOfComponents));

        // Write states info
        writeStatesInfo(
                Integer.parseInt(infoPageInfo.get(4)),
                Integer.parseInt(infoPageInfo.get(5))
        );

        // Remove states info for learner and add final check state
        ArrayList<String> info = new ArrayList<>(infoPageInfo);
        info.removeLast();
        info.removeLast();
        info.add(String.valueOf(true));

        // Process for each number of components
        for (int numOfComponents = minNumOfComponents; numOfComponents <= maxNumOfComponents; numOfComponents++) {
            try {
                if (infoPageInfo.get(3).equalsIgnoreCase("Real")) {
                    FileWriter myWriter = new FileWriter("Configs/Real Tests.txt");
                    myWriter.write(numOfComponents + "\n");
                    myWriter.write(String.valueOf(Integer.parseInt(infoPageInfo.get(6)) * 1000));
                    myWriter.close();

                    System.out.println("Choosing the Real tests components...");
//                    runFile("python", "src/test/Real Tests/ChooseTests.py");
                } else {
                    FileWriter myWriter = new FileWriter("Configs/Generated Tests.txt");
                    myWriter.write(infoPageInfo.get(3) + '\n');
                    myWriter.write(numOfComponents + "\n");
                    myWriter.write(String.valueOf(Integer.parseInt(infoPageInfo.get(6)) * 1000));
                    myWriter.close();

                    System.out.println("Generating the tests...");
                    runFile("python", "src/test/Generated Tests/GenerateTests.py");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

            try {
                FsmLearner learner = new FsmLearner(info);
                learner.learn();
                info.set(info.size() - 1, String.valueOf(false));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                if (! infoPageInfo.get(3).equalsIgnoreCase("Real")) {
                    FileWriter myWriter = new FileWriter("Configs/Generated Tests.txt");
                    myWriter.write(infoPageInfo.get(3) + '\n');
                    myWriter.write(numOfComponents + "\n");
                    myWriter.write(String.valueOf(Integer.parseInt(infoPageInfo.get(6)) * 1000));
                    myWriter.close();

                    System.out.println("Deleting the tests...");
                    runFile("python", "src/test/Generated Tests/DeleteTests.py");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    private static void runFile(String type, String filePath) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(type, filePath);

        // Redirect error stream to avoid missing any issues
        builder.redirectErrorStream(true);

        Process process = builder.start();

        // Capture the output from the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // Ensure the process completes
        try {
            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void writeStatesInfo(int minNumOfStates, int maxNumOfStates) {
        try {
            FileWriter myWriter = new FileWriter("Configs/States.txt");
            myWriter.write(minNumOfStates + "\n");
            myWriter.write(String.valueOf(maxNumOfStates));
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
