import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DSClient {
    public static final String NEWLINE_CHAR = "\n";
    public static final String AUTH_EMMA = "AUTH emma" + NEWLINE_CHAR;
    public static final String SCHD = "SCHD";

    public static void main(String[] args) {

        int PORT = 50000;
        try {
            //make a socket connection with ds-server
            Socket s = new Socket("127.0.0.1", PORT);

            DataInputStream din = new DataInputStream(s.getInputStream());
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());

            String dsMsg;
            //initiate and authenticate

            //say hello (initiate comms)
            dout.write("HELO\n".getBytes(StandardCharsets.UTF_8));
            dout.flush();
            din.readLine();
            //send auth
            dout.write(AUTH_EMMA.getBytes(StandardCharsets.UTF_8));
            dout.flush();
            dsMsg = din.readLine();

            boolean first = true;
            List<SchedulingTrackerItem> trackers = new ArrayList<>();

            int scheduleCount = -1; //set the count to -1 so we can increment after scheduling a job, and reference this in an array
            while (!dsMsg.equals("NONE")) {
                //send REDY
                dout.write("REDY\n".getBytes(StandardCharsets.UTF_8));
                dout.flush();
                dsMsg = din.readLine();


                //if Job sent by ds-server is a JOBN, capture details & schedule
                if (dsMsg.contains("JOBN")) {
                    String[] jobDetails = dsMsg.split("\\s");
                    int submitTime = Integer.parseInt(jobDetails[1]);
                    String jobID = jobDetails[2];
                    int estRuntime = Integer.parseInt(jobDetails[3]);
                    int coreCountRequired = Integer.parseInt(jobDetails[4]);
                    int memoryRequired = Integer.parseInt(jobDetails[5]);
                    int diskRequired = Integer.parseInt(jobDetails[6]);

                    //send server a message to get a list of servers capable of running the job
                    String query = "GETS Capable " + coreCountRequired + " " + memoryRequired + " " + diskRequired + "\n";
                    dout.write(query.getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    //parse the response to find the number of capable servers
                    dsMsg = din.readLine(); //eg. DATA 5 124
                    String[] capableServersDataResponse = dsMsg.split("\\s");
                    int numberOfRecords = Integer.parseInt(capableServersDataResponse[1]);

                    //send ok to get servers (list of all capable)
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    //iterate through all server records returned by server, parsing to convert to ServerState object for convenience
                    List<Server> dsServerList = generateServerList(din, numberOfRecords);

                    //send ok
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();
                    din.readLine();

                    String serverType = null;
                    int serverid = 0;
                    System.out.println("JOB ID: "+jobID);
                    //sort servers on core count
                    dsServerList.sort(Server::compareTo);
                    for (Server server : dsServerList) {
                        //use biggest servers (by type only) if available core count on any
                        List<Server> biggestServers = new ArrayList<>();
                        for (Server ser : dsServerList) {
                            if (ser.type.equalsIgnoreCase(server.type)) biggestServers.add(ser);
                        }

                        //get jobs running on each server to determine how many cores are currently available
                        setRunningJobs(biggestServers, dout, din, submitTime);

                        /*//now sort all servers based on updated available cores - most to fewest
                            dsServerList.sort(Server::compareTo);//TODO check how many cores are on each server here at job 12
                        */
                        //find first server with enough cores using the returned list of preferred servers (preference based on my algoritm,
                        // favouring worst fit + minimal wait time to minimise turn around time)
                        for (Server ser : dsServerList) {
                            if (ser.getAvailableCores(submitTime) >= coreCountRequired) {
                                serverType = ser.type;
                                serverid = ser.serverID;
                                break;
                            }
                        }

                        if (serverType != null) break;
/*
                            //if we get here, no adequate core count is available in our servers list to run job immediately
                            // my algorithm now decides to schedule the job on whicherver server will next have the available
                            // capacity to run the job based on estimated wait time
                            Collections.sort(dsServerList, Comparator.comparing(Server::getWaitTime));
                            Server server = dsServerList.get(0);
                            serverType = server.type;
                            serverid = server.serverID;*/
                    }
                    if(scheduleCount>=dsServerList.size()-1)scheduleCount=-1;
                    if (serverType == null) {
                        scheduleCount = scheduleCount+1;
                        //use first fit
                        serverid = dsServerList.get(scheduleCount).serverID;
                        serverType = dsServerList.get(scheduleCount).type;

                    }
                    //schedule job
                    String schCommand = SCHD + " " + jobID + " " + serverType + " " + serverid + NEWLINE_CHAR;
                    dout.write(schCommand.getBytes(StandardCharsets.UTF_8));
                    dout.flush();
                    din.readLine(); //do nothing with response from server (should be "OK"): optimise: handle case when not OK
                }
            }

            dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
            dout.flush();

            //send QUIT
            String newline = "QUIT\n";
            dout.write(newline.getBytes(StandardCharsets.UTF_8));
            dout.flush();
            din.readLine();//do nothing with response from server (should be "OK"): optimise: handle case when not OK

            dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
            dout.flush();

            dout.close();
            s.close();
        } catch (
                Exception e) {
            System.out.println("something went wrong: " + e.getMessage());
        }

    }

    public static void setRunningJobs(List<Server> servers, DataOutputStream dout, DataInputStream din, int submitTime) throws IOException {
        for (Server ser : servers) {
            //find the number of jobs currently running or waiting on each server
            String runningJobsquery = "LSTJ " + ser.type + " " + ser.serverID + "\n";
            dout.write(runningJobsquery.getBytes(StandardCharsets.UTF_8));
            dout.flush();
            String dataMsg = din.readLine();

            dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
            dout.flush();

            int numberOfJobsRunning = Integer.parseInt(dataMsg.split(" ")[1]);
            //update servers in the capable list, with details of current running/waiting jobs and available cores per server.

            //Determine which server to schedule job to minimise TurnAroundTime
            List<SchedulingTrackerItem> lstis = setNumberOfAvailableCoresAndWaitTimePerServer(din, numberOfJobsRunning, ser);
            ser.setJobs(lstis);
            ser.setAvailableCores(submitTime);

            if (numberOfJobsRunning <= 0) {
                String f = din.readLine();
            } else {
                dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                dout.flush();
                String msg = din.readLine();
            }
        }
    }

    public static List<Server> generateServerList(DataInputStream din, int numberOfRecords) throws IOException {
        List<Server> dsServerList = new ArrayList<>();
        for (int i = 0; i < numberOfRecords; i++) {
            //add each server to list
            String[] serverInfo = din.readLine().split("\\s");
            Server server = getServerState(serverInfo);
            dsServerList.add(server);
        }
        return dsServerList;
    }

    /* This method uses a returned list of jobs running or waiting on the server, to calculate remaining available cores and track available times/cores for scheduling jobs*/
    public static List<SchedulingTrackerItem> setNumberOfAvailableCoresAndWaitTimePerServer(DataInputStream din, int numberOfItems, Server server) throws IOException {
        List<SchedulingTrackerItem> items = new ArrayList<>();
        if (numberOfItems > 0) {
            for (int i = 0; i < numberOfItems; i++) {
                String s = din.readLine();
                String[] deets = s.split("\\s");
                int startTime = Integer.parseInt(deets[3]);
                String jobID = String.valueOf(deets[0]);
                int submissionTime = Integer.parseInt(deets[2]);
                int estimatedRunTime = Integer.parseInt(deets[4]);
                int coresRequiredForJob = Integer.parseInt(deets[5]);
                int turnAroundTime = startTime + estimatedRunTime;
                int endTime = startTime + estimatedRunTime;

                //if job is waiting, the waitTime is set to -1 (magic number I know) so we can use conditions of '>-1' to
                // determine whether to use wait time in scheduling decisions
                int waitTime = startTime > -1 ? (startTime - submissionTime) : -1;
                //calculate remaining available cores using last SchedulingTrackerItem for the server in question
                int remainingAvailableCores = server.getCoreCount() - coresRequiredForJob;
                if (i > 0) {
                    for (int j = i; j > 0; j--) {
                        SchedulingTrackerItem item = items.get(j - 1);
                        if (item.endTime > submissionTime) {
                            remainingAvailableCores -= item.coresRequiredForJob;
                        }
                    }
                }
                items.add(new SchedulingTrackerItem(
                        jobID,
                        server.type,
                        server.serverID,
                        submissionTime,
                        waitTime, //waitTime
                        startTime, //startTime
                        endTime, //estRunTime
                        coresRequiredForJob, //core
                        remainingAvailableCores,
                        turnAroundTime //turnAround
                ));
            }

        }
        return items;
    }

    public static Server getServerState(String[] serverInfo) {
        String type = serverInfo[0];
        int serverID = Integer.parseInt(serverInfo[1]);
        return new Server(
                type,
                serverID, //ID
                serverInfo[2], //state
                Integer.parseInt(serverInfo[3]), //starttime
                Integer.parseInt(serverInfo[4]), //core
                Integer.parseInt(serverInfo[5]), //memory
                Integer.parseInt(serverInfo[6]),
                Integer.parseInt(serverInfo[7]),
                Integer.parseInt(serverInfo[8])
        );
    }
}
/*
                    //get estimated wait time for each server
                    for (Server ser : dsServerList) {
                        String timeCommand = "EJWT " + ser.type + " " + ser.serverID + "\n";
                        dout.write(timeCommand.getBytes(StandardCharsets.UTF_8));
                        dout.flush();
                        int waitTime = Integer.parseInt(din.readLine());
                        ser.setWaitTime(waitTime);
                    }
*/


