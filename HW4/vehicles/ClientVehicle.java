package vehicles;

import station.StationMonitor;

public class ClientVehicle extends SpaceVehicle {
    public ClientVehicle (StationMonitor station, String name, int nitrogenNeeded, int quantumFluidNeeded, int visits) {
        super(station, name, nitrogenNeeded, quantumFluidNeeded, visits);
    }

    @Override
    public void run () {
        for (int i = 0; i < visits; i++) {
            try {
                station.enterStation(name);
                station.requestFuel(name, nitrogenNeeded, quantumFluidNeeded, false);
                station.leaveStation(name);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
