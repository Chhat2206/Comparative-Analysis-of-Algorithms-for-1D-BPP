import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AntColonyOptimization {
    private static final int MAX_ITERATIONS = 20;
    private static final int NUMBER_OF_ANTS = 10;
    private double ALPHA = 1.0; // Influence of pheromone
    private double BETA = 2.0; // Influence of heuristic information
    private double q0 = 0.9; // Probability of exploiting the best option
    private double[][] pheromones; // Pheromones matrix
    private double[][] heuristic; // Heuristic matrix (could be inverse of the item size, number of successors, etc.)
    private Random random = new Random();
    private int iterationsWithoutImprovement = 0;
    private int numItems;
    private int binCapacity;
    private int[] itemSizes;

    private int[] bestSolution;
    private int bestBinCount = Integer.MAX_VALUE;
    private boolean[][] precedence;
    private int[] numberOfSuccessors;

    public AntColonyOptimization(int numItems, int binCapacity, int[] itemSizes, boolean[][] precedence) {
        this.numItems = numItems;
        this.binCapacity = binCapacity;
        this.itemSizes = itemSizes;
        this.pheromones = new double[numItems][numItems];
        this.heuristic = new double[numItems][numItems];
        this.precedence = precedence != null ? precedence : new boolean[numItems][numItems]; // Safely initialize
        initializePheromones();
        initializeHeuristic();
    }

    // Alternative constructor if precedence data isn't initially available
    public AntColonyOptimization(int numItems, int binCapacity, int[] itemSizes) {
        this(numItems, binCapacity, itemSizes, null); // Call the main constructor with null precedence
    }

    // Sets the initial values for the pheromones, which represents the attractiveness of placing items next to each other.
    // Initially, value is determined based on the estimated optimal solution & scaling factor.
    private void initializePheromones() {
        double mStar = estimateOfOptimalSolution();
        double scaleFactor = 0.01; // Adjust this factor based on empirical testing
        double initialPheromoneValue = scaleFactor / (numItems * mStar);
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                pheromones[i][j] = initialPheromoneValue;
            }
        }
        System.out.println("Initialization complete: Pheromones are set with initial value: " + initialPheromoneValue);
    }

    // Calculates pheromone values by placing an item relative to other items, influcing the probability of selecting placements during solution construction.
    private double[] calculatePheromoneSummations(int item) {
        double[] pheromoneSums = new double[numItems]; // Assuming numItems refers to potential bin positions as well.
        for (int bin = 0; bin < numItems; bin++) {
            double sum = 0.0;
            for (int prevBin = 0; prevBin <= bin; prevBin++) {
                sum += pheromones[item][prevBin];
            }
            pheromoneSums[bin] = sum;
        }
        return pheromoneSums;
    }

    // Simulated ant constructing a solution and evaluating it. If the ant finds a better solution, it updates it.
    public int[] solve() {
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            boolean improved = false;
            List<int[]> solutions = new ArrayList<>();
            for (int ant = 0; ant < NUMBER_OF_ANTS; ant++) {
                int[] solution = constructSolution();
                int binCount = evaluateSolution(solution);
                if (binCount < bestBinCount) {
                    bestBinCount = binCount;
                    bestSolution = solution.clone();
                    improved = true;
                }
            }
            if (!improved) {
                iterationsWithoutImprovement = 0;
                updateGlobalPheromone(bestSolution);
            } else {
                iterationsWithoutImprovement++;
            }
        }
        return bestSolution;
    }

    // Constructs the solution for one ant. Repeated selects the next best item to place based on pheronome levels, heuristic values, and item precedence, ensuring
    // items are placed in feasable bins according to capacity
    private int[] constructSolution() {
        int[] solution = new int[numItems];
        Arrays.fill(solution, -1);
        List<Integer> unassignedItems = IntStream.range(0, numItems).boxed().collect(Collectors.toList());

        while (!unassignedItems.isEmpty()) {
            int item = selectNextItem(solution, unassignedItems);
            if (item == -1) break; // No valid item could be placed, exit loop
            int bin = findBin(solution, item); // Method to find a suitable bin
            if (bin != -1) {
                solution[item] = bin;
                updateLocalPheromone(item, bin);  // Update local pheromone after placing item
                unassignedItems.remove(Integer.valueOf(item));
            }
        }
        return solution;
    }

    private void updateLocalPheromone(int i, int j) {
        double rho1 = 0.1; // Local evaporation rate
        double tau0 = 1.0 / (numItems * estimateOfOptimalSolution()); // Initial pheromone level
        pheromones[i][j] = (1 - rho1) * pheromones[i][j] + rho1 * tau0;
    }

    private void updateGlobalPheromone(int[] bestSolution) {
        double rho2 = 0.1; // Global evaporation rate
        for (int i = 0; i < numItems; i++) {
            int j = bestSolution[i]; // assuming bestSolution[i] gives the bin in which item i is placed
            if (j != -1) { // Check if the item was placed
                double deltaTau = 1.0 / bestBinCount; // Example of setting Δτ_ij
                pheromones[i][j] = (1 - rho2) * pheromones[i][j] + rho2 * deltaTau;
            }
        }
    }

    private double estimateOfOptimalSolution() {
        int totalSize = Arrays.stream(itemSizes).sum();
        return Math.ceil((double) totalSize / binCapacity);
    }

    // Selects the next item to place in the bin packing sequence based on the calculated probability or both
    // phereomone and heuristic values, by applying a roulette wheel selection mechanism
    private int selectNextItem(int[] solution, List<Integer> unassignedItems) {
        List<Integer> eligibleItems = unassignedItems.stream()
                .filter(item -> allPredecessorsPlaced(solution, item))
                .collect(Collectors.toList());

        if (eligibleItems.isEmpty()) {
            return -1; // No items can be legally placed
        }

        // Adjust pheromone contribution by summation rule
        Map<Integer, Double> pheromoneContributions = new HashMap<>();
        for (int item : eligibleItems) {
            pheromoneContributions.put(item, calculatePheromoneSummations(item)[item]); // Use summation rule here
        }

        // Calculate probabilities incorporating summation rule
        double[] probabilities = new double[numItems];
        double totalProbability = 0.0;

        for (int item : eligibleItems) {
            double pheromoneContribution = pheromoneContributions.get(item);
            double heuristicContribution = heuristic[item][item]; // Using heuristic value for simplicity

            probabilities[item] = Math.pow(pheromoneContribution, ALPHA) * Math.pow(heuristicContribution, BETA);
            totalProbability += probabilities[item];
        }

        // Select an item based on the calculated probabilities using a roulette wheel mechanism
        double randomThreshold = random.nextDouble() * totalProbability;
        double cumulativeProbability = 0.0;

        for (int item : eligibleItems) {
            cumulativeProbability += probabilities[item];
            if (cumulativeProbability >= randomThreshold) {
//                System.out.println("Selecting next item: Current probabilities - " + Arrays.toString(probabilities));
                return item;
            }
        }

        return -1; // Fallback in case of rounding errors
    }




    // Checks if an item can be placed in any bin based on precedence constraints
    private boolean canPlaceItem(int[] solution, int item) {
        return allPredecessorsPlaced(solution, item);
    }

    // Checks if predessors of a given item has been placed
    private boolean allPredecessorsPlaced(int[] solution, int item) {
        for (int i = 0; i < numItems; i++) {
            if (precedence[i][item] && solution[i] == -1) {
                return false; // Predecessor `i` is not placed yet
            }
        }
        return true; // All predecessors are placed
    }

    // Checks if item can be placed to make sure its not over 10k
    private int findBin(int[] solution, int item) {
        for (int bin = 0; bin < numItems; bin++) {
            if (canPlaceItemInBin(solution, bin, item)) {
                return bin;
            }
        }
        return -1; // Return -1 if no valid bin is found
    }

    private void calculateNumberOfSuccessors() {
        numberOfSuccessors = new int[numItems];
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                if (precedence[i][j]) {  // If item i must precede item j
                    numberOfSuccessors[i]++;
                }
            }
        }
    }

    // Heuristic initialized based on item size and number of successors, influencing the decision making process
    // by giving priority by placing items with fewer or smaller successors earlier
    private void initializeHeuristic() {
        calculateNumberOfSuccessors();

        heuristic = new double[numItems][numItems];
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                if (i== j) {
                    heuristic[i][j] = 1.0 / (itemSizes[i] + numberOfSuccessors[i] * 0.1);
                }
            }
        }
        System.out.println("Heuristic initialized based on item sizes and number of successors.");
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