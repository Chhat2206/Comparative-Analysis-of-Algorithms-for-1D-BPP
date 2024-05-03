import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Arrays;

public class CuckooSearchGeneticAlgorithm {

    private List<int[]> nests;
    private List<Double> fitness; // Declare fitness list
    private double pa; // Discovery rate
    private double alpha; // Step size scale
    private double beta; // Used in Levy flight calculation
    private int numNests;
    private int maxGenerations;
    private int[] bestNest;
    private double bestFitness;
    private List<List<List<Integer>>> bestBinsConfigurations;
    private Random random;
    private int binCapacity;
    private List<Integer> items;

    public CuckooSearchGeneticAlgorithm(int binCapacity, List<Integer> items, int numNests, double pa, double alpha, int maxGenerations) {
        this.binCapacity = binCapacity;
        this.items = items;
        this.numNests = numNests;
        this.pa = pa;
        this.alpha = alpha;
        this.beta = 1.5; // Typically set in Cuckoo Search
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
            double fitnessValue = getFitness(newNest); // Calculate fitness for new nest
            fitness.add(fitnessValue); // Add the fitness value to the list
            if (fitnessValue < bestFitness) {
                bestFitness = fitnessValue;
                bestNest = newNest.clone();
            }
        }
    }

    private int[] generateNewSolution() {
        int[] newSolution = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            newSolution[i] = items.get(i);
        }
        Collections.shuffle(Arrays.asList(newSolution)); // Shuffling to create a random permutation
        return newSolution;
    }

    private int[] levyFlight(int[] sol) {
        int[] newSol = Arrays.copyOf(sol, sol.length); // Copy the existing solution
        for (int i = 0; i < sol.length; i++) {
            if (random.nextDouble() < pa) {
                // Perform a Lévy flight step
                double stepSize = getLevyStepSize();
                int step = (int) (stepSize * (random.nextDouble() - 0.5));
                int newIndex = (i + step + sol.length) % sol.length; // Ensure the new index wraps around the solution array
                if (newIndex < 0) {
                    newIndex += sol.length; // Adjust newIndex if it's negative
                }
                // Swap the items at indices i and newIndex
                int temp = newSol[i];
                newSol[i] = newSol[newIndex];
                newSol[newIndex] = temp;
            }
        }
        return newSol;
    }


    // Function to get a random number from Lévy distribution
    private double getLevyStepSize() {
        double u, v, stepSize;
        do {
            u = random.nextGaussian() * Math.sqrt(beta) / Math.pow(Math.abs(random.nextGaussian()), 1.0 / beta);
            v = random.nextGaussian();
            stepSize = u / Math.pow(Math.abs(v), 1.0 / beta);
        } while (Double.isNaN(stepSize) || Double.isInfinite(stepSize) || stepSize == 0);
        return stepSize;
    }


    private double getFitness(int[] solution) {
        List<List<Integer>> bins = packItems(solution);
        int totalWastage = 0;
        for (List<Integer> bin : bins) {
            int binWeight = bin.stream().mapToInt(Integer::intValue).sum();
            totalWastage += this.binCapacity - binWeight;
        }
        return 1.0 / totalWastage; // The fitness is the inverse of total wastage
    }

    public void startSearch() {
        for (int gen = 0; gen < maxGenerations; gen++) {
            for (int i = 0; i < numNests; i++) {
                int[] newSol = levyFlight(nests.get(i));
                double newFit = getFitness(newSol);
                if (newFit > fitness.get(i)) { // Access fitness using index
                    nests.set(i, newSol);
                    fitness.set(i, newFit); // Update fitness value in the list
                    if (newFit > bestFitness) {
                        bestFitness = newFit;
                        bestNest = newSol.clone();
                    }
                }
            }
            abandonWorseNests();
        }
    }

    // Update the abandonWorseNests method to handle fitness list
    private void abandonWorseNests() {
        for (int i = 0; i < nests.size(); i++) {
            if (random.nextDouble() < pa) {
                int[] newNest = generateNewSolution();
                double newFit = getFitness(newNest);
                if (newFit > fitness.get(i)) { // Access fitness using index
                    nests.set(i, newNest);
                    fitness.set(i, newFit); // Update fitness value in the list
                    if (newFit > bestFitness) {
                        bestFitness = newFit;
                        bestNest = newNest.clone();
                    }
                }
            }
        }
    }


    private List<List<Integer>> packItems(int[] solution) {
        List<List<Integer>> bins = new ArrayList<>();
        for (int weight : solution) {
            boolean placed = false;
            for (List<Integer> bin : bins) {
                int currentWeight = bin.stream().mapToInt(Integer::intValue).sum();
                if (currentWeight + weight <= binCapacity) {
                    bin.add(weight);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Integer> newBin = new ArrayList<>();
                newBin.add(weight);
                bins.add(newBin);
            }
        }
        return bins;
    }

    public static void main(String[] args) {
        String filePath = "src/BPP.txt"; // Provide the path to the bpp.txt file
        List<Object[]> testCases = parseBPPFile(filePath);

        for (Object[] testCase : testCases) {
            String datasetName = (String) testCase[0]; // Get the actual test case name from the first element
            List<Integer> items = (List<Integer>) testCase[1]; // Get the list of items from the second element
            CuckooSearchGeneticAlgorithm csga = new CuckooSearchGeneticAlgorithm(10000, items, 15, 0.25, 0.01, 1000);
            csga.startSearch();

            // Display the details of the current test case
            System.out.println("Dataset: " + datasetName); // Dataset name
            List<List<Integer>> bestConfiguration = csga.packItems(csga.bestNest); // Get best configuration
            System.out.println("Number of bins used: " +  bestConfiguration.size()); // Number of bins used
            for (int j = 0; j < bestConfiguration.size(); j++) {
                List<Integer> binItems = bestConfiguration.get(j);
                int binWeight = binItems.stream().mapToInt(Integer::intValue).sum();
                System.out.print("Bin " + (j + 1) + ": " + binItems); // Items in each bin
                System.out.println(" - Bin weight: " + binWeight + " out of " + csga.binCapacity); // Bin weight
            }

            System.out.println("Optimization completed for dataset " + datasetName + ".");
            System.out.println();
        }
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
