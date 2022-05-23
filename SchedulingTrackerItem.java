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
    int coresAvailable;
    int turnAroundTime;

    public SchedulingTrackerItem() {
    }
//
//    jobID jobState submitTime startTime estRunTime core memory disk.


    public SchedulingTrackerItem(String jobID, String serverType, int serverID, int submissionTime, int waitTime, int startTime, int endTime, int coresRequiredForJob, int turnAroundTime) {
        this.jobID = jobID;
        this.serverType = serverType;
        this.serverID = serverID;
        this.submissionTime = submissionTime;
        this.waitTime = waitTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.coresRequiredForJob = coresRequiredForJob;
        this.turnAroundTime = turnAroundTime;
    }


    void setCoresAvailable(int cores){
        this.coresAvailable = cores;
    }
    void setStartTime(){
        this.startTime = this.submissionTime+this.waitTime;
    }

    public void setCalculatedTimes(int timeJobRequiresToComplete) {
        setStartTime();
        this.endTime = this.startTime+timeJobRequiresToComplete;
        this.turnAroundTime = timeJobRequiresToComplete+this.waitTime;
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
