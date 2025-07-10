
import utilities.Coordinate;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Worker implements Runnable {

    final double G = 6.67e-11;

    private final int workerID;
    private final CyclicBarrier barrier;
    private static Vector<Coordinate> aggregatedForces;

    public Worker(int workerID, CyclicBarrier barrier) {
        this.workerID = workerID;
        this.barrier = barrier;

        Worker.aggregatedForces = new Vector<>(Runner.getNumBodies());
        for(int i = 0; i < Runner.getNumBodies(); i++)
            aggregatedForces.add(new Coordinate(0, 0));
    }

    // Calculate total force for every pair of bodies.
    private void calculateForces () {

        double distance, magnitude;
        Coordinate direction;

        // Positions are only read.
        Vector<Coordinate> positions = Runner.getPositions();

        // Forces are read and written into.
        Vector<Vector<Coordinate>> forces = Runner.getForces();

        // Assign bodies with the strips method.
        for(int i = workerID; i < Runner.getNumBodies() - 1; i += Runner.getNumWorkers()) {
            for(int j = i + 1; j < Runner.getNumBodies(); j++) {

                // Compute the distance between the two bodies.
                double xComponent = positions.get(i).getX() - positions.get(j).getX();
                double yComponent = positions.get(i).getY() - positions.get(j).getY();
                distance = Math.sqrt(xComponent * xComponent + yComponent * yComponent);

                // Compute the magnitude of the force between them.
                magnitude = (G * Runner.getMasses().get(i) * Runner.getMasses().get(j)) / (distance * distance);

                // Compute the direction of the force.
                direction = new Coordinate(positions.get(j).getX() - positions.get(i).getX(),
                                           positions.get(j).getY() - positions.get(i).getY());

                // Update the force exerted on each body.
                double forceX = magnitude * direction.getX() / distance;
                double forceY = magnitude * direction.getY() / distance;
                Coordinate force = new Coordinate(forceX, forceY);

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

    private Coordinate computePosition (Coordinate oldPosition, Coordinate deltaP) {

        Random rnd = new Random();
        double gridWidth = Runner.X_AXIS_MAX - Runner.X_AXIS_MIN;
        double gridHeight = Runner.Y_AXIS_MAX - Runner.Y_AXIS_MIN;
        double newPositionX = oldPosition.getX() + deltaP.getX();
        double newPositionY = oldPosition.getY() + deltaP.getY();

        // Use random values to ensure that a lot of nodes don't end up in some equal extreme point.
        if(newPositionX > Runner.X_AXIS_MAX)
            newPositionX = Runner.X_AXIS_MAX - rnd.nextDouble(gridWidth) / 10;
        else if(newPositionX < Runner.X_AXIS_MIN)
            newPositionX = Runner.X_AXIS_MIN + rnd.nextDouble(gridWidth) / 10;
        if(newPositionY > Runner.Y_AXIS_MAX)
            newPositionY = Runner.Y_AXIS_MAX - rnd.nextDouble(gridHeight) / 10;
        else if(newPositionY < Runner.Y_AXIS_MIN)
            newPositionY = Runner.Y_AXIS_MIN + rnd.nextDouble(gridHeight) / 10;

        return new Coordinate(newPositionX, newPositionY);
    }

    // Calculates new velocity and position for each body.
    private void moveBodies() {
        Coordinate deltaV, deltaP;

        // Positions and velocities are read and written into.
        Vector<Coordinate> positions = Runner.getPositions();
        Vector<Coordinate> velocities = Runner.getVelocities();
        Vector<Vector<Coordinate>> forces = Runner.getForces();
        Vector<Double> masses = Runner.getMasses();
        int DT = Runner.getDT();

        for(int body = workerID; body < Runner.getNumBodies(); body += Runner.getNumWorkers()) {

            // Sum forces and reset them to 0 for the current body
            Coordinate aggregatedForce = aggregateForces(forces, body);


            // Compute velocity increase of body i with F = m*(v/DT)
            deltaV = new Coordinate(aggregatedForce.getX() / masses.get(body) * DT,
                                    aggregatedForce.getY() / masses.get(body) * DT);

            // Compute position of body i.
            deltaP = new Coordinate((velocities.get(body).getX() + deltaV.getX() / 2) * DT,
                                    (velocities.get(body).getY() + deltaV.getY() / 2) * DT);

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

    public static Vector<Coordinate> getAggregatedForces () {
        return aggregatedForces;
    }
}
