import java.util.Comparator;

public class SchedulingTrackerItem implements Comparable{

    String jobID;
    int serverID;

    String serverType;
    int submissionTime;
    int waitTime;
    int startTime;
    int endTime;
    int coresRequiredForJob;
    int remainingCoresAvailable;
    int turnAroundTime;

//
//    jobID jobState submitTime startTime estRunTime core memory disk.


    public SchedulingTrackerItem(String jobID, String serverType, int serverID, int submissionTime, int waitTime, int startTime, int endTime, int coresRequiredForJob, int remainingAvailableCores, int turnAroundTime) {
        this.jobID = jobID;
        this.serverType = serverType;
        this.serverID = serverID;
        this.submissionTime = submissionTime;
        this.waitTime = waitTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.coresRequiredForJob = coresRequiredForJob;
        this.remainingCoresAvailable = remainingAvailableCores;
        this.turnAroundTime = turnAroundTime;
    }

    void setStartTime(){
        this.startTime = this.submissionTime+this.waitTime;
    }

    public int getCoresRequiredForJob() {
        return coresRequiredForJob;
    }

    public int getStartTime(){
        return startTime;
    }
    @Override
    public int compareTo(Object o) {
        return Comparator.comparing(SchedulingTrackerItem::getStartTime)
                .compare(this, (SchedulingTrackerItem) o);
    }
}
