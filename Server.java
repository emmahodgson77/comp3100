import java.util.Comparator;
import java.util.List;

public class Server implements Comparable {
    public String type;
    int serverID;
    String state;
    int startUpTime;
    private int coreCount;
    int memory;
    int disk;
    int numberOfWaitingJobs;
    int numberOfRunningJobs;
    int waitTime;
    List<SchedulingTrackerItem> jobs;
    int availableCores;
    int originalCoreCount;

    public Server(String type, int serverID, String state, int startUpTime, int coreCount, int memory, int disk, int numberOfWaitingJobs, int numberOfRunningJobs) {
        this.type = type;
        this.serverID = serverID;
        this.state = state;
        this.startUpTime = startUpTime;
        this.coreCount = coreCount;
        this.memory = memory;
        this.disk = disk;
        this.numberOfWaitingJobs = numberOfWaitingJobs;
        this.numberOfRunningJobs = numberOfRunningJobs;
        this.availableCores = coreCount;
    }


    public int getCoreCount() {
        return coreCount;
    }

    public void setOriginalCoreCount(int cores){
        this.originalCoreCount = cores;
    }

    public int getOriginalCoreCount(){
        return originalCoreCount;
    }
    @Override
    public int compareTo(Object o) {
        return Comparator.comparing(Server::getStatus)
                .thenComparing(Server::getWaitTime)
                .thenComparing(Server::getCoreCount)
                .thenComparing(Server::getAvailableCores)
                .compare(this, (Server) o);
    }

    public int getStatus(){
        //status returns are based on preference for sorting
        switch (this.state){
            case "inactive":return 3;
            case "idle":return 0;
            case "booting": return 1;
            case "active":return 2;
            default: return 4;
        }
    }


    public int compareWaitTimes(Object o) {
        return Comparator.comparing(Server::getWaitTime)
                .compare(this, (Server) o);
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public void setJobs(List<SchedulingTrackerItem> jobs) {
        this.jobs = jobs;
    }

    public int getAvailableCores() {
        return this.availableCores;
    }

    private void setCoreCount(int coresAvailable) {
        this.coreCount = coresAvailable;
    }


    public void setAvailableCores(int serverCoresAvailable) {
        this.availableCores = serverCoresAvailable;
    }
}
