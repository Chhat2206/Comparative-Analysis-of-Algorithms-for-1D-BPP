import java.util.ArrayList;
import java.util.List;

public class Bin {
    List<Item> items;

    public Bin() {
        this.items = new ArrayList<>();
    }

    // Add a constructor that accepts a list of items
    public Bin(List<Item> items) {
        this.items = new ArrayList<>(items); // Create a new list based on the passed items
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public int getCurrentSize() {
        return items.stream().mapToInt(i -> i.size).sum();
    }

    public boolean canAddItem(Item item, int binCapacity) {
        return getCurrentSize() + item.size <= binCapacity;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
