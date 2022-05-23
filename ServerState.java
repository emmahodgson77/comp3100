import java.util.Comparator;

public class ServerState implements Comparable{

        public String type;
        int serverID;
        String state;
        int startTime;
        int core;
        int memory;
        int disk;
        int numberOfWaitingJobs;
        int numberOfRunningJobs;
        int waitTime;

        public ServerState(String type, int serverID, String state, int startTime, int core, int memory, int disk, int numberOfWaitingJobs, int numberOfRunningJobs) {
            this.type = type;
            this.serverID = serverID;
            this.state = state;
            this.startTime = startTime;
            this.core = core;
            this.memory = memory;
            this.disk = disk;
            this.numberOfWaitingJobs = numberOfWaitingJobs;
            this.numberOfRunningJobs = numberOfRunningJobs;
        }


    public int getCore() {
        return core;
    }

    public int getNumberOfWaitingJobs() {
        return numberOfWaitingJobs;
    }

    @Override
    public int compareTo(Object o) {
            return Comparator.comparing(ServerState::getCore)
                    .thenComparing(ServerState::getWaitTime)
                    .compare(this, (ServerState) o);
    }

    public int compareWaitTimes(Object o){
            return Comparator.comparing(ServerState::getWaitTime)
                    .compare(this,(ServerState) o);
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
}
