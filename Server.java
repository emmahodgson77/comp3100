import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Server implements Comparable{
//jobID jobState submitTime startTime estRunTime core memory disk.
        public String type;
        int serverID;
        String state;
        int startUpTime;
        int coreCount;
        int memory;
        int disk;
        int numberOfWaitingJobs;
        int numberOfRunningJobs;
        int waitTime;
    private List<SchedulingTrackerItem> jobs;

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
        }


    public int getCoreCount() {
        return coreCount;
    }

    public int getNumberOfWaitingJobs() {
        return numberOfWaitingJobs;
    }

    @Override
    public int compareTo(Object o) {
            return Comparator.comparing(Server::getCoreCount).reversed() //best fit sorting largest core count to smallest
                    .thenComparing(Server::getWaitTime)
                    .compare(this, (Server) o);
    }

    public int compareWaitTimes(Object o){
            return Comparator.comparing(Server::getWaitTime)
                    .compare(this,(Server) o);
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getWaitTime(){
            return this.waitTime;
    }

    public void incrementNumberOfRunningJobs() {
            this.numberOfRunningJobs++;
    }

    public void setJobs(List<SchedulingTrackerItem> jobs) {
        this.jobs = jobs;
        SchedulingTrackerItem first = jobs.get(0);
        first.setCoresAvailable(this.coreCount-first.getCoresRequiredForJob());
        for(int i=1;i<jobs.size();i++){
            SchedulingTrackerItem current = jobs.get(i);
            //loop through all jobs and subtract all "required core counts" of jobs that haven't completed before current job's start time
            Collections.sort(jobs); //sort by startTime of each job - only loop up until start time of current job (anything starting after this job will not take cores away)
            for(int j=0;j<jobs.size();j++) {
                SchedulingTrackerItem compare = jobs.get(j);
                if (current.startTime < compare.endTime) { //if previous job hasn't released cores yet, subtract
                    current.setCoresAvailable(this.coreCount - compare.coresAvailable - current.getCoresRequiredForJob());
                }
            }
        }
    }

    public int getAvailableCores(int submitTime) {
        SchedulingTrackerItem lastJob = this.jobs.get(this.jobs.size() - 1);
        if(lastJob.endTime<0 || lastJob.endTime>submitTime){
            return lastJob.coresAvailable;
        }else{
            return this.coreCount;
        }
    }
}
