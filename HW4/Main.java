import station.StationMonitor;
import vehicles.ClientVehicle;
import vehicles.SpaceVehicle;
import vehicles.SupplyVehicle;

public class Main {

    private static final int maxNitrogen = 100;
    private static final int maxQuantumFluid = 100;
    private static final int maxDockingPlaces = 2;
    public static void main(String[] args) throws InterruptedException {
        StationMonitor station = new StationMonitor(maxNitrogen, maxQuantumFluid, maxDockingPlaces);

//        SpaceVehicle[] vehicles = {
//                new ClientVehicle(station, "CV 1", 10, 10, 2),
//                new ClientVehicle(station, "CV 2", 5, 5, 3),
//                new ClientVehicle(station, "CV 3", 8, 3, 2),
//                new SupplyVehicle(station, "SV 1", 2, 3, 3, 3, 1),
//                new SupplyVehicle(station, "SV 2", 4, 2, 2, 3, 1)
//        };

        SpaceVehicle[] vehicles = {
                new ClientVehicle(station, "CV 1", 1, 1, 10),
                new ClientVehicle(station, "CV 2", 1, 1, 10),
                new ClientVehicle(station, "CV 3", 1, 1, 10),
                new SupplyVehicle(station, "SV 1", 1, 1, 1, 1, 10),
                new SupplyVehicle(station, "SV 2", 1, 1, 1, 1, 10)
        };

        station.displayStatus("\nInitial status");

        // Start threads
        for (SpaceVehicle vehicle : vehicles) {
            vehicle.start();
        }

        for(SpaceVehicle vehicle : vehicles) {
            vehicle.join();
        }

        System.out.println("Exiting main thread...");
    }
}
