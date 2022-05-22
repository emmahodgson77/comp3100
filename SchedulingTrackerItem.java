public class SchedulingTrackerItem {

    String jobID;
    int serverID;

    String serverType;
    int submissionTime;
    int waitTime;
    int startTime;
    int timeJobWillComplete;
    int turnAroundTime;

    public SchedulingTrackerItem() {
    }

    public SchedulingTrackerItem(String jobID, String serverType, int serverID, int submissionTime, int waitTime, int startTime, int timeJobWillComplete, int turnAroundTime) {
        this.jobID = jobID;
        this.serverType = serverType;
        this.serverID = serverID;
        this.submissionTime = submissionTime;
        this.waitTime = waitTime;
        this.startTime = startTime;
        this.timeJobWillComplete = timeJobWillComplete;
        this.turnAroundTime = turnAroundTime;
    }



    void setStartTime(){
        this.startTime = this.submissionTime+this.waitTime;
    }

    public void setCalculatedTimes(int timeJobRequiresToComplete) {
        setStartTime();
        this.timeJobWillComplete = this.startTime+timeJobRequiresToComplete;
        this.turnAroundTime = timeJobRequiresToComplete+this.waitTime;
    }


}
