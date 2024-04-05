import java.util.ArrayList;
import java.util.List;

public class FirstFit {

    public static List<List<Integer>> applyFirstFit(int[] weights, int[] quantities, int capacity) {
        List<List<Integer>> bins = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < quantities[i]; j++) {
                boolean placed = false;
                for (int k = 0; k < bins.size(); k++) {
                    int currentBinWeight = bins.get(k).stream().mapToInt(Integer::intValue).sum();
                    if (currentBinWeight + weights[i] <= capacity) {
                        bins.get(k).add(weights[i]);
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    List<Integer> newBin = new ArrayList<>();
                    newBin.add(weights[i]);
                    bins.add(newBin);
                }
            }
        }
        return bins;
    }
}
