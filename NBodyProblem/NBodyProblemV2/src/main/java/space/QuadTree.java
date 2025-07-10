package space;

import java.util.Vector;

public class QuadTree {

    private static final Double THETA = 0.5;
    public final static Double G = 6.67e-11;
    public final static Double X_AXIS_MIN = 0.0;
    public final static Double Y_AXIS_MIN = 0.0;
    public final static Double X_AXIS_MAX = 50.0;
    public final static Double Y_AXIS_MAX = 50.0;

    private final Vector<Node> nodes;

    // Note the position of the node is the same position from the body at Runner (reference wise).
    public QuadTree(Vector<Double> masses, Vector<Coordinate> positions) {
        this.nodes = new Vector<>();
        for (int i = 0; i < masses.size(); i++)
            this.insert(new NodeInfo(masses.get(i), positions.get(i)));
    }

    private Node root;
    private final class Node {
        private final NodeInfo info;         // Associated data
        private Node NW, NE, SE, SW;         // Four subtrees
        Node(NodeInfo info) {
            this.info = info;
        }
    }

    /**
     * Inserts a node in the tree.
     * @param nodeInfo Info of node to be inserted.
     */
    private void insert(NodeInfo nodeInfo) {
        root = insert(root, new Node(nodeInfo));
    }

    /**
     * Recursively inserts a node in the tree.
     * @param parent Parent of {@code child}
     * @param child Node to be inserted
     * @return Returns root of the tree
     */
    private Node insert(Node parent, Node child) {

        // Note that "nodeInfo" is the information of the node to be inserted.
        // "parent" is the parent node of the node to be inserted.

        if (parent == null) {
            if(this.isEmpty()) // If the tree is empty.
                child.info.setRectangle(new Rectangle(new Interval<>(X_AXIS_MIN, X_AXIS_MAX),
                                                      new Interval<>(Y_AXIS_MIN, Y_AXIS_MAX)));
            nodes.add(child);
            return child;
        }

        // Parent is not null ==> parent != first node to be inserted or quadrant.
        child.info.setRectangle(findRectangle(parent, child));

        double childX = child.info.getNodePosition().getX();
        double childY = child.info.getNodePosition().getY();
        double parentX = parent.info.getNodePosition().getX();
        double parentY = parent.info.getNodePosition().getY();

        // Which quadrant does it go to?
        if (childX < parentX && childY < parentY)
            parent.SW = insert(parent.SW, child);

        if (childX < parentX && childY >= parentY)
            parent.NW = insert(parent.NW, child);

        if (childX >= parentX && childY < parentY)
            parent.SE = insert(parent.SE, child);

        if (childX >= parentX && childY >= parentY)
            parent.NE = insert(parent.NE, child);

        return parent;
    }

    /* === === === === === === === === === === === === === === === === === ===
     *  Auxiliary functions
     * === === === === === === === === === === === === === === === === === === */

    private boolean isEmpty() {
        return this.size() == 0;
    }

    public int size () {
        return nodes.size();
    }

    private Vector<Node> getChildren(Node node) {
        Vector<Node> children = new Vector<>();
        if(node.NW != null) children.add(node.NW);
        if(node.NE != null) children.add(node.NE);
        if(node.SW != null) children.add(node.SW);
        if(node.SE != null) children.add(node.SE);
        return children;
    }

    // Calculate distance from n1 to n2 (order matters in this case because of estimation).
    private double distance(Node n1, Node n2, boolean estimate) {
        // Coordinate n1Pos = estimate ? n1.info.getCenterOfMass() : n1.info.getNodePosition();
        Coordinate n2Pos = estimate ? n2.info.getCenterOfMass() : n2.info.getNodePosition();
        return n1.info.getNodePosition().distance(n2Pos);
    }

