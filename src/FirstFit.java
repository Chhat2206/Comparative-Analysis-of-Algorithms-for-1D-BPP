import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class FirstFit {

    public static List<List<Integer>> applyFirstFitDecreasing(int[] weights, int[] quantities, int capacity) {
        // Step 1: Create a list of all items based on their quantities
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < quantities[i]; j++) {
                items.add(weights[i]);
            }
        }

        // Step 2: Sort items in decreasing order by weight
        items.sort((a, b) -> b - a);

        // Step 3: Create bins to store the items
        List<List<Integer>> bins = new ArrayList<>();

        // Step 4: Place each item in the first bin that has enough space
        for (int item : items) {
            boolean placed = false;
            for (List<Integer> bin : bins) {
                int currentBinWeight = bin.stream().mapToInt(Integer::intValue).sum();
                if (currentBinWeight + item <= capacity) {
                    bin.add(item);
                    placed = true;
                    break;
                }
            }

            // Step 5: If no bin was found, create a new bin
            if (!placed) {
                List<Integer> newBin = new ArrayList<>();
                newBin.add(item);
                bins.add(newBin);
            }
        }

        return bins;
    }
}
