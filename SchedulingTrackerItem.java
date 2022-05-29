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
    int estimatedRunTime;
    int state;

//
//    jobID jobState submitTime startTime estRunTime core memory disk.

    //                SchedulingTrackerItem job = new SchedulingTrackerItem(startTime,submissionTime,estimatedRunTime,coresRequiredForJob,endTime);
    public SchedulingTrackerItem(int startTime, int submissionTime, int estimatedRunTime, int coresRequiredForJob, int state) {
        this.submissionTime = submissionTime;
        this.startTime = startTime;
        this.coresRequiredForJob = coresRequiredForJob;
        this.estimatedRunTime = estimatedRunTime;
        this.state = state;
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
