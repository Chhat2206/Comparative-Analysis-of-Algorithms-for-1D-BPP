public class Item {
    int size;

    public Item(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return String.valueOf(size);
    }
}
