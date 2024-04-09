import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BinPackingGA {
    // Constants
    private static final int POPULATION_SIZE = 100; // Size of the population in each generation
    private static final int GENERATIONS = 1000; // Number of generations for which the algorithm will run
    private static final Random random = new Random(); // Random number generator
    private static final int BIN_CAPACITY = 10000; // The capacity of each bin
    private static final int OFFSPRING_SIZE = 50; // Number of offspring to produce in each generation

    // Adjust initial population generation method
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
        List<Bin> offspringBins = new ArrayList<>();

        // Randomly select bins from the first parent
        Collections.shuffle(parent1.bins);
        List<Bin> selectedBinsFromParent1 = parent1.bins.subList(0, parent1.bins.size() / 2);

        // Add selected bins to offspring
        for (Bin bin : selectedBinsFromParent1) {
            offspringBins.add(new Bin(new ArrayList<>(bin.items)));
        }

        // Add bins from the second parent that contain items not already in the offspring
        for (Bin bin : parent2.bins) {
            if (offspringBins.stream().noneMatch(ob -> ob.items.containsAll(bin.items))) {
                offspringBins.add(new Bin(new ArrayList<>(bin.items)));
            }
        }

        // Fill remaining space in bins with items not already placed, following a FF-like approach
        Set<Item> placedItems = offspringBins.stream()
                .flatMap(bin -> bin.items.stream())
                .collect(Collectors.toSet());

        List<Item> remainingItems = parent1.bins.stream()
                .flatMap(bin -> bin.items.stream())
                .filter(item -> !placedItems.contains(item))
                .collect(Collectors.toList());

        for (Item item : remainingItems) {
            Optional<Bin> bin = offspringBins.stream()
                    .filter(b -> b.canAddItem(item, binCapacity))
                    .findFirst();

            bin.ifPresentOrElse(
                    b -> b.addItem(item),
                    () -> {
                        Bin newBin = new Bin();
                        newBin.addItem(item);
                        offspringBins.add(newBin);
                    }
            );
        }

        return new Individual(offspringBins);
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

    private static void selectionUsingMGG(List<Individual> population, int offspringSize, int binCapacity) {
        Random random = new Random();

        // Select two parents randomly
        Individual parent1 = population.get(random.nextInt(population.size()));
        Individual parent2 = population.get(random.nextInt(population.size()));

        // Generate offspring
        List<Individual> offspring = IntStream.range(0, offspringSize)
                .mapToObj(i -> crossover(parent1, parent2, binCapacity))
                .collect(Collectors.toList());

        // Combine parents and offspring
        List<Individual> group = new ArrayList<>(offspring);
        group.add(parent1);
        group.add(parent2);

        // Sort by fitness (you may need to implement a fitness function)
        group.sort(Comparator.comparing(Individual::getFitness));

        // Replace the worst individuals in the population with the best in the group
        for (int i = 0; i < 2; i++) {
            population.remove(parent1);
            population.remove(parent2);
            population.add(group.get(i));
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
                selectionUsingMGG(population, OFFSPRING_SIZE, BIN_CAPACITY);
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