import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class BinPackingGA {
    private static int currentGeneration = 0;
    // Constants
    private static final int POPULATION_SIZE = 100;
    private static final int GENERATIONS = 1000;
    private static final Random random = new Random();
    private static final int BIN_CAPACITY = 10000;

    private static final int OFFSPRING_SIZE = 250;
    // A larger population size allows for more exploration but also increases computational complexity. 500 individuals is a reasonable size for many problems

    private static final double MUTATION_RATE = 0.01;
    // the probability of random changes in individual genes.
    // Generally low, often between 0.001 and 0.01.
    // A higher rate can prevent premature convergence to local optima by introducing diversity,
    // but too high a rate can turn the search into a random walk.


    // This method is responsible for creating the initial population of solutions for the genetic algorithm.
    // Creates an empty list to hold the population of individuals.
    // Loops over the number of individuals to be generated (specified by populationSize).
    // Shuffles the items - It takes a list of original items, creates a copy, and then shuffles this copy randomly. This step is crucial as it introduces randomness in the initial solutions, promoting genetic diversity from the outset.
    // Generates an individual - For each shuffled list of items, it calls the generateIndividualUsingFF method to create a new individual based on the First-Fit heuristic.
    // Generate the initial population of solutions using both random shuffling and heuristic-based approaches
    public static List<Individual> generateInitialPopulation(List<Item> originalItems, int binCapacity, int populationSize) {
        List<Individual> population = new ArrayList<>();
        int heuristicPopulation = populationSize / 3;  // Divide population into thirds

        // First third: Random shuffling and First-Fit
        for (int i = 0; i < heuristicPopulation; i++) {
            List<Item> shuffledItems = new ArrayList<>(originalItems);
            Collections.shuffle(shuffledItems, random);
            population.add(new Individual(applyFirstFit(shuffledItems, binCapacity)));
        }

        // Second third: Heuristic approach using Modified Best-Fit Slack (MBS')
        List<Item> sortedItemsDesc = new ArrayList<>(originalItems);
        sortedItemsDesc.sort(Comparator.comparing(Item::getSize).reversed());
        for (int i = heuristicPopulation; i < 2 * heuristicPopulation; i++) {
            population.add(new Individual(applyModifiedBestFitSlack(sortedItemsDesc, binCapacity)));
        }

        // Final third: Heuristic approach using Minimum Bin Slack (MBS)
        for (int i = 2 * heuristicPopulation; i < populationSize; i++) {
            population.add(new Individual(applyMinimumBinSlack(sortedItemsDesc, binCapacity)));
        }

        return population;
    }

    private static List<Bin> applyMinimumBinSlack(List<Item> items, int binCapacity) {
        List<Bin> bins = new ArrayList<>();
        for (Item item : items) {
            Bin bestBin = null;
            int minSlack = Integer.MAX_VALUE;

            for (Bin bin : bins) {
                int currentSlack = binCapacity - bin.getCurrentSize();
                if (currentSlack >= item.getSize() && currentSlack - item.getSize() < minSlack) {
                    bestBin = bin;
                    minSlack = currentSlack - item.getSize();
                }
            }

            if (bestBin != null) {
                bestBin.addItem(item);
            } else {
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
        return bins;
    }


    // This method implements the First-Fit algorithm, which is used to place items into bins. Here's the step-by-step process:
    // Initializes an empty list of bins.
    // Iterates over each item in the shuffled list:
    //   It checks if there is an existing bin that can accommodate the item without exceeding the bin's capacity.
    //   If such a bin exists, the item is added to the first suitable bin found.
    //   If no suitable bin is found, a new bin is created, and the item is added to this new bin.
    // Returns an individual - An individual, represented by the list of bins (each containing a set of items), is returned. This individual is a solution to the bin packing problem under the First-Fit strategy.
    private static List<Bin> applyFirstFit(List<Item> items, int binCapacity) {
        List<Bin> bins = new ArrayList<>();
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
        return bins;
    }

    // Modified Best-Fit Slack Heuristic
    private static List<Bin> applyModifiedBestFitSlack(List<Item> items, int binCapacity) {
        List<Bin> bins = new ArrayList<>();
        for (Item item : items) {
            Bin bestFitBin = null;
            int minSlack = Integer.MAX_VALUE;

            for (Bin bin : bins) {
                int currentSlack = binCapacity - bin.getCurrentSize();
                if (currentSlack >= item.getSize() && currentSlack - item.getSize() < minSlack) {
                    bestFitBin = bin;
                    minSlack = currentSlack - item.getSize();
                }
            }

            if (bestFitBin != null) {
                bestFitBin.addItem(item);
            } else {
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
        return bins;
    }

    private static Individual crossover(Individual parent1, Individual parent2, int binCapacity) {
        // New offspring individual with no bins initially
        Individual offspring = new Individual(new ArrayList<>());

        // S1: Randomly select a subset of bins from parent1
        Set<Bin> S1 = selectRandomBins(parent1);
        Set<Item> itemsInS1 = new HashSet<>();
        for (Bin bin : S1) {
            offspring.getBins().add(new Bin(new ArrayList<>(bin.getItems())));
            itemsInS1.addAll(bin.getItems());
        }

        // S2: Select bins from parent2 that do not contain any items from S1
        Set<Bin> S2 = new HashSet<>();
        for (Bin bin : parent2.getBins()) {
            if (Collections.disjoint(bin.getItems(), itemsInS1)) {
                offspring.getBins().add(new Bin(new ArrayList<>(bin.getItems())));
                S2.add(bin);
            }
        }

        // T: Create a temporary set containing items not in S1 or S2
        Set<Item> itemsInS2 = S2.stream()
                .flatMap(bin -> bin.getItems().stream())
                .collect(Collectors.toSet());
        Set<Item> itemsNotInOffspring = getAllItemsFromBothParents(parent1, parent2);
        itemsNotInOffspring.removeAll(itemsInS1);
        itemsNotInOffspring.removeAll(itemsInS2);

        // S3: Try to pack remaining items into the current bins optimally
        reintegrateItemsDynamically(offspring.getBins(), itemsNotInOffspring, binCapacity);

        return offspring;
    }

    private static Set<Bin> selectRandomBins(Individual parent) {
        List<Bin> bins = new ArrayList<>(parent.getBins());
        Collections.shuffle(bins, random);
        int numBins = bins.size() / 2; // Select about half of the bins
        return new HashSet<>(bins.subList(0, numBins));
    }

    private static Set<Item> getAllItemsFromBothParents(Individual parent1, Individual parent2) {
        Set<Item> items = new HashSet<>();
        parent1.getBins().forEach(bin -> items.addAll(bin.getItems()));
        parent2.getBins().forEach(bin -> items.addAll(bin.getItems()));
        return items;
    }

    private static void reintegrateItemsDynamically(List<Bin> bins, Set<Item> remainingItems, int binCapacity) {
        // You may use any heuristic here, e.g., Best Fit, First Fit, etc.
        for (Item item : remainingItems) {
            if (!placeItemInExistingBins(bins, item, binCapacity)) {
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
    }

    private static boolean placeItemInExistingBins(List<Bin> bins, Item item, int binCapacity) {
        for (Bin bin : bins) {
            if (bin.canAddItem(item, binCapacity)) {
                bin.addItem(item);
                return true;
            }
        }
        return false;
    }


    // Method to optimize filling of bins using a detailed replacement strategy
    private static void optimizeBinFilling(List<Bin> bins, List<Item> allItems) {
        for (Bin bin : bins) {
            int currentBinCapacity = bin.getCurrentSize();
            int slack = BIN_CAPACITY - currentBinCapacity;

            List<Item> itemsInBin = new ArrayList<>(bin.getItems());
            List<Item> itemsToRemove = new ArrayList<>();
            List<Item> itemsToAdd = new ArrayList<>();

            // Iterate over possible replacements and attempt to optimize bin fill
            for (Item original : itemsInBin) {
                int remainingSlack = BIN_CAPACITY - (currentBinCapacity - original.getSize());
                Item bestReplacement = findBestReplacement(original, remainingSlack, allItems);
                if (bestReplacement != null && !itemsInBin.contains(bestReplacement)) {
                    itemsToRemove.add(original);
                    itemsToAdd.add(bestReplacement);
                    currentBinCapacity = currentBinCapacity - original.getSize() + bestReplacement.getSize();
                }
            }

            // Apply the replacements
            bin.getItems().removeAll(itemsToRemove);
            bin.getItems().addAll(itemsToAdd);

            // Check and handle any discrepancies
            if (!itemsCorrectlyReplaced(itemsToRemove, itemsToAdd)) {
                throw new IllegalStateException("Error in bin optimization: Item replacement mismatch.");
            }
        }
    }

    // Function to find the best replacement for an item considering all available items
    private static Item findBestReplacement(Item original, int remainingSlack, List<Item> candidateItems) {
        Item bestFit = null;
        int minSlack = Integer.MAX_VALUE;

        for (Item candidate : candidateItems) {
            if (candidate.getSize() > original.getSize() && candidate.getSize() <= remainingSlack) {
                int slackDifference = remainingSlack - candidate.getSize();
                if (slackDifference < minSlack) {
                    minSlack = slackDifference;
                    bestFit = candidate;
                }
            }
        }

        return bestFit;
    }

    // Function to ensure that the replacement process is correct
    private static boolean itemsCorrectlyReplaced(List<Item> removed, List<Item> added) {
        if (removed.size() != added.size()) {
            return false;
        }

        int removedWeight = removed.stream().mapToInt(Item::getSize).sum();
        int addedWeight = added.stream().mapToInt(Item::getSize).sum();
        return removedWeight <= addedWeight;
    }

    private static Set<Item> extractItems(List<Bin> binsForMutation) {
        Set<Item> extractedItems = new HashSet<>();
        for (Bin bin : binsForMutation) {
            extractedItems.addAll(bin.items);
            bin.items.clear();  // Clear the items from the bin after extraction
        }
        return extractedItems;
    }

    private static void replaceWorstWithOffspring(List<Individual> population, Individual offspring) {
        // Sort population based on fitness and replace the least fit with offspring
        population.sort(Comparator.comparingInt(Individual::getFitness));  // Assume fitness is already calculated to reflect quality
        if (offspring.getFitness() > population.get(0).getFitness()) {  // Assuming higher fitness is better
            population.set(0, offspring);  // Replace the worst individual
        }
    }

    private static void mutate(Individual individual, int binCapacity) {
        // Select a subset of bins randomly for mutation
        List<Bin> binsForMutation = selectRandomBinsForMutation(individual, random.nextInt(2) + 2); // Randomly 2 or 3 bins

        // Extract all items from these bins and clear the bins
        Set<Item> extractedItems = extractItems(binsForMutation);
        individual.bins.removeAll(binsForMutation); // Remove the selected bins from the individual

        // Reintegrate extracted items using a heuristic
        reintegrateItemsUsingHeuristic(individual, extractedItems, binCapacity);
    }

    private static void reintegrateItemsUsingHeuristic(Individual individual, Set<Item> items, int binCapacity) {
        // Using Modified Best-Fit Slack (MBS) heuristic
        for (Item item : items) {
            Bin bestFitBin = null;
            int minSlack = Integer.MAX_VALUE;

            // Find the bin with the minimum slack that can still accommodate the item
            for (Bin bin : individual.bins) {
                int currentSlack = binCapacity - bin.getCurrentSize();
                if (currentSlack >= item.getSize() && currentSlack - item.getSize() < minSlack) {
                    bestFitBin = bin;
                    minSlack = currentSlack - item.getSize();
                }
            }

            // Place the item in the best-fit bin according to the slack or create a new bin if no suitable bin is found
            if (bestFitBin != null) {
                bestFitBin.addItem(item);
            } else {
                Bin newBin = new Bin();
                newBin.addItem(item);
                individual.bins.add(newBin);
            }
        }
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

    private static Individual findBestSolution(List<Individual> population) {
        return Collections.max(population, Comparator.comparing(Individual::getFitness));
    }

    static class Individual {
        List<Bin> bins;

        public Individual(List<Bin> bins) {
            this.bins = bins;
        }

        public int getFitness() {
            // Fitness is the negative of the number of bins (we want to minimize the number of bins)
            return -bins.size();
            //return calculateFitness();
        }

        public double calculateFitness() {
            double loadSumSquared = bins.stream()
                    .mapToDouble(bin -> Math.pow((double)bin.getCurrentSize() / BIN_CAPACITY * 100, 2))
                    .sum();
            double loadSumNormalized = loadSumSquared / 10000;
            return loadSumNormalized / bins.size();
        }


        public List<Bin> getBins() {
            return bins;
        }
    }

    private static int totalItemWeight(List<Item> allItems) {
        return allItems.stream().mapToInt(Item::getSize).sum();
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
            for (Individual individual : population) {
                optimizeBinFilling(individual.getBins(), allItems);
            }
            System.out.println("Initial population generated");
            validateAndLogBinWeights(population, allItems, "Initial Population Generation");

            for (int i = 0; i < GENERATIONS; i++) {
                // Apply MGG
                selectionUsingMGG(population, OFFSPRING_SIZE, BIN_CAPACITY);
                currentGeneration = i;

//                for (int i = 0; i < GENERATIONS; i++) {
//                    selectionUsingMGG(population, OFFSPRING_SIZE, BIN_CAPACITY);
//                    if (i % 100 == 0) {  // Periodically optimize the entire population
//                        for (Individual individual : population) {
//                            optimizeBinFilling(individual.getBins());
//                        }
//                        System.out.println("Performed optimization at generation " + i);
//                    }
//                }


                // Track and log metrics after selection
                double avgFill = averageFillPercentage(population, BIN_CAPACITY);
                int diversity = calculateDiversity(population);
                int bestFitness = findBestSolution(population).getFitness();

                System.out.println("Generation " + currentGeneration + ": Avg Fill = " + avgFill + "%, Diversity = " + diversity + ", Best Fitness = " + bestFitness);

                int totalItemWeight = totalItemWeight(allItems);
                // Check if fitness equals the maximum possible number of bins
                if (bestFitness == -((totalItemWeight / BIN_CAPACITY)+1)) {
                    System.out.println("Stopping criteria met. Fitness equals the minimum possible number of bins.");
                    break; // Exit the loop if the stopping criteria is met
                }

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

    private static double averageFillPercentage(List<Individual> population, int binCapacity) {
        double totalFill = 0;
        int totalBins = 0;
        for (Individual individual : population) {
            for (Bin bin : individual.bins) {
                totalFill += ((double) bin.getCurrentSize() / binCapacity);
                totalBins++;
            }
        }
        return totalBins > 0 ? (totalFill / totalBins) * 100 : 0;
    }
    private static int calculateDiversity(List<Individual> population) {
        Set<String> uniqueConfigurations = new HashSet<>();
        for (Individual individual : population) {
            StringBuilder config = new StringBuilder();
            for (Bin bin : individual.bins) {
                List<Integer> items = bin.items.stream().map(Item::getSize).sorted().collect(Collectors.toList());
                config.append(items.toString());
            }
            uniqueConfigurations.add(config.toString());
        }
        return uniqueConfigurations.size();
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