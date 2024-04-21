import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class AntColonyOptimization {
    private static final int MAX_ITERATIONS = 200;
    private static final int NUMBER_OF_ANTS = 50;
    private static final double ALPHA = 1.0;
    private static final double BETA = 2.0;
    private static final double RHO = 0.1;

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
        // Calculate the positional weights or some other metric if needed
        double[] positionalWeights = calculatePositionalWeights();

        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                pheromones[i][j] = 1.0 / (numItems * binCapacity);
                // Combine item size and positional weights in the heuristic
                // Assuming the smaller the item and the higher the weight, the better (e.g., smaller items with more successors should be placed earlier)
                heuristic[i][j] = (1.0 / (itemSizes[i] + 1)) * positionalWeights[i];
            }
        }
    }

    // Example method to calculate positional weights based on successors
    private double[] calculatePositionalWeights() {
        double[] weights = new double[numItems];
        for (int i = 0; i < numItems; i++) {
            int successors = 0;
            // Count the number of times an item is a predecessor to others
            for (int j = 0; j < numItems; j++) {
                if (precedence[i][j]) { // i must precede j
                    successors++;
                }
            }
            // Weight could be proportional to the number of successors
            weights[i] = 1 + successors * 0.1; // Example: base weight + increment per successor
        }
        return weights;
    }


    public int[] solve() {
        initialize();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            List<int[]> solutions = new ArrayList<>();
            for (int ant = 0; ant < NUMBER_OF_ANTS; ant++) {
                int[] solution = constructSolution();
                solutions.add(solution);
                int binCount = evaluateSolution(solution);

                if (binCount < bestBinCount) {
                    bestBinCount = binCount;
                    bestSolution = solution.clone();
                }

                // Apply local pheromone update during the construction of each solution
                updatePheromones(solution, binCount, false);
            }

            // After all solutions are constructed, apply global update using the best solution found
            updatePheromones(bestSolution, bestBinCount, true);
        }

        return bestSolution;
    }


    private int[] constructSolution() {
        int[] solution = new int[numItems];
        Arrays.fill(solution, -1);

        for (int i = 0; i < numItems; i++) {
            int nextItem = selectNextItem(solution);
            int binIndex = findBin(solution, nextItem);
            solution[nextItem] = binIndex;
        }

        return solution;
    }

    private int selectNextItem(int[] solution) {
        double[] probabilities = new double[numItems];
        double sum = 0.0;

        for (int i = 0; i < numItems; i++) {
            if (solution[i] == -1) {  // Check if item i is unassigned
                double cumulativePheromone = 0.0;
                for (int h = 0; h < numItems; h++) {
                    if (solution[h] == -1) {
                        cumulativePheromone += pheromones[h][i];  // Sum pheromones from all unassigned items up to i
                    }
                }
                probabilities[i] = Math.pow(cumulativePheromone, ALPHA) * Math.pow(heuristic[i][i], BETA);
                sum += probabilities[i];
            }
        }

        // Select an item based on the accumulated probabilities
        double threshold = random.nextDouble() * sum;
        sum = 0.0;
        for (int i = 0; i < numItems; i++) {
            if (solution[i] == -1) {
                sum += probabilities[i];
                if (sum > threshold) {
                    return i;
                }
            }
        }

        return -1;  // If no item is selected, which should not occur if implemented correctly
    }



    private int findBin(int[] solution, int item) {
        int bin = 0;
        while (bin < numItems) {
            if (canPlaceItemInBin(solution, bin, item) && allPredecessorsPlaced(solution, item, bin)) {
                return bin;
            }
            bin++;
        }
        return bin;  // Fallback to next available bin if no suitable bin found
    }

    private boolean allPredecessorsPlaced(int[] solution, int item, int bin) {
        for (int i = 0; i < numItems; i++) {
            if (precedence[i][item] && (solution[i] == -1 || solution[i] > bin)) {
                return false;  // Predecessor i is not placed or placed in a later bin than item
            }
        }
        return true;
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

    // Assuming an additional method parameter that tells us whether to perform local or global update
    private void updatePheromones(int[] solution, int binCount, boolean isGlobal) {
        if (isGlobal) {
            // Global Update: Reinforce the path of the best solution found so far
            for (int i = 0; i < numItems; i++) {
                for (int j = 0; j < numItems; j++) {
                    // Apply evaporation
                    pheromones[i][j] *= (1 - RHO);
                }
            }

            for (int i = 0; i < numItems; i++) {
                int assignedBin = solution[i];
                // Reinforce the pheromone trail for the best solution
                pheromones[i][assignedBin] += 1.0 / binCount;
            }
        } else {
            // Local Update: Apply during the construction of solutions to diversify the search
            for (int i = 0; i < numItems; i++) {
                for (int j = 0; j < numItems; j++) {
                    // Only apply local updating rule to parts of the solution being constructed
                    if (solution[i] == j) {
                        pheromones[i][j] = (1 - RHO) * pheromones[i][j] + RHO * (1.0 / numItems);
                    }
                }
            }
        }
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