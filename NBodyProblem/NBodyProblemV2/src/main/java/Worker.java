import space.Coordinate;
import space.QuadTree;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Worker implements Runnable {

    private final int workerID;
    private final CyclicBarrier barrier;
    private QuadTree quadTree;
    private static Vector<Coordinate> aggregatedForces;

    public Worker(int workerID, CyclicBarrier barrier) {
        this.workerID = workerID;
        this.barrier = barrier;

        // Vector is thread safe.
        Worker.aggregatedForces = new Vector<>(Runner.getNumBodies());
        for(int i = 0; i < Runner.getNumBodies(); i++)
            aggregatedForces.add(new Coordinate(0, 0));
    }

    // Calculate total force for every pair of bodies.
    private void calculateForces () {

        // Forces are read and written into.
        Vector<Vector<Coordinate>> forces = Runner.getForces();

        // Using strips method.
        for(int i = workerID; i < Runner.getNumBodies() - 1; i += Runner.getNumWorkers()) {
            for(int j = i + 1; j < Runner.getNumBodies(); j++) {
                Coordinate force = quadTree.computeForceOn(i);
                forces.get(workerID).get(i).add(force);
                forces.get(workerID).get(j).substract(force);
            }
        }
    }

    private Coordinate aggregateForces (Vector<Vector<Coordinate>> forces, int body) {
        Coordinate force = new Coordinate(0, 0);
        for (Vector<Coordinate> worker : forces) {
            force.add(worker.get(body));
            worker.get(body).set(new Coordinate(0,0));
        }
        // To be read later by runner to report it.
        aggregatedForces.set(body, force);
        return force;
    }

    // Function to keep bodies on the grid defined by QuadTree.
    private Coordinate computePosition (Coordinate oldPosition, Coordinate deltaP) {

        Random rnd = new Random();
        double gridWidth = QuadTree.X_AXIS_MAX - QuadTree.X_AXIS_MIN;
        double gridHeight = QuadTree.Y_AXIS_MAX - QuadTree.Y_AXIS_MIN;
        double newPositionX = oldPosition.getX() + deltaP.getX();
        double newPositionY = oldPosition.getY() + deltaP.getY();

        // Use random values to ensure that a lot of nodes don't end up in some equal extreme point.
        if(newPositionX > QuadTree.X_AXIS_MAX)
            newPositionX = QuadTree.X_AXIS_MAX - rnd.nextDouble(gridWidth) / 10;
        else if(newPositionX < QuadTree.X_AXIS_MIN)
            newPositionX = QuadTree.X_AXIS_MIN + rnd.nextDouble(gridWidth) / 10;
        if(newPositionY > QuadTree.Y_AXIS_MAX)
            newPositionY = QuadTree.Y_AXIS_MAX - rnd.nextDouble(gridHeight) / 10;
        else if(newPositionY < QuadTree.Y_AXIS_MIN)
            newPositionY = QuadTree.Y_AXIS_MIN + rnd.nextDouble(gridHeight) / 10;

        return new Coordinate(newPositionX, newPositionY);
    }

    // Calculates new velocity and position for each body.
    private void moveBodies() {
        Coordinate deltaV, deltaP;

        double DT = Runner.getDT();
        Vector<Double> masses = Runner.getMasses();

        // Positions and velocities are read and written into.
        Vector<Coordinate> positions = Runner.getPositions();
        Vector<Coordinate> velocities = Runner.getVelocities();
        Vector<Vector<Coordinate>> forces = Runner.getForces();

        for(int body = workerID; body < Runner.getNumBodies(); body += Runner.getNumWorkers()) {

            // Sum forces and reset them to 0 for the current body
            Coordinate aggregatedForce = aggregateForces(forces, body);

            // Compute velocity increase of body i with F = m*(v/DT)
            deltaV = new Coordinate((aggregatedForce.getX() / masses.get(body)) * DT,
                                    (aggregatedForce.getY() / masses.get(body)) * DT);

            // Compute position of body i.
            deltaP = new Coordinate((velocities.get(body).getX() + deltaV.getX() / 2) * DT,
                                    (velocities.get(body).getY() + deltaV.getY() / 2) * DT);

            // Update velocity and position of body.
            velocities.get(body).add(deltaV);
            positions.get(body).set(computePosition(positions.get(body), deltaP));
        }
    }


    @Override
    public void run () {

        try {
            calculateForces();
            barrier.await();
            moveBodies();
            barrier.await();
        }
        catch (InterruptedException | BrokenBarrierException e) {
            System.out.println("ERROR in worker " + workerID + ": " + e.getMessage());
        }
    }

    public static Vector<Coordinate> getAggregatedForces () {return aggregatedForces;}

    public void setQuadTree (QuadTree quadTree) {
        this.quadTree = quadTree;
    }
}
