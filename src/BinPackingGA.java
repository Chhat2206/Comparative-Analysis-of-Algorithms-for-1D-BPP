import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class BinPackingGA {
    private static final int POPULATION_SIZE = 100;
    private static final int GENERATIONS = 800;
    private static final Random random = new Random();
    private static final int BIN_CAPACITY = 10000;
    private static final int FAMILY_SIZE = 5;
    private static final int OFFSPRING_SIZE = 20;

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

    private static void applyMGG(List<Individual> population, int offspringSize) {
        // Step 1: Select two parents
        Individual parent1 = population.get(random.nextInt(population.size()));
        Individual parent2 = population.get(random.nextInt(population.size()));

        // Step 2: Generate offspring
        List<Individual> offspring = generateOffspring(Arrays.asList(parent1, parent2), offspringSize);

        // Evaluate and select for replacement
        List<Individual> candidates = new ArrayList<>(offspring);
        candidates.add(parent1);
        candidates.add(parent2);

        // Sort based on fitness
        candidates.sort(Comparator.comparing(Individual::getFitness));

        // Step 3: Replace two worst individuals in the population with the best two offspring
        replaceWorstIndividuals(population, candidates.subList(0, 2));
    }

    private static void replaceWorstIndividuals(List<Individual> population, List<Individual> newIndividuals) {
        population.sort(Comparator.comparing(Individual::getFitness).reversed());
        for (int i = 0; i < newIndividuals.size(); i++) {
            population.set(population.size() - 1 - i, newIndividuals.get(i));
        }
    }

    private static List<Individual> generateOffspring(List<Individual> family, int offspringSize) {
        List<Individual> offspring = new ArrayList<>();
        for (int i = 0; i < offspringSize; i++) {
            Individual parent1 = family.get(random.nextInt(family.size()));
            Individual parent2 = family.get(random.nextInt(family.size()));
            Individual child = crossover(parent1, parent2);
            mutate(child);
            offspring.add(child);
        }
        return offspring;
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Program started");

        Map<String, List<Item>> testCases = loadItems("src/BPP.txt");

        for (Map.Entry<String, List<Item>> entry : testCases.entrySet()) {
            String testCaseName = entry.getKey();
            List<Item> items = entry.getValue();

            System.out.println("Solving test case: " + testCaseName);
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
            System.out.println("Best solution for " + testCaseName + " uses " + bestSolution.bins.size() + " bins.");

            // Print the details of each bin in the best solution
            System.out.println("Details of bins in the best solution for " + testCaseName + ":");
            bestSolution.printBinDetails();
        }
    }


    private static Individual crossover(Individual parent1, Individual parent2) {
//        System.out.println("Performing crossover...");
        List<Bin> offspringBins = new ArrayList<>();

        // Select some bins from parent1
        Collections.shuffle(parent1.bins);
        offspringBins.addAll(parent1.bins.subList(0, parent1.bins.size() / 2));

        // Add items from parent2 if they are not already in offspring
        for (Bin bin : parent2.bins) {
            if (!containsAny(offspringBins, bin.items)) {
                offspringBins.add(bin);
            }
        }

//        System.out.println("Crossover completed.");
        return new Individual(offspringBins);
    }

    private static boolean containsAny(List<Bin> bins, List<Item> items) {
        // Check if any item in 'items' is present in 'bins'
        for (Bin bin : bins) {
            for (Item item : items) {
                if (bin.items.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void mutate(Individual individual) {
        if (individual.bins.isEmpty()) return;

        // Randomly select a bin to mutate
        Bin selectedBin = individual.bins.get(random.nextInt(individual.bins.size()));
        if (selectedBin.items.isEmpty()) return;

        // Remove a random item from the selected bin
        Item removedItem = selectedBin.items.remove(random.nextInt(selectedBin.items.size()));

        // Try placing the removed item into another bin
        boolean placed = false;
        for (Bin bin : individual.bins) {
            if (bin != selectedBin && bin.canAddItem(removedItem, BIN_CAPACITY)) {
                bin.addItem(removedItem);
                placed = true;
                break;
            }
        }

        // If the item was not placed in any existing bin and if the selected bin is now empty, remove it
        if (!placed) {
            if (selectedBin.items.isEmpty()) {
                individual.bins.remove(selectedBin);
            } else {
                Bin newBin = new Bin();
                newBin.addItem(removedItem);
                individual.bins.add(newBin);
            }
        }

        // Cleanup: Remove any empty bins created after mutation
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

        public void printBinDetails() {
            for (int i = 0; i < bins.size(); i++) {
                Bin bin = bins.get(i);
                int totalWeight = bin.items.stream().mapToInt(item -> item.size).sum(); // Calculate the total weight
                System.out.println("Bin " + (i + 1) + ": " + bin.items + " - Total weight: " + totalWeight + "/" + BIN_CAPACITY);
            }
        }
    }

}
