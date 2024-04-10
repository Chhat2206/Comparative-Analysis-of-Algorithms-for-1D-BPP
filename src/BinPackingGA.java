import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BinPackingGA {
    // Constants
    private static final int POPULATION_SIZE = 1000;
    // n set between 50 and 1000. Smaller problems might work well with 50-100, while larger,
    // more complex problems may require more diverse genetic pool but increase computation time.
    // A balance must be found between diversity and computational efficiency.

    private static final int GENERATIONS = 1000;
    // Can range from tens to thousands,
    // More generations allow more time for the algorithm to evolve solutions but increase computation time.
    // Stop the algorithm if no significant improvement is observed over several generations.
    private static final Random random = new Random(); // Random number generator
    private static final int BIN_CAPACITY = 10000; // The capacity of each bin
    private static final int OFFSPRING_SIZE = 500;
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

    private static List<Individual> generateInitialPopulation(List<Item> items, int binCapacity, int populationSize) {
        // Sort items in non-increasing order
        items.sort((item1, item2) -> item2.size - item1.size);

        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
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
        // Step 1: Inherit bins from both parents
        List<Bin> offspringBins = inheritBinsFromParents(parent1, parent2);

        // Step 2: Local optimization - Repack bins using FF and MBS’
        repackBinsUsingFFAndMBS(offspringBins, binCapacity);

        return new Individual(offspringBins);
    }

    private static void repackBinsUsingFFAndMBS(List<Bin> bins, int binCapacity) {
        List<Item> allItems = flattenItems(bins);
        bins.clear(); // Clear existing bins for repacking

        // First-Fit (FF)
        for (Item item : allItems) {
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

        // Modified Best-Fit Slack (MBS’)
        List<Item> remainingItems = flattenItems(bins);
        bins.clear(); // Clear bins for final packing

        for (Item item : remainingItems) {
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
            } else {
                Bin newBin = new Bin();
                newBin.addItem(item);
                bins.add(newBin);
            }
        }
    }

    private static List<Item> flattenItems(List<Bin> bins) {
        List<Item> items = new ArrayList<>();
        for (Bin bin : bins) {
            items.addAll(bin.items);
        }
        return items;
    }

    private static List<Bin> inheritBinsFromParents(Individual parent1, Individual parent2) {
        List<Bin> inheritedBins = new ArrayList<>();
        // Implement logic to inherit bins from parents (as per your current approach or a modified approach)
        return inheritedBins;
    }

    // mutation function goes wild at 100 population size and 50 generation size
    private static void mutate(Individual individual, int binCapacity) {
        // Step 1: Select a few bins at random
        List<Bin> selectedBins = selectRandomBinsForMutation(individual, 2); // Selecting 2 bins for mutation
        Set<Item> temporaryItems = new HashSet<>();

        // Extract items from selected bins
        for (Bin bin : selectedBins) {
            temporaryItems.addAll(bin.items);
            bin.items.clear(); // Clear the items in the selected bin
        }

        // Step 2: Attempt to re-distribute items from temporary set back into bins
        redistributeItems(individual, temporaryItems, binCapacity);

        // Step 3: Reinsert any remaining items using FF-like strategy
        for (Item item : temporaryItems) {
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

    private static void redistributeItems(Individual individual, Set<Item> items, int binCapacity) {
        for (Item item : new HashSet<>(items)) { // Use a new HashSet to avoid concurrent modification
            Bin bestFitBin = null;
            int minSpaceLeft = Integer.MAX_VALUE;

            // Find the bin that would have the least space left after adding this item
            for (Bin bin : individual.bins) {
                int spaceLeft = binCapacity - bin.getCurrentSize();
                if (spaceLeft >= item.size && spaceLeft < minSpaceLeft) {
                    bestFitBin = bin;
                    minSpaceLeft = spaceLeft;
                }
            }

            // Place the item in the best-fit bin, if found
            if (bestFitBin != null) {
                bestFitBin.addItem(item);
                items.remove(item); // Remove the item from the temporary set
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
        Random random = new Random();

        // Select elites for preservation
        List<Individual> elites = getElites(population, ELITISM_SIZE);

        // Pick two parents randomly
        Individual parent1 = population.get(random.nextInt(population.size()));
        Individual parent2 = population.get(random.nextInt(population.size()));

        // Generate offspring
        List<Individual> offspring = IntStream.range(0, offspringSize)
                .mapToObj(i -> {
                    if (random.nextDouble() < CROSSOVER_PROBABILITY) {
                        // Perform crossover
                        return crossover(parent1, parent2, binCapacity);
                    } else {
                        // No crossover, just copy one of the parents
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
                .distinct() // Avoid duplicating elites
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

    private static void includeElites(List<Individual> population, List<Individual> elites) {
        for (Individual elite : elites) {
            population.remove(elite);
            population.add(elite);
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
            return -bins.size();
        }

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
                // Apply MGG
                selectionUsingMGG(population, OFFSPRING_SIZE, BIN_CAPACITY);

                // Apply mutation to a portion of the population
                for (int j = 0; j < population.size(); j++) {
                    if (random.nextDouble() < MUTATION_RATE) {
                        mutate(population.get(j), BIN_CAPACITY);
                    }
                }


                // Generation-wise logging (keeping your existing logging)
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

}