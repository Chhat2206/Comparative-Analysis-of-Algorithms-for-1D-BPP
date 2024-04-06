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

    public int getCurrentSize() {
        return items.stream().mapToInt(i -> i.size).sum();
    }

    public boolean canAddItem(Item item, int binCapacity) {
        return getCurrentSize() + item.size <= binCapacity;
    }
}
