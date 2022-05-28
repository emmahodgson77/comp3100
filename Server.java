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
    private List<SchedulingTrackerItem> jobs;
    int availableCores;

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

    @Override
    public int compareTo(Object o) {
        return Comparator.comparing(Server::getCoreCount).reversed() //worst fit sorting: largest core count to smallest
//                .thenComparing(Server::getWaitTime) //then by smallest waitTime to minimise TurnAroundTime
                .compare(this, (Server) o);
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

    public int getAvailableCores(int submitTime) {
        int remainingCores = this.coreCount;
        if (this.jobs != null){
            for(SchedulingTrackerItem item: this.jobs){
                if(item.endTime>submitTime){
                    remainingCores -= item.coresRequiredForJob;
                }
            }
        }
        //if no jobs running return full core count of this server
        return remainingCores;
    }

    private void setCoreCount(int coresAvailable) {
        this.coreCount = coresAvailable;
    }

    public void setAvailableCores(int submitTime) {
        this.availableCores = getAvailableCores(submitTime);
    }


}
