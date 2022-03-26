package src.main.java;

public class Server implements Comparable<Server>{
    String type;
    int limit; //number of servers at the site
    double hourlyRate;
    int availableCores;
    int availableMemory;
    int availableDiskSpace;

    public Server(String type, int limit, double hourlyRate, int availableCores, int availableMemory, int availableDiskSpace) {
        this.type = type;
        this.limit = limit;
        this.hourlyRate = hourlyRate;
        this.availableCores = availableCores;
        this.availableMemory = availableMemory;
        this.availableDiskSpace = availableDiskSpace;
    }

    @Override
    public int compareTo(Server s) {
        return Integer.compare(this.availableCores,s.availableCores);
    }
}
