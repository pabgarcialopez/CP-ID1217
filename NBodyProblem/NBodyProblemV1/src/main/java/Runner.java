import utilities.Coordinate;
import utilities.Report;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Runner {

    public static final double X_AXIS_MIN = 0.0;
    public static final double Y_AXIS_MIN = 0.0;
    public static final double X_AXIS_MAX = 50.0;
    public static final double Y_AXIS_MAX = 50.0;

    public final static Integer SEED = 1547;
    public final static Integer TIMEOUT = 90; // Measured in seconds
    private final static Double MASS_MAGNITUDE = 10e8;
    private final static Double INITIAL_VELOCITY_LIMIT = 1.5;

    private static int DT;
    private static int start;
    private static int numBodies;
    private static int numSteps;
    private static int numWorkers;
    private static boolean makeReport;

    // Vector is thread-safe.
    private static Vector<Double> masses;
    private static Vector<Coordinate> positions;
    private static Vector<Coordinate> velocities;
    private static Vector<Vector<Coordinate>> forces;

    // To save the data from each iteration.
    Vector<Vector<Report>> report;

    public Runner(int numBodies, int numSteps, int start, int DT, int numWorkers, boolean makeReport) {
        Runner.DT = DT;
        Runner.start = start;
        Runner.numBodies = numBodies;
        Runner.numSteps = numSteps;
        Runner.numWorkers = numWorkers;
        Runner.makeReport = makeReport;

        // Initialize vectors.
        Random rnd = new Random(SEED);
        Runner.masses = new Vector<>(numBodies);
        Runner.positions = new Vector<>(numBodies);
        Runner.velocities = new Vector<>(numBodies);
        HashSet<Coordinate> pointSet = new HashSet<>();
        for(int i = 0; i < numBodies; i++) {
            positions.add(obtainValidPosition(pointSet));
            masses.add((rnd.nextDouble() * MASS_MAGNITUDE) % MASS_MAGNITUDE);
            velocities.add(new Coordinate(rnd.nextDouble(INITIAL_VELOCITY_LIMIT),
                                          rnd.nextDouble(INITIAL_VELOCITY_LIMIT)));
        }

        // Initialize forces matrix.
        forces = new Vector<>(numWorkers);
        for(int i = 0; i < numWorkers; i++) {
            forces.add(new Vector<>(numBodies));
            for(int j = 0; j < numBodies; j++)
                forces.get(i).add(new Coordinate(0, 0));
        }

        if(makeReport) {
            // Initialize matrix of reports.
            int totalNumSteps = (numSteps - start) / DT;
            this.report = new Vector<>(numBodies);
            for (int i = 0; i < numBodies; i++) {
                report.add(new Vector<>(totalNumSteps));
                for (int j = 0; j < totalNumSteps; j++)
                    report.get(i).add(new Report());
            }
        }
    }

    // Ensures that newly created coordinates don't occupy the same spot on the grid.
    private Coordinate obtainValidPosition (HashSet<Coordinate> coordinateSet) {
        Random rnd = new Random();
        Coordinate pos;
        do {pos = new Coordinate(rnd.nextDouble(X_AXIS_MAX - X_AXIS_MIN) + X_AXIS_MIN,
                                 rnd.nextDouble(Y_AXIS_MAX - Y_AXIS_MIN) + Y_AXIS_MIN);}
        while(coordinateSet.contains(pos));
        coordinateSet.add(pos);
        return pos;
    }

    private void reportPosition (int body, int time) {
        report.get(body).get(time).setPosition(new Coordinate(positions.get(body)));
    }

    private void reportVelocity (int body, int time) {
        report.get(body).get(time).setVelocity(new Coordinate(velocities.get(body)));
    }

    private void reportForce (int body, int time) {
        report.get(body).get(time).setForce(new Coordinate(Worker.getAggregatedForces().get(body)));
    }

    private void reportResults (int time) {
        for(int body = 0; body < numBodies; body++) {
            reportPosition(body, time);
            reportVelocity(body, time);
            reportForce(body, time);
        }
    }

    private void timeout () {
        System.out.println("TIMEOUT after " + TIMEOUT + " second(s).");
        System.exit(1);
    }

    public void runProgram() throws InterruptedException {

        double executionTime = 0, initTime;
        int totalNumSteps = (numSteps - start) / DT;

        // Initialize threads
        Worker[] workers = new Worker[numWorkers];
        CyclicBarrier barrier = new CyclicBarrier(numWorkers);
        for(int i = 0; i < workers.length; i++)
            workers[i] = new Worker(i, barrier);

        // A new pool is created with each timestep. A thread that has already been executed cannot be reused.
        ExecutorService threadPool;

        // Run program
        for(int time = 0; time < totalNumSteps; time++) {

            threadPool = Executors.newFixedThreadPool(numWorkers);

            initTime = System.nanoTime();
            for (Worker w : workers)
                threadPool.execute(w);
            threadPool.shutdown();
            if(!threadPool.awaitTermination(TIMEOUT, TimeUnit.SECONDS))
                timeout();
            executionTime += System.nanoTime() - initTime;

            if(makeReport)
                reportResults(time);
        }

        // Aggregate results
        System.out.println("Execution time was: " + executionTime/10e9 + " s.");

        if(makeReport)
            new utilities.Excel().saveResults(report, numBodies, numSteps, DT, start, executionTime, masses, numWorkers);
    }

    public static int getDT () {return DT;}
    public static int getNumWorkers() {return numWorkers;}
    public static int getNumBodies () {return numBodies;}
    public static Vector<Double> getMasses () {return masses;}
    public static Vector<Coordinate> getPositions () {return positions;}
    public static Vector<Coordinate> getVelocities () {return velocities;}
    public static Vector<Vector<Coordinate>> getForces () {return forces;}
}
