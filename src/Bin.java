import java.util.ArrayList;
import java.util.List;

public class Bin {
    List<Item> items;

    public Bin() {
        this.items = new ArrayList<>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    // Method to check if nthe bin is empty
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getCurrentSize() {
        return items.stream().mapToInt(i -> i.size).sum();
    }

    public boolean canAddItem(Item item, int binCapacity) {
        return getCurrentSize() + item.size <= binCapacity;
    }

    public boolean canMerge(Bin other, int binCapacity) {
        int totalSize = this.getCurrentSize() + other.getCurrentSize();
        return totalSize <= binCapacity;
    }

    public void merge(Bin other) {
        this.items.addAll(other.items);
        other.items.clear();
    }

    // New constructor to create a Bin with a given list of items
    public Bin(List<Item> items) {
        this.items = new ArrayList<>(items); // Create a new list from the given items
    }

}
