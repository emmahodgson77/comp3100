public class Job {
    String jobID;
    int submitTime;
    int estRunTime;
    int coreCountRequired;
    int memoryRequired;
    int diskRequired;

    public Job(String jobID, int submitTime, int estRunTime, int coreCountRequired, int memoryRequired, int diskRequired) {
        this.jobID = jobID;
        this.submitTime = submitTime;
        this.estRunTime = estRunTime;
        this.coreCountRequired = coreCountRequired;
        this.memoryRequired = memoryRequired;
        this.diskRequired = diskRequired;
    }
}