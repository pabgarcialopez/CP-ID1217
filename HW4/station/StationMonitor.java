package station;

public class StationMonitor {
    private int currentNitrogen;
    private int currentQuantumFluid;
    private int freeDockingPlaces;
    private final int nitrogenCapacity;
    private final int quantumFluidCapacity;
    private final int dockingPlacesCapacity;

    public StationMonitor(int nitrogenCapacity, int quantumFluidCapacity, int maxDockingPlaces) {
        this.currentNitrogen = nitrogenCapacity;
        this.nitrogenCapacity = nitrogenCapacity;
        this.currentQuantumFluid = quantumFluidCapacity;
        this.quantumFluidCapacity = quantumFluidCapacity;
        this.freeDockingPlaces = maxDockingPlaces;
        this.dockingPlacesCapacity = maxDockingPlaces;
    }
    public void displayStatus(String tag) throws InterruptedException {
        System.out.println(tag);
        System.out.println("     ================================STATION DATA==================================");
        System.out.println("     Nitrogen: " + currentNitrogen + "/" + nitrogenCapacity);
        System.out.println("     Quantum fluid: " + currentQuantumFluid + "/" + quantumFluidCapacity);
        System.out.println("     Free docking places: " + freeDockingPlaces + "/" + dockingPlacesCapacity);
        System.out.println("     ==============================================================================\n");
        Thread.sleep(1000);
    }

    private void action(String name, int nitrogen, int quantumFluid, boolean supplier) {
        if(nitrogen == 0)
            System.out.println(name + (supplier ? " supplying " : " getting ") + quantumFluid +  "quantum fluid.");
        else if(quantumFluid == 0)
            System.out.println(name + (supplier ? " supplying " : " getting ") + nitrogen +  "nitrogen.");
        else
            System.out.println(name + (supplier ? " supplying " : " getting ") + nitrogen + " nitrogen and " + quantumFluid + " quantum fluid.");
    }
    public void enterStation(String name) throws InterruptedException {
        System.out.println(name + " arriving at the station.");
    }
    public synchronized void requestFuel(String name, int nitrogenNeeded, int quantumFluidNeeded, boolean supplier) throws InterruptedException {
        while (currentNitrogen < nitrogenNeeded ||
                currentQuantumFluid < quantumFluidNeeded ||
                (freeDockingPlaces == 0 && !supplier)) {
            System.out.println(name + " WAITING to request fuel");
            wait();
        }

        action(name, nitrogenNeeded, quantumFluidNeeded, false);

        currentNitrogen -= nitrogenNeeded;
        currentQuantumFluid -= quantumFluidNeeded;

        // If it's a supplier, it never left, so the number of free docking places remains the same.
        if(!supplier)
            freeDockingPlaces--;

        displayStatus(name + " modified the station's resources:");
        Thread.sleep(1000);
    }
    public synchronized void supplyFuel(String name, int nitrogenSupplied, int quantumFluidSupplied) throws InterruptedException {
        while (currentNitrogen + nitrogenSupplied > nitrogenCapacity ||
                currentQuantumFluid + quantumFluidSupplied > quantumFluidCapacity ||
                freeDockingPlaces == 0) {
            System.out.println(name + " WAITING to supply fuel...");
            wait();
        }

        action(name, nitrogenSupplied, quantumFluidSupplied, true);

        currentNitrogen += nitrogenSupplied;
        currentQuantumFluid += quantumFluidSupplied;
        freeDockingPlaces--;

        displayStatus(name  + " modified the station's resources:");
    }
    public synchronized void leaveStation(String name) throws InterruptedException {
        System.out.println(name + " leaving the station.");
        freeDockingPlaces++;
        displayStatus("Status after " + name + " has left:");
        notifyAll();
    }
}