    private Coordinate computeForce (Node node1, Node node2, boolean estimate) {
        // Get data of node2 depending on whether we're estimating or not.
        double node2Mass = estimate ? node2.info.getAccumulatedMass() : node2.info.getNodeMass();
        Coordinate node2Pos = estimate ? node2.info.getCenterOfMass() : node2.info.getNodePosition();

        // Compute distance between the nodes.
        double dist = distance(node1, node2, estimate);

        // Compute the magnitude of the force between them.
        double magnitude = (G * node1.info.getNodeMass() * node2Mass) / (dist * dist);

        // Compute the direction of the force.
        Coordinate direction = new Coordinate(node2Pos.getX() - node1.info.getNodePosition().getX(),
                                              node2Pos.getY() - node1.info.getNodePosition().getY());

        // Update the force exerted on each body.
        double forceX = magnitude * direction.getX() / dist;
        double forceY = magnitude * direction.getY() / dist;

        return new Coordinate(forceX, forceY);
    }

    /**
     * Finds the rectangle in which {@code child} is contained.
     * @param parent Parent node of {@code child}
     * @param child  Node for the rectangle search
     * @return Rectangle in which {@code child} is contained.
     */
    private Rectangle findRectangle (Node parent, Node child) {

        Interval<Double> xInterval, yInterval;

        Rectangle parentRectangle = parent.info.getRectangle();
        double childX = child.info.getNodePosition().getX();
        double childY = child.info.getNodePosition().getY();
        double parentX = parent.info.getNodePosition().getX();
        double parentY = parent.info.getNodePosition().getY();

        double bottomX = parentRectangle.intervalX.min();
        double topX = parentRectangle.intervalX.max();
        double middleX = (bottomX + topX) / 2;
        double bottomY = parentRectangle.intervalY.min();
        double topY = parentRectangle.intervalY.max();
        double middleY = (bottomY + topY) / 2;

        // Set x-axis of rectangle.
        if(childX < parentX) xInterval = new Interval<>(bottomX, middleX);
        else xInterval = new Interval<>(middleX, topX);
        // Set y-axis of rectangle.
        if(childY < parentY) yInterval = new Interval<>(bottomY, middleY);
        else yInterval = new Interval<>(middleY, topY);

        return new Rectangle(xInterval, yInterval);
    }

    /* === === === === === === === === === === === === === === === === === ===
     *  N Body problem functions
     * === === === === === === === === === === === === === === === === === === */

    public void computeMassTree () {
        // System.out.println("positions before computeMassTree: " + this.positions);
        computeMassTree(root);
        // System.out.println("positions after computeMassTree: " + this.positions);
    }

    private NodeInfo computeMassTree (Node node) {

        Vector<Node> children = getChildren(node);

        // Case of a single body with no children.
        if(children.isEmpty())
            return node.info;

        double accumulatedMass = 0;
        Coordinate centerOfMass = new Coordinate(0, 0);

        for(Node child : children) {
            // Compute and get info of child.
            NodeInfo childInfo = computeMassTree(child);
            double childBodyMass = childInfo.getAccumulatedMass();
            Coordinate childCenterOfMass = new Coordinate(childInfo.getCenterOfMass());

            // Update information for node.
            accumulatedMass += childBodyMass;
            centerOfMass.add(childCenterOfMass);
        }

        // Center of mass calculation.
        if(accumulatedMass != 0) // The children might all be leaves.
            centerOfMass.divide(accumulatedMass);

        // Store information in node.
        node.info.setAccumulatedMass(accumulatedMass);
        node.info.setCenterOfMass(centerOfMass);

        return node.info;
    }

    public Coordinate computeForceOn (int node) {
        return computeForceTree(nodes.get(node), root);
    }

    // Compute gravitational force on node "particle" due to all particles in the box at node.
    private Coordinate computeForceTree(Node particle, Node node) {

        Vector<Node> children = getChildren(node);
        Coordinate force = new Coordinate(0, 0);

        if(particle != node) {
            if(children.isEmpty())
                return this.computeForce(particle, node, false);

            // particle != node ==> distance > 0
            double distance = distance(particle, node, false);
            double quadrantWidth = particle.info.getRectangle().getWidth();

            if(quadrantWidth/distance < THETA)
                return computeForce(particle, node, true);
        }

        for(Node child : children)
            force.add(computeForceTree(particle, child));

        return force;
    }
}
