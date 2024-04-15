import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Individual {
    List<Bin> bins;
    private double fitness = -1;

    public Individual(List<Bin> bins) {
        this.bins = bins;
    }

    public double getFitness() {
        return fitness;
    }

    public void calculateFitness(int optimalBinCount, int binCapacity) {
        int currentBinCount = bins.size();
        double binsUsedPenalty = currentBinCount / (double) optimalBinCount;

        double totalWaste = 0;
        double totalOverflow = 0;
        for (Bin bin : bins) {
            int totalBinItemsSize = bin.items.stream().mapToInt(Item::getSize).sum();
            if (totalBinItemsSize > binCapacity) {
                totalOverflow += totalBinItemsSize - binCapacity;
            } else {
                totalWaste += binCapacity - totalBinItemsSize;
            }
        }

        double wastePenalty = totalWaste / (binCapacity * currentBinCount);
        double overflowPenalty = totalOverflow / (binCapacity * currentBinCount);

        double lambda1 = 0.5; // Penalty factor for using more bins than optimal
        double lambda2 = 0.3; // Penalty factor for wasted space
        double lambda3 = 0.2; // Penalty factor for overflow

        this.fitness = 1 / (1 + lambda1 * binsUsedPenalty + lambda2 * wastePenalty + lambda3 * overflowPenalty);
    }
}


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

    private static final double CROSSOVER_PROBABILITY = 0.6;
    //The probability of crossover (mating) between individuals.
    //Typically set between 0.6 and 0.9.
    //Tweaking: Higher rates increase genetic diversity but might disrupt convergence.

    // Adjust initial population generation method

    private static final int TOURNAMENT_SIZE = 5;
    private static final int ELITISM_SIZE = 2;

    private static List<Individual> generateInitialPopulation(List<Item> originalItems, int binCapacity, int populationSize) {
        List<Individual> population = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            List<Item> items = new ArrayList<>(originalItems);
            Collections.shuffle(items);  // Randomly shuffle the items
            population.add(generateIndividualUsingFF(items, binCapacity));
        }

        return population;
    }


    // First-Fit procedure
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
        Set<Item> tempSetT = new HashSet<>();

        // Step 1: Select bins from parent1
        for (Bin bin : parent1.bins) {
            if (random.nextBoolean()) {
                offspringBins.add(new Bin(new ArrayList<>(bin.items)));
                tempSetT.addAll(bin.items);
            }
        }

        // Step 2: Copy non-conflicting bins from parent2
        for (Bin bin : parent2.bins) {
            if (Collections.disjoint(bin.items, tempSetT)) {
                offspringBins.add(new Bin(new ArrayList<>(bin.items)));
            } else {
                tempSetT.addAll(bin.items);
            }
        }

        // Step 3: Replacement procedure
        replaceItemsInOffspringBins(offspringBins, tempSetT, binCapacity);

        // Step 4: First-Fit for remaining items in T
        applyFirstFitProcedure(offspringBins, tempSetT, binCapacity);

        // Step 5: Modified Best-Fit Slack (assuming its logic is similar to Best-Fit)
        applyModifiedBestFitSlack(offspringBins, tempSetT, binCapacity);

        return new Individual(offspringBins);
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

    private static void replaceItemsInOffspringBins(List<Bin> offspringBins, Set<Item> tempSetT, int binCapacity) {
        for (Bin bin : offspringBins) {
            // Find the best replacement combination from tempSetT
            List<Item> bestReplacement = findBestReplacement(bin, tempSetT, binCapacity);

            // Perform the replacement if a better combination is found
            if (!bestReplacement.isEmpty()) {
                bin.items.clear();
                bin.items.addAll(bestReplacement);

                // Update tempSetT by removing used items and adding back items removed from the bin
                updateTempSetT(tempSetT, bestReplacement, bin);
            }
        }
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
        // Select bins randomly for mutation and extract items
        List<Bin> selectedBins = selectRandomBinsForMutation(individual, 2);
        Set<Item> temporaryItems = new HashSet<>();
        for (Bin bin : selectedBins) {
            temporaryItems.addAll(bin.items);
            bin.items.clear();
        }

        // Shuffle and redistribute items
        List<Item> shuffledItems = new ArrayList<>(temporaryItems);
        Collections.shuffle(shuffledItems);
        for (Item item : shuffledItems) {
            boolean placed = false;
            for (Bin bin : individual.bins) {
                if (bin.canAddItem(item, binCapacity)) {
                    bin.addItem(item);
                    placed = true;
                    break;
                }
            }
            // Return items to their original bin if not placed
            if (!placed) {
                findOriginalBinForItem(item, individual).addItem(item);
            }
        }

        // Check if all items are placed and handle any missing items
        handleMissingItems(individual, temporaryItems, binCapacity);
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
        // Select elites for preservation
        List<Individual> elites = getElites(population, ELITISM_SIZE);

        // Generate offspring
        List<Individual> offspring = IntStream.range(0, offspringSize)
                .mapToObj(i -> {
                    // Use tournament selection to pick parents
                    Individual parent1 = tournamentSelection(population);
                    Individual parent2 = tournamentSelection(population);

                    // Perform crossover or simply copy a parent based on probability
                    if (random.nextDouble() < CROSSOVER_PROBABILITY) {
                        return crossover(parent1, parent2, binCapacity);
                    } else {
                        return random.nextBoolean() ? new Individual(new ArrayList<>(parent1.bins))
                                : new Individual(new ArrayList<>(parent2.bins));
                    }
                })
                .collect(Collectors.toList());

        // Apply mutation to the offspring
        offspring.forEach(individual -> {
            if (random.nextDouble() < MUTATION_RATE) {
                mutate(individual, binCapacity);
            }
        });

        // Combine the current population and offspring
        List<Individual> combinedGroup = new ArrayList<>(population);
        combinedGroup.addAll(offspring);

        // Sort by fitness and replace the population with the best individuals, preserving elites
        combinedGroup.sort(Comparator.comparing(Individual::getFitness));
        population.clear();
        population.addAll(elites);
        population.addAll(combinedGroup.stream()
                .limit(POPULATION_SIZE - ELITISM_SIZE)
                .collect(Collectors.toList()));
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

    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.currentTimeMillis();
        System.out.println("Program started");

        Map<String, List<Item>> testCases = loadItems("src/BPP.txt");

        for (Map.Entry<String, List<Item>> entry : testCases.entrySet()) {
            String testCaseName = entry.getKey();
            List<Item> allItems = new ArrayList<>(entry.getValue()); // Define allItems
            List<Item> items = entry.getValue();

            System.out.println("Solving test case: " + testCaseName);
            System.out.println("Items loaded: " + items.size());

            int totalWeightOfAllItems = items.stream().mapToInt(Item::getSize).sum();
            System.out.println("Total weight of all items in " + testCaseName + ": " + totalWeightOfAllItems + ANSI_BLUE);
            int optimalBinCount = (int) Math.ceil((double) totalWeightOfAllItems / BIN_CAPACITY);
            System.out.println("Optimal bin count for " + testCaseName + " based on total weight: " + optimalBinCount);

            List<Individual> population = generateInitialPopulation(items, BIN_CAPACITY, POPULATION_SIZE);
            for (Individual individual : population) {
                individual.calculateFitness(optimalBinCount, BIN_CAPACITY);
            }

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
                if (i % 2 == 0) {
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