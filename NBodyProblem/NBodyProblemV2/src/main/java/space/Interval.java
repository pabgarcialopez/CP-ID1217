package space;

public class Interval<Key extends Comparable<Key>> {
    private final Key min;    // min endpoint
    private final Key max;    // max endpoint

    public Interval(Key min, Key max) {
        if (less(max, min))
            throw new RuntimeException("Illegal argument");
        this.min = min;
        this.max = max;
    }

    // return min endpoint
    public Key min() {
        return min;
    }

    // return max endpoint
    public Key max() {
        return max;
    }

    // is x between min and max
    public boolean contains(Key x) {
        return !less(x, min) && !less(max, x);
    }

    // does this interval a intersect interval b?
    public boolean intersects(Interval<Key> b) {
        return !less(this.max, b.min) && !less(b.max, this.min);
    }

    // does this interval a equal interval b?
    public boolean equals(Interval<Key> b) {
        return this.min.equals(b.min) && this.max.equals(b.max);
    }


    // comparison helper functions
    private boolean less(Key x, Key y) {
        return x.compareTo(y) < 0;
    }

    // return string representation
    public String toString() {
        return "[" + min + ", " + max + "]";
    }
}