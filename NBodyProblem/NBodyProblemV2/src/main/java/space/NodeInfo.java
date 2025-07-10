package space;

public class NodeInfo {

    private final double nodeMass;
    private final Coordinate nodePosition;
    private Rectangle rectangle;

    // Used when computing mass tree.
    private double childrenMass;
    private Coordinate centerOfMass;

    public NodeInfo (double nodeMass, Coordinate bodyPosition) {
        this.nodeMass = nodeMass;
        this.nodePosition = bodyPosition;
        this.childrenMass = 0;
        this.centerOfMass = new Coordinate(bodyPosition.getX(), bodyPosition.getY());
        this.centerOfMass.divide(nodeMass);
    }

    public double getNodeMass () {
        return nodeMass;
    }

    public Coordinate getNodePosition () {
        return nodePosition;
    }

    public Rectangle getRectangle () {
        return rectangle;
    }

    public void setRectangle (Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    public void setAccumulatedMass (double newBodyMass) {
        this.childrenMass = newBodyMass;
    }

    public void setCenterOfMass (Coordinate newBodyPos) {
        this.centerOfMass = newBodyPos;
    }

    public double getAccumulatedMass () {
        return this.childrenMass;
    }

    public Coordinate getCenterOfMass () {
        return this.centerOfMass;
    }
}
