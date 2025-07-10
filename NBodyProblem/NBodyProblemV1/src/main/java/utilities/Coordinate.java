package utilities;

public class Coordinate {

    private double x, y;
    public Coordinate (double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Coordinate (Coordinate p) {
        this.x = p.getX();
        this.y = p.getY();
    }

    public void add(Coordinate c) {
        this.add(c.getX(), c.getY());
    }

    public void add (double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    public void substract (Coordinate c) {
        this.add(-c.getX(), -c.getY());
    }


    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }

    public void set(Coordinate c) {set(c.getX(), c.getY());}

    public void set(double x, double y) {setX(x); setY(y);}

    public double getX () {return x;}

    public void setX (double x) {
        this.x = x;
    }

    public double getY () {return y;}

    public void setY (double y) {this.y = y;}
}
