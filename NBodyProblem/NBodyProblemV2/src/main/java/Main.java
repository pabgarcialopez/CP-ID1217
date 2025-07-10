import utilities.ArgsParser;

public class Main {
    public static void main(String[] args) {

        ArgsParser.parseArguments(args);

        int DT = ArgsParser.getDT();
        int numBodies = ArgsParser.getNumBodies();
        int numSteps = ArgsParser.getNumSteps();
        int start = ArgsParser.getStart();
        int numWorkers = ArgsParser.getNumWorkers();
        boolean report = ArgsParser.getReport();

        try {
            new Runner(numBodies, numSteps, start, DT, numWorkers, report).runProgram();
        } catch (InterruptedException e) {
            System.err.println("ERROR in Runner: " + e.getMessage());
        }
    }
}
