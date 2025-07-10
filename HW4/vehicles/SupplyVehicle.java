package vehicles;

import station.StationMonitor;

public class SupplyVehicle extends ClientVehicle {

    private final int nitrogenSupplied;
    private final int quantumFluidSupplied;
    public SupplyVehicle (StationMonitor station, String name, int nitrogenNeeded, int quantumFluidNeeded,  int nitrogenSupplied, int quantumFluidSupplied, int visits) {
        super(station, name, nitrogenNeeded, quantumFluidNeeded, visits);
        this.nitrogenSupplied = nitrogenSupplied;
        this.quantumFluidSupplied = quantumFluidSupplied;
    }

    @Override
    public void run () {
        for (int i = 0; i < visits; i++) {
            try {
                station.enterStation(name);
                station.supplyFuel(name, nitrogenSupplied, quantumFluidSupplied);
                station.requestFuel(name, nitrogenNeeded, quantumFluidNeeded, true);
                station.leaveStation(name);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }

    }
}
