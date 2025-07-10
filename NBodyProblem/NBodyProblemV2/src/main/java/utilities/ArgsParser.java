package utilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ArgsParser {

    private static int DT;
    private static Integer numBodies;
    private static Integer numSteps;
    private static int start;
    private static int numWorkers;
    private static int report;

    private static final HashSet<String> ALLOWED_PARAMETERS = new HashSet<>(Arrays.asList(
            "bodies",
            "steps",
            "timestep",
            "start",
            "workers",
            "report"
    ));


    private static void usage (String tag) {
        System.out.println(tag);
        System.out.println("Usage: java Main --bodies <number> --steps <number> [options]");
        System.out.println("Options may be:");
        System.out.println("--timestep <number>\n--start <number>\n--workers <number>\n--report <0/1>");
        System.exit(1);
    }

    private static void checkErrors () {

        if(numBodies == null || numSteps == null)
            usage("Parameters \"numBodies\" and \"steps\" must be included");

        if(numBodies <= 0 || numSteps <= 0 || DT <= 0 || numWorkers <= 0)
            usage("Parameters \"numBodies\", \"steps\", \"DT\" and \"np\" must all be greater than 0.");

        if(start < 0)
            usage("Parameter \"start\" must be nonnegative");

        if(start > numSteps)
            usage("Parameter \"start\" cannot exceed parameter \"numSteps\"");

        if(report != 0 && report != 1)
            usage("Parameter \"report\" must be 0 or 1");

        if((numSteps - start) % DT != 0)
            usage("(steps - start) % DT must be 0");
    }

    public static void parseArguments (String[] args) {

        if (args.length < 4 || args.length > ALLOWED_PARAMETERS.size() * 2)
            usage("Incorrect argument size (" + args.length + ")");

        HashMap<String, Integer> parameters = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            String key = args[i].substring(2);
            if(!ALLOWED_PARAMETERS.contains(key)) usage("Parameter " + key + " is not valid.");
            parameters.put(key, Integer.parseInt(args[i + 1]));
        }

        numBodies = parameters.get("bodies");
        numSteps = parameters.get("steps");
        DT = parameters.getOrDefault("timestep", 1);
        start = parameters.getOrDefault("start", 0);
        numWorkers = parameters.getOrDefault("workers", 1);
        report = parameters.getOrDefault("report", 0);

        checkErrors();
    }

    public static int getDT () {return DT;}
    public static Integer getNumBodies () {return numBodies;}
    public static Integer getNumSteps () {return numSteps;}
    public static int getStart () {return start;}
    public static int getNumWorkers () {return numWorkers;}
    public static boolean getReport() {return report == 1;}
}
