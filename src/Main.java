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
            int totalItems = 0; // To count the total number of items

            for (int i = 0; i < numberOfItems; i++) {
                String[] line = scanner.nextLine().trim().split("\\s+");
                weights[i] = Integer.parseInt(line[0]);
                quantities[i] = Integer.parseInt(line[1]);
                totalItems += quantities[i]; // Add the quantity of each item to totalItems
            }

            // Solve the bin packing problem using First Fit algorithm
//            List<List<Integer>> bins = FirstFit.applyFirstFit(weights, quantities, binCapacity);

            // Solve the bin packing problem using Best Fit algorithm
            List<List<Integer>> bins = BestFit.applyBestFit(weights, quantities, binCapacity);

            // Print the result
            System.out.println("Test case: " + testName);
            System.out.println("Number of bins required: " + bins.size());
            System.out.println("Total items loaded: " + totalItems);
            for (int i = 0; i < bins.size(); i++) {
                int binTotalWeight = bins.get(i).stream().mapToInt(Integer::intValue).sum();
                System.out.println("Bin " + (i + 1) + ": " + bins.get(i) + " - Total weight: " + binTotalWeight + "/" + binCapacity);
            }
            System.out.println();
        }

        scanner.close();
    }
}
