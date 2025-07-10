package vehicles;
import station.StationMonitor;

public abstract class SpaceVehicle extends Thread {
    protected final StationMonitor station;
    protected final String name;
    protected final int nitrogenNeeded;
    protected final int quantumFluidNeeded;
    protected final int visits;

    public SpaceVehicle(StationMonitor station, String name, int nitrogenNeeded, int quantumFluidNeeded, int visits) {
        this.station = station;
        this.name = name;
        this.nitrogenNeeded = nitrogenNeeded;
        this.quantumFluidNeeded = quantumFluidNeeded;
        this.visits = visits;
    }

    public abstract void run();
}