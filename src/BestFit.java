import java.util.ArrayList;
import java.util.List;

public class BestFit {

    public static List<List<Integer>> applyBestFit(int[] weights, int[] quantities, int capacity) {
        List<List<Integer>> bins = new ArrayList<>();

        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < quantities[i]; j++) {
                int bestBinIndex = -1;
                int minSpaceLeft = capacity;

                for (int k = 0; k < bins.size(); k++) {
                    int currentBinWeight = bins.get(k).stream().mapToInt(Integer::intValue).sum();
                    int spaceLeft = capacity - currentBinWeight;

                    if (spaceLeft >= weights[i] && spaceLeft - weights[i] < minSpaceLeft) {
                        minSpaceLeft = spaceLeft - weights[i];
                        bestBinIndex = k;
                    }
                }

                if (bestBinIndex != -1) {
                    bins.get(bestBinIndex).add(weights[i]);
                } else {
                    List<Integer> newBin = new ArrayList<>();
                    newBin.add(weights[i]);
                    bins.add(newBin);
                }
            }
        }

        return bins;
    }
}
