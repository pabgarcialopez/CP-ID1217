package utilities;

import space.Coordinate;

public class Report {

    Coordinate position;
    Coordinate velocity;
    Coordinate force;
    public Coordinate getPosition () {
        return position;
    }
    public Coordinate getVelocity () {
        return velocity;
    }
    public Coordinate getForce () {return force;}
    public void setPosition (Coordinate position) {
        this.position = position;
    }

    public void setVelocity (Coordinate velocity) {
        this.velocity = velocity;
    }

    public void setForce (Coordinate force) {
        this.force = force;
    }


}
