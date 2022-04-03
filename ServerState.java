package src.main.java;

public class ServerState implements Comparable{

        String type;
        int serverID;
        String state;
        String startTime;
        int core;
        int memory;
        int disk;

        public ServerState(String type, int serverID, String state, String startTime, int core, int memory, int disk) {
            this.type = type;
            this.serverID = serverID;
            this.state = state;
            this.startTime = startTime;
            this.core = core;
            this.memory = memory;
            this.disk = disk;
        }


    @Override
    public int compareTo(Object o) {
            ServerState s =(ServerState)o;
        return Integer.compare(s.core,this.core);
    }


}
