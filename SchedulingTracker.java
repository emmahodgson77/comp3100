import java.util.ArrayList;
import java.util.List;

public class SchedulingTracker {

    SchedulingTrackerItem[] items;

    public int getQueuedJobCountForServer(String serverType, int serverID, int submissionTime){
        int count =0;
        for(SchedulingTrackerItem item:this.items){
            if(item.serverID == serverID && item.serverType.equalsIgnoreCase(serverType) && submissionTime<=item.timeJobWillComplete){

                count++;
            }
        }
        return count;
    }
    List<ServerState> servers = new ArrayList<>();

}
