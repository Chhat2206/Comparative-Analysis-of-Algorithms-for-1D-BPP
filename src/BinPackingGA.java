import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class BinPackingGA {
    // Constants
    private static final int POPULATION_SIZE = 100; // Size of the population in each generation
    private static final int GENERATIONS = 3000; // Number of generations for which the algorithm will run
    private static final Random random = new Random(); // Random number generator
    private static final int BIN_CAPACITY = 10000; // The capacity of each bin
    private static final int OFFSPRING_SIZE = 50; // Number of offspring to produce in each generation

    // Generates the initial population for the genetic algorithm using a best-fit decreasing strategy
    private static List<Individual> generateInitialPopulation(List<Item> items, int binCapacity, int populationSize) {
        System.out.println("Generating initial population with BFD...");

        List<Individual> population = new ArrayList<>();

        // Sort items by size in decreasing order
        Collections.sort(items, (item1, item2) -> Integer.compare(item2.size, item1.size));

        for (int i = 0; i < populationSize; i++) {
            List<Bin> bins = new ArrayList<>();
            for (Item item : items) {
                Bin bestFitBin = null;
                int minSpaceLeft = Integer.MAX_VALUE;

                // Find the bin that fits the item best
                for (Bin bin : bins) {
                    int spaceLeft = binCapacity - bin.getCurrentSize();
                    if (spaceLeft >= item.size && spaceLeft < minSpaceLeft) {
                        bestFitBin = bin;
                        minSpaceLeft = spaceLeft;
                    }
                }

                if (bestFitBin != null) {
                    // Place the item in the best-fit bin
                    bestFitBin.addItem(item);
                } else {
                    // Create a new bin for the item
                    Bin newBin = new Bin();
                    newBin.addItem(item);
                    bins.add(newBin);
                }
            }
            population.add(new Individual(bins));
        }

        System.out.println("Initial population generated with BFD");
        return population;
    }

    private static Map<String, List<Item>> loadItems(String fileName) throws FileNotFoundException {
        Map<String, List<Item>> testCases = new HashMap<>();
        File file = new File(fileName);

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String testName = scanner.nextLine().trim(); // Test case name
                int numberOfItems = Integer.parseInt(scanner.nextLine().trim());
                int binCapacity = Integer.parseInt(scanner.nextLine().trim()); // Bin capacity, can be used as needed

                List<Item> items = new ArrayList<>();
                for (int i = 0; i < numberOfItems; i++) {
                    String[] line = scanner.nextLine().trim().split("\\s+");
                    int weight = Integer.parseInt(line[0]);
                    int quantity = Integer.parseInt(line[1]);

                    for (int j = 0; j < quantity; j++) {
                        items.add(new Item(weight));
                    }
                }
                testCases.put(testName, items);
            }
        }
        return testCases;
    }

