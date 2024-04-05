import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Problem {
    String name;
    int binCapacity;
    List<Integer> items;

    Problem() {
        items = new ArrayList<>();
    }

    int calculateLowerBound() {
        return items.stream().mapToInt(Integer::intValue).sum() / binCapacity;
    }

    int calculateNofitems() {
        return items.size();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Problem: \"").append(name).append("\"\n");
        result.append("Bin Capacity: ").append(binCapacity).append("\n");
        result.append("Items = [");

        for (int i = 0; i < items.size(); i++) {  // Added '<' to fix the condition
            result.append(items.get(i));
            if (i < items.size() - 1) {
                result.append(", ");
            }
        }

        result.append("]\n");
        result.append("No of Items: ").append(calculateNofitems()).append("\n");
        result.append("Lower Bound: ").append(calculateLowerBound()).append("\n");

        return result.toString();
    }
}
