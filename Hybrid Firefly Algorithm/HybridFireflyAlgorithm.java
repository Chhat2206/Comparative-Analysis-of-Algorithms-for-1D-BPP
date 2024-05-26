import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class HybridFireflyAlgorithm {
    private List<Integer> items; // Items to be packed
    private List<List<Integer>> population; // Population of solutions
    private List<Integer> bestConfiguration; // Best configuration found
    private int bestBinCount; // Number of bins in the best configuration
    private int binCapacity;
    private double gamma; // Light absorption coefficient
    private double beta0; // Initial attractiveness
    private double alpha; // Randomness component
    private double mutationRate;
    private Random random;

    public HybridFireflyAlgorithm(int binCapacity, List<Integer> items, int populationSize, double gamma, double beta0, double alpha, double mutationRate) {
        this.binCapacity = binCapacity;
        this.items = new ArrayList<>(items);
        this.gamma = gamma;
        this.beta0 = beta0;
        this.alpha = alpha;
        this.mutationRate = mutationRate;
        this.population = new ArrayList<>();
        this.bestBinCount = Integer.MAX_VALUE;
        this.random = new Random();

        initializePopulation(populationSize);
    }

    private void initializePopulation(int populationSize) {
        for (int i = 0; i < populationSize; i++) {
            Collections.shuffle(items, random);
            population.add(new ArrayList<>(items));
        }
    }

    public void optimize(int maxGenerations) {
        for (int t = 0; t < maxGenerations; t++) {
            List<List<Integer>> newPopulation = new ArrayList<>();
            for (int i = 0; i < population.size(); i++) {
                List<Integer> newFirefly = new ArrayList<>(population.get(i));
                int currentBinCount = evaluate(newFirefly);
                if (currentBinCount < bestBinCount) {
                    bestBinCount = currentBinCount;
                    bestConfiguration = new ArrayList<>(newFirefly);
                }

                for (int j = 0; j < population.size(); j++) {
                    if (i != j) {
                        if (evaluate(population.get(j)) < currentBinCount) {
                            moveFirefly(newFirefly, population.get(j));
                        }
                    }
                }

                if (random.nextDouble() < mutationRate) {
                    mutateFirefly(newFirefly);
                }
                newPopulation.add(newFirefly);
            }
            population = newPopulation;
            // Dynamic adjustment of mutation rate and alpha
           mutationRate *= (1.0 - (double) t / maxGenerations);
            alpha *= 0.95; // Decrease alpha to reduce randomness over time
        }
    }

    private void moveFirefly(List<Integer> firefly, List<Integer> brighterFirefly) {
        double r = calculateDistance(firefly, brighterFirefly);
        double beta = beta0 * Math.exp(-gamma * r);
        for (int i = 0; i < firefly.size(); i++) {
            int movement = (int) ((brighterFirefly.get(i) - firefly.get(i)) * beta + alpha * random.nextGaussian());
            firefly.set(i, Math.min(Math.max(firefly.get(i) + movement, 0), binCapacity)); // Ensuring the new position is within bounds
        }
    }

    private void mutateFirefly(List<Integer> firefly) {
        int segmentLength = firefly.size() / 4;  // Example: mutate a quarter of the list
        int start = random.nextInt(firefly.size() - segmentLength);
        int end = start + segmentLength;
        List<Integer> subList = new ArrayList<>(firefly.subList(start, end));
        Collections.shuffle(subList, random);
        for (int i = start; i < end; i++) {
            firefly.set(i, subList.get(i - start));
        }
    }


    private double calculateDistance(List<Integer> config1, List<Integer> config2) {
        double sum = 0.0;
        for (int k = 0; k < Math.min(config1.size(), config2.size()); k++) {
            sum += Math.pow(config1.get(k) - config2.get(k), 2);
        }
        return Math.sqrt(sum);
    }

    private int evaluate(List<Integer> configuration) {
        List<List<Integer>> bins = packBins(configuration, binCapacity);
        int penalty = 0;
        for (List<Integer> bin : bins) {
            int binLoad = bin.stream().mapToInt(Integer::intValue).sum();
            penalty += binCapacity - binLoad; // Penalize for unused space
        }
        return bins.size() + penalty / 1000;  // Adjust the penalty factor appropriately
    }

    private static List<List<Integer>> packBins(List<Integer> items, int capacity) {
        List<List<Integer>> bins = new ArrayList<>();
        for (int item : items) {
            boolean placed = false;
            for (List<Integer> bin : bins) {
                int currentWeight = bin.stream().mapToInt(Integer::intValue).sum();
                if (currentWeight + item <= capacity) {
                    bin.add(item);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Integer> newBin = new ArrayList<>();
                newBin.add(item);
                bins.add(newBin);
            }
        }
        return bins;
    }

    private static List<Object[]> parseBPPFile(String filePath) {
        List<Object[]> testCases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<Integer> items = null;
            String testName = null; // Variable to store the test case name
            while ((line = reader.readLine()) != null) {
                if (line.contains("TEST")) {
                    if (items != null) {
                        testCases.add(new Object[]{testName, new ArrayList<>(items)});
                    }
                    testName = line.trim(); // Save the test case name
                    items = new ArrayList<>();
                } else {
                    String[] parts = line.trim().split("\\s+");
                    if(parts.length == 2){
                        int weight = Integer.parseInt(parts[0]);
                        int count = Integer.parseInt(parts[1]);
                        for(int i = 0; i < count; i++){
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


    public static void main(String[] args) {
        String filePath = "Hybrid Firefly Algorithm/BPP.txt";
        List<Object[]> testCases = parseBPPFile(filePath);
        long startTime = System.currentTimeMillis();

        for (Object[] testCase : testCases) {
            String testName = (String) testCase[0]; // Get the test case name
            List<Integer> testCaseItems = (List<Integer>) testCase[1];
            HybridFireflyAlgorithm hfa = new HybridFireflyAlgorithm(10000, testCaseItems, 15, 0.3, 0.3, 0.1, 0.95);
            hfa.optimize(100);
            System.out.println("Optimization completed for dataset " + testName + ".");
            List<List<Integer>> bins = evaluateBins(hfa.bestConfiguration, hfa.binCapacity);
            System.out.println("Number of bins used: " + bins.size());

            // We need to reevaluate the best configuration to display bins correctly
            for (int i = 0; i < bins.size(); i++) {
                List<Integer> bin = bins.get(i);
                int binWeight = bin.stream().mapToInt(Integer::intValue).sum();
                System.out.println("Bin " + (i + 1) + " contains: " + bin + " - Bin weight: " + binWeight + " out of " + hfa.binCapacity);
            }
        }
        long stopTime = System.currentTimeMillis(); // Stop time
        long elapsedTime = stopTime - startTime; // Elapsed time
        System.out.println("Elapsed time: " + elapsedTime + " milliseconds");
    }

    private static List<List<Integer>> evaluateBins(List<Integer> configuration, int binCapacity) {
        List<List<Integer>> bins = new ArrayList<>();
        for (int item : configuration) {
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
            }
        }
        return bins;
    }

}