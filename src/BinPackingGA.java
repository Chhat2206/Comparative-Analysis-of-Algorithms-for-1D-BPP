import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BinPackingGA {
    // Constants
    private static final int POPULATION_SIZE = 1000;
    // n set between 50 and 1000. Smaller problems might work well with 50-100, while larger,
    // more complex problems may require more diverse genetic pool but increase computation time.
    // A balance must be found between diversity and computational efficiency.

    private static final int GENERATIONS = 50;
    // Can range from tens to thousands,
    // More generations allow more time for the algorithm to evolve solutions but increase computation time.
    // Stop the algorithm if no significant improvement is observed over several generations.
    private static final Random random = new Random(); // Random number generator + MAY BE THE ISSUE
    private static final int BIN_CAPACITY = 10000; // The capacity of each bin
    private static final int OFFSPRING_SIZE = 500;
    // A larger population size allows for more exploration but also increases computational complexity. 500 individuals is a reasonable size for many problems

    private static final double MUTATION_RATE = 0.01;
    // the probability of random changes in individual genes.
    // Generally low, often between 0.001 and 0.01.
    // A higher rate can prevent premature convergence to local optima by introducing diversity,
    // but too high a rate can turn the search into a random walk.

    private static final double CROSSOVER_PROBABILITY = 0.7;
    //The probability of crossover (mating) between individuals.
    //Typically set between 0.6 and 0.9.
    //Tweaking: Higher rates increase genetic diversity but might disrupt convergence.

    // Adjust initial population generation method

    private static final int TOURNAMENT_SIZE = 5;
    private static final int ELITISM_SIZE = 2;

    // This method is responsible for creating the initial population of solutions for the genetic algorithm.
    // Creates an empty list to hold the population of individuals.
    // Loops over the number of individuals to be generated (specified by populationSize).
    // Shuffles the items - It takes a list of original items, creates a copy, and then shuffles this copy randomly. This step is crucial as it introduces randomness in the initial solutions, promoting genetic diversity from the outset.
    // Generates an individual - For each shuffled list of items, it calls the generateIndividualUsingFF method to create a new individual based on the First-Fit heuristic.
    private static List<Individual> generateInitialPopulation(List<Item> originalItems, int binCapacity, int populationSize) {
        List<Individual> population = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            List<Item> items = new ArrayList<>(originalItems);
            Collections.shuffle(items);  // Randomly shuffle the items
            population.add(generateIndividualUsingFF(items, binCapacity));
        }

        return population;
    }

    // This method implements the First-Fit algorithm, which is used to place items into bins. Here's the step-by-step process:
    // Initializes an empty list of bins.
    // Iterates over each item in the shuffled list:
    //   It checks if there is an existing bin that can accommodate the item without exceeding the bin's capacity.
    //   If such a bin exists, the item is added to the first suitable bin found.
    //   If no suitable bin is found, a new bin is created, and the item is added to this new bin.
    // Returns an individual - An individual, represented by the list of bins (each containing a set of items), is returned. This individual is a solution to the bin packing problem under the First-Fit strategy.
    private static Individual generateIndividualUsingFF(List<Item> items, int binCapacity) {
        List<Bin> bins = new ArrayList<>();
        for (Item item : items) {
            Optional<Bin> bin = bins.stream()
                    .filter(b -> b.canAddItem(item, binCapacity))
                    .findFirst();

            if (bin.isPresent()) {
                bin.get().addItem(item);
            } else {
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
        return new Individual(bins);
    }

    private static Individual crossover(Individual parent1, Individual parent2, int binCapacity) {
        List<Bin> offspringBins = new ArrayList<>();
        Set<Item> includedItems = new HashSet<>();

        // Randomly select bins from parent1 and preserve their item sets.
        for (Bin bin : parent1.bins) {
            if (random.nextBoolean()) {
                offspringBins.add(new Bin(new ArrayList<>(bin.items)));
                includedItems.addAll(bin.items);
            }
        }

        // Add non-conflicting bins from parent2, ensuring item sets are preserved.
        for (Bin bin : parent2.bins) {
            if (Collections.disjoint(bin.items, includedItems)) {
                offspringBins.add(new Bin(new ArrayList<>(bin.items)));
                includedItems.addAll(bin.items);
            }
        }

        // Handle remaining items: identify missing items and reintegrate using designed heuristic methods.
        Set<Item> allItems = getAllItems(parent1, parent2);
        allItems.removeAll(includedItems);
        reintegrateItems(offspringBins, allItems, binCapacity);

        return new Individual(offspringBins);
    }

    private static void reintegrateItems(List<Bin> bins, Set<Item> remainingItems, int binCapacity) {
        // First-Fit Decrease: Attempt to place remaining items using a first-fit heuristic
        applyFirstFitDecreasing(bins, remainingItems, binCapacity);

        // Best-Fit: Attempt to optimize placement by minimizing the slack in bins
        applyBestFitHeuristic(bins, remainingItems, binCapacity);
    }

    private static void applyBestFitHeuristic(List<Bin> bins, Set<Item> items, int binCapacity) {
        for (Item item : new ArrayList<>(items)) {
            Bin bestFitBin = null;
            int minSpaceLeft = Integer.MAX_VALUE;

            for (Bin bin : bins) {
                int spaceLeft = binCapacity - bin.getCurrentSize();
                if (spaceLeft >= item.size && spaceLeft < minSpaceLeft) {
                    bestFitBin = bin;
                    minSpaceLeft = spaceLeft;
                }
            }

            if (bestFitBin != null) {
                bestFitBin.addItem(item);
                items.remove(item);
            } else {
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
    }


    private static Set<Item> extractItems(List<Bin> binsForMutation) {
        Set<Item> extractedItems = new HashSet<>();
        for (Bin bin : binsForMutation) {
            extractedItems.addAll(bin.items);
            bin.items.clear();  // Clear the items from the bin after extraction
        }
        return extractedItems;
    }

    private static Set<Item> getAllItems(Individual parent1, Individual parent2) {
        Set<Item> allItems = new HashSet<>();
        for (Bin bin : parent1.bins) {
            allItems.addAll(bin.items);
        }
        for (Bin bin : parent2.bins) {
            allItems.addAll(bin.items);
        }
        return allItems;
    }


    private static void redistributeItems(Individual individual, Set<Item> items, int binCapacity) {
        List<Item> itemList = new ArrayList<>(items);
        Collections.shuffle(itemList);  // Shuffle to introduce randomness
        for (Item item : itemList) {
            addOrNewBin(individual.bins, item, binCapacity);  // Add item to a bin or create a new one
        }
    }

    private static void applyTwoPhaseProcedure(List<Bin> bins, Set<Item> items, int binCapacity) {
        // First phase: Try to fit items using a best-fit approach
        for (Item item : new ArrayList<>(items)) {
            Bin bestFitBin = findBestFitBin(bins, item, binCapacity);
            if (bestFitBin != null) {
                bestFitBin.addItem(item);
                items.remove(item);
            }
        }

        // Second phase: Any remaining items should be added to new bins
        for (Item item : items) {
            Bin newBin = new Bin();
            newBin.addItem(item);
            bins.add(newBin);
        }
    }

    private static void replaceWorstWithOffspring(List<Individual> population, Individual offspring) {
        // Sort population based on fitness and replace the least fit with offspring
        population.sort(Comparator.comparingInt(Individual::getFitness));  // Assume fitness is already calculated to reflect quality
        if (offspring.getFitness() > population.get(0).getFitness()) {  // Assuming higher fitness is better
            population.set(0, offspring);  // Replace the worst individual
        }
    }



    private static void applyFirstFitProcedure(List<Bin> bins, Set<Item> items, int binCapacity) {
        for (Item item : items) {
            boolean placed = false;
            for (Bin bin : bins) {
                if (bin.canAddItem(item, binCapacity)) {
                    bin.addItem(item);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
    }

    private static void applyFirstFitDecreasing(List<Bin> bins, Set<Item> items, int binCapacity) {
        // Sort items by decreasing size
        List<Item> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(Item::getSize).reversed());

        // Apply First-Fit logic
        for (Item item : sortedItems) {
            addOrNewBin(bins, item, binCapacity);
        }
    }

    private static void addOrNewBin(List<Bin> bins, Item item, int binCapacity) {
        for (Bin bin : bins) {
            if (bin.canAddItem(item, binCapacity)) {
                bin.addItem(item);
                return;
            }
        }
        Bin newBin = new Bin();
        newBin.addItem(item);
        bins.add(newBin);
    }



    private static List<Item> findBestReplacement(Bin bin, Set<Item> tempSetT, int binCapacity) {
        List<Item> bestCombination = new ArrayList<>();
        int currentSize = 0;

        for (Item item : tempSetT) {
            if (currentSize + item.size <= binCapacity) {
                bestCombination.add(item);
                currentSize += item.size;
            }
        }

        return bestCombination;
    }

    private static void updateTempSetT(Set<Item> tempSetT, List<Item> newItems, Bin bin) {
        tempSetT.removeAll(newItems);
        tempSetT.addAll(bin.items);
    }

    private static void applyModifiedBestFitSlack(List<Bin> bins, Set<Item> items, int binCapacity) {
        for (Item item : new HashSet<>(items)) {
            Bin bestFitBin = findBestFitBin(bins, item, binCapacity);
            if (bestFitBin != null) {
                bestFitBin.addItem(item);
                items.remove(item);
            } else {
                // If no suitable bin found, create a new bin
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
    }

    private static Bin findBestFitBin(List<Bin> bins, Item item, int binCapacity) {
        Bin bestFitBin = null;
        int minSpaceLeft = Integer.MAX_VALUE;

        for (Bin bin : bins) {
            int spaceLeft = binCapacity - bin.getCurrentSize();
            if (spaceLeft >= item.size && spaceLeft < minSpaceLeft) {
                bestFitBin = bin;
                minSpaceLeft = spaceLeft;
            }
        }
        return bestFitBin;
    }


    private static void mutate(Individual individual, int binCapacity) {
        List<Bin> binsForMutation = selectRandomBinsForMutation(individual, 2);
        Set<Item> temporaryItems = extractItems(binsForMutation);

        Collections.shuffle(new ArrayList<>(temporaryItems));  // Introduce randomness
        redistributeItems(individual, temporaryItems, binCapacity);

        // Attempt local optimization after mutation
        applyBestFitHeuristic(individual.bins, temporaryItems, binCapacity);
    }



    private static void handleMissingItems(Individual individual, Set<Item> allItems, int binCapacity) {
        Set<Item> placedItems = individual.bins.stream()
                .flatMap(bin -> bin.items.stream())
                .collect(Collectors.toSet());
        allItems.removeAll(placedItems); // Remove items that are already placed

        // Place remaining items in new or existing bins
        for (Item item : allItems) {
            boolean placed = false;
            for (Bin bin : individual.bins) {
                if (bin.canAddItem(item, binCapacity)) {
                    bin.addItem(item);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                Bin newBin = new Bin();
                newBin.addItem(item);
                individual.bins.add(newBin);
            }
        }
    }

    private static Bin findOriginalBinForItem(Item item, Individual individual) {
        // Find the original bin where this item was located before the mutation
        return individual.bins.stream()
                .filter(bin -> bin.items.contains(item))
                .findFirst()
                .orElseGet(() -> {
                    // If the item is not found in any bin, create a new bin for it
                    Bin newBin = new Bin();
                    newBin.addItem(item);
                    individual.bins.add(newBin);
                    return newBin;
                });
    }

    private static List<Bin> selectRandomBinsForMutation(Individual individual, int numBins) {
        List<Bin> bins = new ArrayList<>(individual.bins);
        Collections.shuffle(bins);
        return bins.subList(0, Math.min(numBins, bins.size()));
    }

    private static Map<String, List<Item>> loadItems(String fileName) throws FileNotFoundException {
        Map<String, List<Item>> testCases = new HashMap<>();
        File file = new File(fileName);

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String testName = scanner.nextLine().trim(); // Test case name
                int numberOfItems = Integer.parseInt(scanner.nextLine().trim());
                int binCapacity = Integer.parseInt(scanner.nextLine().trim()); // Bin capacity

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

    private static Individual tournamentSelection(List<Individual> population) {
        List<Individual> tournament = new ArrayList<>();
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual randomIndividual = population.get(random.nextInt(population.size()));
            tournament.add(randomIndividual);
        }
        return Collections.max(tournament, Comparator.comparing(Individual::getFitness));
        // This line returns the best individual from the tournament, i.e., the one with the highest fitness value.
        //Collections.max is a utility method that finds the maximum element of the given collection, according to the order induced by the specified comparator.
        // In this case, the comparator is based on the fitness of the individuals (Individual::getFitness)
    }

    private static void selectionUsingMGG(List<Individual> population, int offspringSize, int binCapacity) {
        while (offspringSize > 0) {
            // Select two parents randomly
            Individual parent1 = population.get(random.nextInt(population.size()));
            Individual parent2 = population.get(random.nextInt(population.size()));

            // Generate offspring
            Individual offspring = crossover(parent1, parent2, binCapacity);
            mutate(offspring, binCapacity);

            // Replace worst individuals with new offspring if better
            replaceWorstWithOffspring(population, offspring);
            offspringSize--;
        }
    }



    // Methods for elitism
    private static List<Individual> getElites(List<Individual> population, int numberOfElites) {
        return population.stream()
                .sorted(Comparator.comparing(Individual::getFitness).reversed())
                .limit(numberOfElites)
                .collect(Collectors.toList());
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

    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.currentTimeMillis();
        System.out.println("Program started");

        Map<String, List<Item>> testCases = loadItems("src/BPP.txt");

        for (Map.Entry<String, List<Item>> entry : testCases.entrySet()) {
            String testCaseName = entry.getKey();
            List<Item> allItems = new ArrayList<>(entry.getValue()); // Define allItems
            List<Item> items = entry.getValue();

            printColored("Solving test case: " + testCaseName, ANSI_YELLOW);
            System.out.println("Items loaded: " + items.size());

            List<Individual> population = generateInitialPopulation(items, BIN_CAPACITY, POPULATION_SIZE);
            System.out.println("Initial population generated");
            validateAndLogBinWeights(population, allItems, "Initial Population Generation");

            for (int i = 0; i < GENERATIONS; i++) {
                // Apply MGG
                selectionUsingMGG(population, OFFSPRING_SIZE, BIN_CAPACITY);

                // Apply mutation to a portion of the population
                for (int j = 0; j < population.size(); j++) {
                    if (random.nextDouble() < MUTATION_RATE) {
                        mutate(population.get(j), BIN_CAPACITY);
                        validateAndLogBinWeights(population, allItems, "Crossover in Generation " + i);  // Validation after crossover
                    }
                }

                // Generation-wise logging (keeping your existing logging)
                if (i % 100 == 0) {
                    Individual bestIndividual = findBestSolution(population);
                    System.out.println("Generation " + i + ", Best Fitness: " + bestIndividual.getFitness());
                    validateAndLogBinWeights(population, allItems, "Mutation in Generation " + i);  // Validation after mutation
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
            System.out.println("Total weight of all items in " + testCaseName + ": " + totalWeightOfAllItems + ANSI_BLUE);

            // Check if total weights match
            if(totalWeightOfAllItems != totalWeightInBins){
                System.out.println("Warning: There is a discrepancy in the total weights for " + testCaseName + ANSI_RED);
            }
            long endTime = System.currentTimeMillis(); // End timer
            double totalTime = endTime - startTime; // Calculate total time

            System.out.println("Execution Time: " + totalTime/1000 + " seconds");

            System.out.println("\n");
        }
    }

    private static void validateAndLogBinWeights(List<Individual> population, List<Item> allItems, String stage) {
        int totalItemWeight = allItems.stream().mapToInt(Item::getSize).sum();
        int totalBinWeight = population.stream()
                .flatMap(individual -> individual.bins.stream())
                .flatMap(bin -> bin.items.stream())
                .distinct()
                .mapToInt(Item::getSize)
                .sum();

        if (totalItemWeight != totalBinWeight) {
            System.out.println(ANSI_RED + "Warning: Weight discrepancy detected after " + stage + ". " +
                    "Total item weight: " + totalItemWeight + ", Total bin weight: " + totalBinWeight + ANSI_RESET);

        }
    }

    // ANSI color code declarations
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";

    // Method for printing colored text to the console
    private static void printColored(String text, String colorCode) {
        System.out.println(colorCode + text + ANSI_RESET);
    }
}