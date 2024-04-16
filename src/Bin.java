import java.util.ArrayList;
import java.util.List;

public class Bin {
    private static int idCounter = 0;  // Static counter to ensure each bin gets a unique ID
    private int id;
    List<Item> items;

    public Bin() {
        this.items = new ArrayList<>();
        this.id = idCounter++;  // Assign an ID and increment the counter
    }

    public Bin(List<Item> items) {
        this.items = new ArrayList<>(items);
        this.id = idCounter++;  // Assign an ID and increment the counter
    }

    public void addItem(Item item) {
        this.items.add(item);
    }

    public boolean canAddItem(Item item, int binCapacity) {
        int currentSize = items.stream().mapToInt(Item::getSize).sum();
        return currentSize + item.getSize() <= binCapacity;
    }

    public int getCurrentSize() {
        return items.stream().mapToInt(Item::getSize).sum();
    }

    public int getId() {
        return id;  // Getter for bin ID
    }

    // Method to create a deep copy of a Bin
    public Bin copy() {
        return new Bin(new ArrayList<>(this.items));  // Use the constructor that accepts items
    }
}

