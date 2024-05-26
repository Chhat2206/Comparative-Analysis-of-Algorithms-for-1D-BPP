import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays; // for Arrays.asList() and Arrays.stream()
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CuckooSearchGeneticAlgorithm {

    private List<int[]> nests;
    private List<Double> fitness;
    private int numNests;
    private int maxGenerations;
    private int[] bestNest;
    private double bestFitness;
    private Random random;
    private int binCapacity;
    private List<Integer> items;
    private double mutationRate = 0.15;

    public CuckooSearchGeneticAlgorithm(int binCapacity, List<Integer> items, int numNests, int maxGenerations) {
        this.binCapacity = binCapacity;
        this.items = items;
        this.numNests = numNests;
        this.maxGenerations = maxGenerations;
        this.fitness = new ArrayList<>();
        this.nests = new ArrayList<>();
        this.bestFitness = Double.MAX_VALUE;
        this.random = new Random();
        initializeNests();
    }

    private void initializeNests() {
        for (int i = 0; i < numNests; i++) {
            int[] newNest = generateNewSolution();
            nests.add(newNest);
            double fitnessValue = getFitness(newNest);
            fitness.add(fitnessValue);
            if (fitnessValue > bestFitness) { // Maximize fitness
                bestFitness = fitnessValue;
                bestNest = newNest.clone();  // Ensure this is correctly cloned
            }
        }
        // Ensure there is a default non-null bestNest even if no fitness update occurs
        if (bestNest == null && !nests.isEmpty()) {
            bestNest = nests.get(0).clone();
        }
    }

    private int[] generateNewSolution() {
        Integer[] newSolution = new Integer[items.size()];
        for (int i = 0; i < items.size(); i++) {
            newSolution[i] = items.get(i);
        }
        Collections.shuffle(Arrays.asList(newSolution)); // Correct shuffling of an Integer array
        return Arrays.stream(newSolution).mapToInt(Integer::intValue).toArray(); // Convert Integer[] back to int[]
    }


    private double getFitness(int[] solution) {
        int waste = calculateWaste(solution);
        return waste == 0 ? Double.MAX_VALUE : 1.0 / waste;
    }

    private int calculateWaste(int[] solution) {
        // Simulate the packing process and calculate the waste
        int totalWaste = 0;
        List<List<Integer>> bins = new ArrayList<>();
        for (int item : solution) {
            boolean placed = false;
            for (List<Integer> bin : bins) {
                int currentWeight = bin.stream().mapToInt(Integer::intValue).sum();
                if (currentWeight + item <= binCapacity) {
                    bin.add(item);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Integer> newBin = new ArrayList<>();
                newBin.add(item);
                bins.add(newBin);
                totalWaste += binCapacity - item;
            }
        }
        return totalWaste;
    }

    public void startSearch() {
        for (int gen = 0; gen < maxGenerations; gen++) {
            for (int i = 0; i < numNests; i++) {
                int[] newSol = performCrossoverAndMutation(nests.get(i));
                double newFit = getFitness(newSol);
                if (newFit > fitness.get(i)) {
                    nests.set(i, newSol);
                    fitness.set(i, newFit);
                    if (newFit > bestFitness) {
                        bestFitness = newFit;
                        bestNest = newSol.clone();  // Update bestNest
                    }
                }
            }
        }
    }

    private int[] performCrossoverAndMutation(int[] nest) {
        // Perform k-tournament selection to find another nest
        int k = 5;  // Tournament size
        List<int[]> tournament = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            tournament.add(nests.get(random.nextInt(nests.size())));
        }

        // Choose the best nest from the tournament
        int[] anotherNest = tournament.stream()
                .max(Comparator.comparingDouble(this::getFitness))
                .orElse(nest);

        // Perform two-point crossover
        int crossPoint = random.nextInt(nest.length);
        int[] child = new int[nest.length];
        System.arraycopy(nest, 0, child, 0, crossPoint);
        System.arraycopy(anotherNest, crossPoint, child, crossPoint, child.length - crossPoint);

        // Perform mutation
        if (random.nextDouble() < mutationRate) {
            int mutationPoint = random.nextInt(child.length);
            child[mutationPoint] = items.get(random.nextInt(items.size()));
        }
        return child;
    }

    public List<List<Integer>> packItems(int[] bestNest) {
        List<List<Integer>> bins = new ArrayList<>();
        for (int itemWeight : bestNest) {
            boolean placed = false;
            for (List<Integer> bin : bins) {
                int currentBinWeight = bin.stream().mapToInt(Integer::intValue).sum();
                if (currentBinWeight + itemWeight <= binCapacity) {
                    bin.add(itemWeight);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Integer> newBin = new ArrayList<>();
                newBin.add(itemWeight);
                bins.add(newBin);
            }
        }
        return bins;
    }

    public static void main(String[] args) {

        String filePath = "Cuckoo Search Genetic Algorithm/BPP.txt";
        List<Object[]> testCases = parseBPPFile(filePath);
        long startTime = System.currentTimeMillis();


        if (testCases.isEmpty()) {
            System.out.println("No test cases found in the file.");
            return;
        }



        for (Object[] testCase : testCases) {
            String datasetName = (String) testCase[0];  // Dataset name
            List<Integer> items = (List<Integer>) testCase[1];  // List of items for the bin packing problem

            System.out.println("Starting optimization for dataset: " + datasetName);

            // Initialize the Cuckoo Search Genetic Algorithm with specified parameters
            CuckooSearchGeneticAlgorithm csga = new CuckooSearchGeneticAlgorithm(10000, items, 30, 100);
            csga.startSearch();  // Start the search process

            // Check if a solution was found (bestNest should be non-null after startSearch if a solution was identified)
            if (csga.bestNest != null) {
                // Use the best nest found to pack items into bins
                List<List<Integer>> bestConfiguration = csga.packItems(csga.bestNest);
                System.out.println("Number of bins used: " + bestConfiguration.size());  // Print the number of bins used
                for (int j = 0; j < bestConfiguration.size(); j++) {
                    List<Integer> binItems = bestConfiguration.get(j);
                    int binWeight = binItems.stream().mapToInt(Integer::intValue).sum();
                    System.out.print("Bin " + (j + 1) + ": " + binItems);  // Print items in each bin
                    System.out.println(" - Bin weight: " + binWeight + " out of " + csga.binCapacity);  // Print the weight of each bin
                }
            } else {
                System.out.println("No optimal configuration found for dataset: " + datasetName);
            }

            System.out.println("Optimization completed for dataset: " + datasetName);
            System.out.println();  // Print a blank line for better separation between test cases
        }

        long stopTime = System.currentTimeMillis();  // Stop time after all optimizations
        long elapsedTime = stopTime - startTime;  // Calculate the elapsed time
        System.out.println("Total elapsed time: " + elapsedTime + " milliseconds");
    }

    private static List<Object[]> parseBPPFile(String filePath) {
        List<Object[]> testCases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<Integer> items = null;
            String testName = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("TEST")) {
                    if (items != null) {
                        testCases.add(new Object[]{testName, new ArrayList<>(items)});
                    }
                    testName = line.trim(); // Save the test case name
                    items = new ArrayList<>();
                } else {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 2) {
                        int weight = Integer.parseInt(parts[0]);
                        int count = Integer.parseInt(parts[1]);
                        for (int i = 0; i < count; i++) {
                            items.add(weight);
                        }
                    }
                }
            }
            if (items != null) {
                testCases.add(new Object[]{testName, items}); // Add the last test case
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testCases;
    }
}
