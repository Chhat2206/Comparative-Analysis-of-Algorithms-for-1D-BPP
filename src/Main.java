import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        String fileName = "src/BPP.txt";
        File file = new File(fileName);
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            // Reading the test case
            String testName = scanner.nextLine().trim();
            int numberOfItems = Integer.parseInt(scanner.nextLine().trim());
            int binCapacity = Integer.parseInt(scanner.nextLine().trim());

            // Reading item weights and quantities
            int[] weights = new int[numberOfItems];
            int[] quantities = new int[numberOfItems];
            int totalItems = 0;
            int totalWeightOfAllItems = 0;

            for (int i = 0; i < numberOfItems; i++) {
                String[] line = scanner.nextLine().trim().split("\\s+");
                weights[i] = Integer.parseInt(line[0]);
                quantities[i] = Integer.parseInt(line[1]);
                totalItems += quantities[i];
                totalWeightOfAllItems += weights[i] * quantities[i];
            }

            // Solve the bin packing problem using Best Fit algorithm
            List<List<Integer>> bins = BestFit.applyBestFit(weights, quantities, binCapacity);

            // Calculate the total weight of items in bins
            int totalWeightInBins = bins.stream()
                    .flatMapToInt(bin -> bin.stream().mapToInt(Integer::intValue))
                    .sum();

            // Print the result
            System.out.println("Test case: " + testName);
            System.out.println("Number of bins required: " + bins.size());
            System.out.println("Total items loaded: " + totalItems);
            for (int i = 0; i < bins.size(); i++) {
                int binTotalWeight = bins.get(i).stream().mapToInt(Integer::intValue).sum();
                System.out.println("Bin " + (i + 1) + ": " + bins.get(i) + " - Total weight: " + binTotalWeight + "/" + binCapacity);
            }

            System.out.println("Total weight of all items in " + testName + ": " + totalWeightOfAllItems);
            System.out.println("Total weight in bins for " + testName + ": " + totalWeightInBins);

            // Check if total weights match
            if (totalWeightOfAllItems != totalWeightInBins) {
                System.out.println("Warning: There is a discrepancy in the total weights for " + testName);
            }
            System.out.println();
        }

        scanner.close();
    }
}
