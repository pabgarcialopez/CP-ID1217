package space;

public class Rectangle {
    public final Interval<Double> intervalX;   // x-interval
    public final Interval<Double> intervalY;   // y-interval

    public Rectangle (Interval<Double> intervalX, Interval<Double> intervalY) {
        this.intervalX = intervalX;
        this.intervalY = intervalY;
    }

    public double getWidth() {
        return this.intervalX.max() - this.intervalX.min();
    }

    // Return String representation
    public String toString() {
        return intervalX + " x " + intervalY;
    }
}
