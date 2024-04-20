import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class AntColonyOptimization {
    private static final int MAX_ITERATIONS = 20;
    private static final int NUMBER_OF_ANTS = 10;
    private static final double ALPHA = 1.0;
    private static final double BETA = 2.0;
    private static final double RHO = 0.1;
    private static final double Q0 = 0.9;

    private int numItems;
    private int binCapacity;
    private int[] itemSizes;
    private double[][] pheromones;
    private double[][] heuristic;
    private Random random = new Random();
    private int[] bestSolution;
    private int bestBinCount = Integer.MAX_VALUE;

    public AntColonyOptimization(int numItems, int binCapacity, int[] itemSizes) {
        this.numItems = numItems;
        this.binCapacity = binCapacity;
        this.itemSizes = itemSizes;
        this.pheromones = new double[numItems][numItems];
        this.heuristic = new double[numItems][numItems];
    }

    public void initialize() {
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                pheromones[i][j] = 1.0 / (numItems * binCapacity);
                heuristic[i][j] = 1.0 / (itemSizes[i] + 1);
            }
        }
    }

    public int[] solve() {
        initialize();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            for (int ant = 0; ant < NUMBER_OF_ANTS; ant++) {
                int[] solution = constructSolution();
                int binCount = evaluateSolution(solution);

                if (binCount < bestBinCount) {
                    bestBinCount = binCount;
                    bestSolution = solution.clone();
                }

                updatePheromones(solution, binCount);
            }
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
            if (solution[i] == -1) {
                probabilities[i] = Math.pow(pheromones[i][i], ALPHA) * Math.pow(heuristic[i][i], BETA);
                sum += probabilities[i];
            }
        }

        double threshold = random.nextDouble() * sum;
        sum = 0.0;
        for (int i = 0; i < numItems; i++) {
            sum += probabilities[i];
            if (sum > threshold) {
                return i;
            }
        }

        return -1;
    }

    private int findBin(int[] solution, int item) {
        int bin = 0;
        while (bin < numItems && !canPlaceItemInBin(solution, bin, item)) {
            bin++;
        }
        return bin;
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

    private void updatePheromones(int[] solution, int binCount) {
        for (int i = 0; i < numItems; i++) {
            for (int j = 0; j < numItems; j++) {
                pheromones[i][j] *= (1 - RHO);
            }
        }

        for (int i = 0; i < numItems; i++) {
            int assignedBin = bestSolution[i];
            pheromones[i][assignedBin] += (1.0 / binCount);
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