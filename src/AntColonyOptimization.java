import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AntColonyOptimization {
    private static final int MAX_ITERATIONS = 20;
    private static final int NUMBER_OF_ANTS = 5;
    private double ALPHA = 1.0;
    private double BETA = 2.0;
    private double DECAY_LOCAL = 0.1;
    private double DECAY_GLOBAL = 0.1;
    private int iterationsWithoutImprovement = 0;
    private int numItems;
    private int binCapacity;
    private int[] itemSizes;
    private double[][] pheromones;
    private double[][] heuristic;
    private Random random = new Random();
    private int[] bestSolution;
    private int bestBinCount = Integer.MAX_VALUE;
    private boolean[][] precedence;


    public AntColonyOptimization(int numItems, int binCapacity, int[] itemSizes, boolean[][] precedence) {
        this.numItems = numItems;
        this.binCapacity = binCapacity;
        this.itemSizes = itemSizes;
        this.pheromones = new double[numItems][numItems];
        this.heuristic = new double[numItems][numItems];
        this.precedence = precedence != null ? precedence : new boolean[numItems][numItems]; // Safely initialize
        initialize();
    }

    // Alternative constructor if precedence data isn't initially available
    public AntColonyOptimization(int numItems, int binCapacity, int[] itemSizes) {
        this(numItems, binCapacity, itemSizes, null); // Call the main constructor with null precedence
    }



    public void initialize() {
        // Initialize pheromones and heuristic matrix with initial values
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                double distanceFactor = 1.0 / (1.0 + Math.abs(i - j));
                pheromones[i][j] = 1.0 / (numItems * binCapacity) * distanceFactor;
                heuristic[i][j] = 1.0; // Initialize with neutral values, will be dynamically updated
            }
        }
    }

    private double calculateDynamicHeuristic(int item, int bin, int[] solution) {
        int currentLoad = getCurrentLoad(bin, solution);
        int remainingCapacity = binCapacity - currentLoad;

        // Calculate the heuristic based on the space utilization efficiency
        double spaceEfficiency = (double) itemSizes[item] / remainingCapacity;
        double penaltyForExcessSpace = remainingCapacity < itemSizes[item] ? 0.5 : 1.0; // Penalize if item does not fit perfectly

        // Combine space efficiency with the count of successors for dynamism
        double positionalWeight = calculatePositionalWeight(item);
        return spaceEfficiency * penaltyForExcessSpace * positionalWeight;
    }

    private int getCurrentLoad(int bin, int[] solution) {
        int load = 0;
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] == bin) {
                load += itemSizes[i];
            }
        }
        return load;
    }

    private double calculatePositionalWeight(int item) {
        int successors = 0;
        for (int j = 0; j < numItems; j++) {
            if (precedence[item][j]) {
                successors++;
            }
        }
        return 1.0 / (itemSizes[item] + 1) + successors * 0.1; // Incorporating the count of successors
    }

    private void updateHeuristicMatrix(int[] solution) {
        for (int item = 0; item < numItems; item++) {
            for (int bin = 0; bin < numItems; bin++) {
                heuristic[item][bin] = calculateDynamicHeuristic(item, bin, solution);
            }
        }
    }

    // Adjusted method to calculate positional weights based on successors and item size
    private double[] calculatePositionalWeights() {
        double[] weights = new double[numItems];
        for (int i = 0; i < numItems; i++) {
            int successors = 0;
            // Count the number of successors for each item
            for (int j = 0; j < numItems; j++) {
                if (precedence[i][j]) {
                    successors++;
                }
            }
            // Calculate weight considering both the item size and its successors
            // Example: smaller items with more successors should have higher weight
            weights[i] = 1.0 / (itemSizes[i] + 1) + successors * 0.1; // Smaller size and more successors increase the weight
        }
        return weights;
    }

    private void updatePheromones(int[] solution, int binCount, boolean isGlobal) {
        // Adjust ACO parameters dynamically based on the current search state
        adjustParameters();

        // Apply the local pheromone update rule first to all paths
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                // Local pheromone decay on all paths, slightly reducing pheromone concentration
                pheromones[i][j] *= (1 - DECAY_LOCAL);
            }
        }

        // Check if this update should apply the global update rules
        if (isGlobal) {
            // Apply global pheromone update only on the paths used in the best solution found
            for (int i = 0; i < numItems; i++) {
                int assignedBin = solution[i];
                if (assignedBin != -1) {
                    // Increase pheromone level significantly on the path used in the best solution
                    pheromones[i][assignedBin] += 1.0 / binCount;  // Increase proportional to solution quality
                }
            }
        }
    }

    private void adjustParameters() {
        if (iterationsWithoutImprovement > 5) {
            ALPHA += 0.1;
            BETA -= 0.1;
            if (ALPHA > 2.0) ALPHA = 2.0;  // Cap ALPHA to prevent it from becoming too influential
            if (BETA < 0.5) BETA = 0.5;  // Ensure BETA does not become too insignificant
        }

        if (iterationsWithoutImprovement > 10) {
            DECAY_LOCAL += 0.01;
            DECAY_GLOBAL += 0.01;
            if (DECAY_LOCAL > 0.2) DECAY_LOCAL = 0.2;  // Cap local decay to prevent too slow evaporation
            if (DECAY_GLOBAL > 0.2) DECAY_GLOBAL = 0.2;  // Cap global decay similarly
        } else if (iterationsWithoutImprovement == 0) {
            // Reset parameters to initial values if a new best solution was found
            ALPHA = 1.0;
            BETA = 2.0;
            DECAY_LOCAL = 0.1;
            DECAY_GLOBAL = 0.1;
        }
    }

    public int[] solve() {
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            boolean improved = false;
            List<int[]> solutions = new ArrayList<>();
            for (int ant = 0; ant < NUMBER_OF_ANTS; ant++) {
                int[] solution = constructSolution();
                solutions.add(solution);
                int binCount = evaluateSolution(solution);

                if (binCount < bestBinCount) {
                    bestBinCount = binCount;
                    bestSolution = solution.clone();
                    improved = true;
                }
            }
            if (!improved) {
                iterationsWithoutImprovement++;
            } else {
                iterationsWithoutImprovement = 0;  // Reset on improvement
            }
            updatePheromones(bestSolution, bestBinCount, true);
        }
        return bestSolution;
    }

    private int[] constructSolution() {
        int[] solution = new int[numItems];
        Arrays.fill(solution, -1);
        List<Integer> unassignedItems = IntStream.range(0, numItems).boxed().collect(Collectors.toList());

        while (!unassignedItems.isEmpty()) {
            int item = selectNextItem(solution, unassignedItems);
            if (item == -1) break;
            if (canPlaceItem(solution, item)) {  // Use canPlaceItem to check if item can be placed
                int bin = findBin(solution, item);
                if (bin != -1) {
                    solution[item] = bin;
                    unassignedItems.remove(Integer.valueOf(item));
                }
            }
        }
        return solution;
    }

    private boolean canPlaceItem(int[] solution, int item) {
        return allPredecessorsPlaced(solution, item);
    }

    private boolean allPredecessorsPlaced(int[] solution, int item) {
        for (int i = 0; i < numItems; i++) {
            if (precedence[i][item] && solution[i] == -1) {
                return false; // Predecessor `i` is not placed yet
            }
        }
        return true; // All predecessors are placed
    }

    private int findBin(int[] solution, int item) {
        for (int bin = 0; bin < numItems; bin++) {
            if (canPlaceItemInBin(solution, bin, item)) {
                return bin;
            }
        }
        return -1; // Return -1 if no valid bin is found
    }

    private int selectNextItem(int[] solution, List<Integer> unassignedItems) {
        // First, filter out items that cannot yet be placed because their predecessors have not all been placed
        List<Integer> eligibleItems = unassignedItems.stream()
                .filter(item -> allPredecessorsPlaced(solution, item))
                .collect(Collectors.toList());

        if (eligibleItems.isEmpty()) {
            return -1; // No items can be legally placed
        }

        // Calculate the probability for each eligible item
        double[] probabilities = new double[numItems];
        double totalProbability = 0.0;

        for (int item : eligibleItems) {
            double pheromoneContribution = 0.0;
            double heuristicContribution = heuristic[item][item]; // Assuming heuristic is pre-calculated for simplicity

            // Calculate the sum of pheromones to this item from all possible 'from' positions (considering an aggregated influence)
            for (int j = 0; j < numItems; j++) {
                if (solution[j] != -1) { // Only consider placed items
                    pheromoneContribution += Math.pow(pheromones[j][item], ALPHA);
                }
            }

            probabilities[item] = Math.pow(pheromoneContribution, ALPHA) * Math.pow(heuristicContribution, BETA);
            totalProbability += probabilities[item];
        }

        // Select an item based on the calculated probabilities using a roulette wheel mechanism
        double randomThreshold = random.nextDouble() * totalProbability;
        double cumulativeProbability = 0.0;

        for (int item : eligibleItems) {
            cumulativeProbability += probabilities[item];
            if (cumulativeProbability >= randomThreshold) {
                return item;
            }
        }

        return -1; // Should not happen, but a fallback in case of rounding errors
    }


    private boolean canPlaceItemInBin(int[] solution, int bin, int item) {
        int currentLoad = 0;
        for (int i = 0; i < numItems; i++) {
            if (solution[i] == bin) {
                currentLoad += itemSizes[i];
            }
        }
        return currentLoad + itemSizes[item] <= binCapacity;
    }

    private int evaluateSolution(int[] solution) {
        return (int) Arrays.stream(solution).distinct().count();
    }



    public static void main(String[] args) throws FileNotFoundException {
        String fileName = "src/BPP.txt";
        File file = new File(fileName);
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            long startTime = System.currentTimeMillis(); // Start time before the solution process

            String testName = scanner.nextLine().trim();
            int numberOfItems = Integer.parseInt(scanner.nextLine().trim());
            int binCapacity = Integer.parseInt(scanner.nextLine().trim());

            List<Integer> itemSizesList = new ArrayList<>();
            for (int i = 0; i < numberOfItems; i++) {
                String line = scanner.nextLine().trim();
                String[] parts = line.split("\\s+");
                int size = Integer.parseInt(parts[0]);  // Item size
                int count = Integer.parseInt(parts[1]);  // Number of such items
                for (int j = 0; j < count; j++) {
                    itemSizesList.add(size);
                }
            }

            int[] itemSizes = itemSizesList.stream().mapToInt(i -> i).toArray();
            AntColonyOptimization aco = new AntColonyOptimization(itemSizes.length, binCapacity, itemSizes);
            int[] solution = aco.solve();

            long endTime = System.currentTimeMillis(); // End time after the solution process

            System.out.println("Best solution for '" + testName + "' uses " + aco.bestBinCount + " bins.");
            System.out.println("Begin test for BIN '" + testName + "':");
            printSolution(solution, aco.itemSizes, aco.binCapacity);
            System.out.println("Total weight in bins for '" + testName + "': " + getTotalWeight(solution, aco.itemSizes));
            System.out.println("Total weight of all items in '" + testName + "': " + Arrays.stream(aco.itemSizes).sum());
            System.out.println("Execution Time: " + (endTime - startTime) / 1000.0 + " seconds");
        }

        scanner.close();
    }

    private static void printSolution(int[] solution, int[] itemSizes, int binCapacity) {
        HashMap<Integer, List<Integer>> bins = new HashMap<>();
        for (int i = 0; i < solution.length; i++) {
            bins.computeIfAbsent(solution[i], k -> new ArrayList<>()).add(itemSizes[i]);
        }

        bins.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.print("Bin " + (entry.getKey() + 1) + ": [");
                    System.out.print(entry.getValue().stream().map(Object::toString).collect(Collectors.joining(", ")));
                    int totalWeight = entry.getValue().stream().mapToInt(Integer::intValue).sum();
                    System.out.println("] - Total weight: " + totalWeight + "/" + binCapacity);
                });
    }

    private static int getTotalWeight(int[] solution, int[] itemSizes) {
        return Arrays.stream(itemSizes).sum();
    }
}