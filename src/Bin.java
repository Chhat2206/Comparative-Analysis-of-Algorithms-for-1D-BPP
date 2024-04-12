import java.util.ArrayList;
import java.util.List;

public class Bin {
    List<Item> items;

    public Bin() {
        this.items = new ArrayList<>();
    }

    public Bin(List<Item> items) {
        this.items = new ArrayList<>(items);
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
}