//    private static void applyMGG(List<Individual> population, int offspringSize) {
//        // Step 1: Select two parents
//        Individual parent1 = population.get(random.nextInt(population.size()));
//        Individual parent2 = population.get(random.nextInt(population.size()));
//
//        // Step 2: Generate offspring
//        List<Individual> offspring = generateOffspring(parent1, parent2, offspringSize);
//
//        // Step 3: Combine parents and offspring into one group
//        List<Individual> candidates = new ArrayList<>(offspring);
//        candidates.add(parent1);
//        candidates.add(parent2);
//
//        // Step 4: Evaluate and select best solutions
//        candidates.sort(Comparator.comparing(Individual::getFitness));
//        Individual bestOffspring1 = candidates.get(0);
//        Individual bestOffspring2 = candidates.get(1);
//
//        // Step 5: Replace parents in the population with the best offspring
//        replaceIndividuals(population, parent1, parent2, bestOffspring1, bestOffspring2);
//    }

    private static void applyMGG(List<Individual> population, int offspringSize) {
        for (int i = 0; i < offspringSize; i++) {
            // Step 1: Select two parents
            Individual parent1 = population.get(random.nextInt(population.size()));
            Individual parent2 = population.get(random.nextInt(population.size()));

            // Step 2: Generate offspring
            Individual offspring = crossover(parent1, parent2);
            mutate(offspring);

            int index1 = population.indexOf(parent1);
            int index2 = population.indexOf(parent2);

            // Step 3: Replace parents with offspring if offspring is better
            if (index1 != -1 && offspring.getFitness() > parent1.getFitness()) {
                population.set(index1, offspring);
            }
            if (index2 != -1 && offspring.getFitness() > parent2.getFitness()) {
                population.set(index2, offspring);
            }
        }
    }


    private static List<Individual> generateOffspring(Individual parent1, Individual parent2, int offspringSize) {
        List<Individual> offspring = new ArrayList<>();
        for (int i = 0; i < offspringSize; i++) {
            Individual child = crossover(parent1, parent2); // Existing crossover logic
            mutate(child); // Existing mutation logic
            offspring.add(child);
        }
        return offspring;
    }

    private static void replaceIndividuals(List<Individual> population, Individual parent1, Individual parent2, Individual offspring1, Individual offspring2) {
        int index1 = population.indexOf(parent1);
        int index2 = population.indexOf(parent2);

        population.set(index1, offspring1);
        population.set(index2, offspring2);
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Program started");

        Map<String, List<Item>> testCases = loadItems("src/BPP.txt");

        for (Map.Entry<String, List<Item>> entry : testCases.entrySet()) {
            String testCaseName = entry.getKey();
            List<Item> items = entry.getValue();

            printColored("Solving test case: " + testCaseName, ANSI_YELLOW);
            System.out.println("Items loaded: " + items.size());

            List<Individual> population = generateInitialPopulation(items, BIN_CAPACITY, POPULATION_SIZE);
            System.out.println("Initial population generated");
            for (int i = 0; i < GENERATIONS; i++) {
                applyMGG(population, OFFSPRING_SIZE);
                if (i % 100 == 0) {
                    Individual bestIndividual = findBestSolution(population);
                    System.out.println("Generation " + i + ", Best Fitness: " + bestIndividual.getFitness());
                }
            }

            Individual bestSolution = findBestSolution(population);
            printColored("Best solution for " + testCaseName + " uses " + bestSolution.bins.size() + " bins.", ANSI_GREEN);

            // Print the details of each bin in the best solution
            System.out.println("\nBegin test for BIN " + testCaseName + ":");
            int totalWeightInBins = 0;
            for (int i = 0; i < bestSolution.bins.size(); i++) {
                Bin bin = bestSolution.bins.get(i);
                int binTotalWeight = bin.items.stream().mapToInt(item -> item.size).sum();
                totalWeightInBins += binTotalWeight;
                System.out.println("Bin " + (i + 1) + ": " + bin.items + " - Total weight: " + binTotalWeight + "/" + BIN_CAPACITY);
            }

            // Calculate and print the total weight of all items
            int totalWeightOfAllItems = items.stream().mapToInt(item -> item.size).sum();
            System.out.println("Total weight in bins for " + testCaseName + ": " + totalWeightInBins);
            System.out.println("Total weight of all items in " + testCaseName + ": " + totalWeightOfAllItems);

            // Check if total weights match
            if(totalWeightOfAllItems != totalWeightInBins){
                System.out.println("Warning: There is a discrepancy in the total weights for " + testCaseName);
            }

            System.out.println("\n");
        }
    }

    // ANSI color code declarations
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";

    // Method for printing colored text to the console
    private static void printColored(String text, String colorCode) {
        System.out.println(colorCode + text + ANSI_RESET);
    }

    private static Individual crossover(Individual parent1, Individual parent2) {
        // Combine items from both parents into a single list
        List<Item> allItems = new ArrayList<>();
        for (Bin bin : parent1.bins) {
            allItems.addAll(bin.items);
        }
        for (Bin bin : parent2.bins) {
            allItems.addAll(bin.items);
        }

        // Remove duplicate items if any
        Set<Item> uniqueItems = new HashSet<>(allItems);
        allItems.clear();
        allItems.addAll(uniqueItems);

        // Shuffle the list to introduce randomness
        Collections.shuffle(allItems, random);

        // Allocate items to bins in the offspring
        List<Bin> offspringBins = new ArrayList<>();
        Bin currentBin = new Bin(); // Start with an empty bin
        offspringBins.add(currentBin); // Add the first bin to the list
        for (Item item : allItems) {
            // Check if adding the item to the current bin exceeds its capacity
            if (!currentBin.canAddItem(item, BIN_CAPACITY)) {
                // If the current bin is full, create a new bin
                currentBin = new Bin();
                offspringBins.add(currentBin); // Add the new bin to the list
            }
            // Add the item to the current bin
            currentBin.addItem(item);
        }

        return new Individual(offspringBins);
    }


    private static void mutate(Individual individual) {
        if (individual.bins.isEmpty()) return;

        // Randomly select a bin to mutate
        Bin selectedBin = individual.bins.get(random.nextInt(individual.bins.size()));
        if (selectedBin.items.isEmpty()) return;

        // Remove a random item from the selected bin
        Item removedItem = selectedBin.items.remove(random.nextInt(selectedBin.items.size()));

        // Attempt to place the removed item into a different bin
        boolean placed = false;
        for (Bin bin : individual.bins) {
            if (bin != selectedBin && bin.canAddItem(removedItem, BIN_CAPACITY)) {
                bin.addItem(removedItem);
                placed = true;
                break;
            }
        }

        // If item not placed, attempt to merge with other bins or create a new bin
        if (!placed) {
            boolean merged = false;
            for (Bin bin : individual.bins) {
                if (bin != selectedBin && bin.canMerge(selectedBin, BIN_CAPACITY)) {
                    bin.merge(selectedBin);
                    bin.addItem(removedItem); // Add the removed item to the merged bin
                    merged = true;
                    break;
                }
            }

            // If no merging is possible, either add the item back to its original bin or create a new bin
            if (!merged) {
                if (selectedBin.canAddItem(removedItem, BIN_CAPACITY)) {
                    // If the original bin can still accommodate the item, add it back
                    selectedBin.addItem(removedItem);
                } else {
                    // If not, create a new bin for the removed item
                    Bin newBin = new Bin();
                    newBin.addItem(removedItem);
                    individual.bins.add(newBin);
                }
            }
        }

        // Clean up any empty bins
        individual.bins.removeIf(bin -> bin.items.isEmpty());
    }

    private static Individual findBestSolution(List<Individual> population) {
        return Collections.max(population, Comparator.comparing(Individual::getFitness));
    }

    static class Individual {
        List<Bin> bins;

        public Individual(List<Bin> bins) {
            this.bins = bins;
        }

        public int getFitness() {
            return -bins.size();
        }

    }
}
