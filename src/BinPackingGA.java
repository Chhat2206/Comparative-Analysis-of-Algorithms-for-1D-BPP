import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class BinPackingGA {
    private static final int POPULATION_SIZE = 100;
    private static final int GENERATIONS = 500;
    private static final Random random = new Random();
    private static final int BIN_CAPACITY = 10000;
    private static final int FAMILY_SIZE = 5;
    private static final int OFFSPRING_SIZE = 2;
    private static List<Individual> generateInitialPopulation(List<Item> items, int binCapacity, int populationSize) {
        long startTime = System.currentTimeMillis();
        System.out.println("Generating initial population...");

        List<Individual> population = new ArrayList<>();
        Collections.sort(items, (item1, item2) -> item2.size - item1.size); // Sort items in decreasing order of size

        for (int i = 0; i < populationSize; i++) {
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
            population.add(new Individual(bins));
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Initial population generated in " + (endTime - startTime) + " ms");
        return population;
    }

    private static List<Item> loadItems(String fileName) throws FileNotFoundException {
        List<Item> items = new ArrayList<>();
        File file = new File(fileName);
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String testName = scanner.nextLine().trim(); // Test case name, currently unused
                int numberOfItems = Integer.parseInt(scanner.nextLine().trim());
                int binCapacity = Integer.parseInt(scanner.nextLine().trim()); // Bin capacity, can be used as needed

                for (int i = 0; i < numberOfItems; i++) {
                    String[] line = scanner.nextLine().trim().split("\\s+");
                    int weight = Integer.parseInt(line[0]);
                    int quantity = Integer.parseInt(line[1]);

                    for (int j = 0; j < quantity; j++) {
                        items.add(new Item(weight));
                    }
                }
            }
        }
        return items;
    }

    private static void applyMGG(List<Individual> population, int familySize, int offspringSize) {
        Random random = new Random();

        // Step 1: Select a small group (family) from the population
        List<Individual> family = random.ints(familySize, 0, population.size())
                .mapToObj(population::get)
                .collect(Collectors.toList());

        // Step 2: Generate offspring
        List<Individual> offspring = generateOffspring(family, offspringSize);

        // Step 3: Replace the worst individuals in the population with the best offspring
        population.addAll(offspring);
        population.sort(Comparator.comparing(Individual::getFitness));
        for (int i = 0; i < offspringSize; i++) {
            population.remove(population.size() - 1); // Remove worst individuals
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

        List<Item> items = loadItems("src/BPP.txt");
        System.out.println("Items loaded: " + items.size());

        List<Individual> population = generateInitialPopulation(items, BIN_CAPACITY, POPULATION_SIZE);
        System.out.println("Initial population generated");
        for (int i = 0; i < GENERATIONS; i++) {
            applyMGG(population, FAMILY_SIZE, OFFSPRING_SIZE);
            if (i % 100 == 0) {
                Individual bestIndividual = findBestSolution(population);
                System.out.println("Generation " + i + ", Best Fitness: " + bestIndividual.getFitness());
            }
        }

        Individual bestSolution = findBestSolution(population);
        System.out.println("Best solution uses " + bestSolution.bins.size() + " bins.");

        // Print the details of each bin in the best solution
        System.out.println("Details of bins in the best solution:");
        bestSolution.printBinDetails();
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
//        System.out.println("Performing mutation...");
        // Randomly select a bin to mutate
        if (individual.bins.isEmpty()) return;

        Bin selectedBin = individual.bins.get(random.nextInt(individual.bins.size()));
        if (selectedBin.items.isEmpty()) return;

        // Remove a random item from the selected bin and place it in a new bin
        Item removedItem = selectedBin.items.remove(random.nextInt(selectedBin.items.size()));
        Bin newBin = new Bin();
        newBin.items.add(removedItem);
        individual.bins.add(newBin);
//        System.out.println("Mutation completed.");
    }

    private static Individual selectIndividual(List<Individual> population) {
        return population.get(random.nextInt(population.size()));
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
                int totalWeight = bin.items.stream().mapToInt(item -> item.size).sum();
                System.out.println("Bin " + (i + 1) + ": " + bin.items + " - Total weight: " + totalWeight + "/" + BIN_CAPACITY);
            }
        }
    }

}
