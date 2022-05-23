import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DSClient {
    public static final String NEWLINE_CHAR = "\n";
    public static final String AUTH_EMMA = "AUTH emma" + NEWLINE_CHAR;
    public static final String SCHD = "SCHD";

    public static void main(String[] args) {
        String arg = args[1];


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
            System.out.println(din.readLine());

            //send auth
            dout.write(AUTH_EMMA.getBytes(StandardCharsets.UTF_8));
            dout.flush();

            dsMsg = din.readLine(); //initially we just want this server response to be not "NONE" in order to use the value to initiate our while condition
//            List<Integer> jobIdsScheduled = new ArrayList<>();//todo remove

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

                    String query = "GETS Capable " + coreCountRequired + " " + memoryRequired + " " + diskRequired + "\n";
                    dout.write(query.getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    //capture the servers information response from ds server
                    dsMsg = din.readLine(); //eg. DATA 5 124
                    String[] dsServerInfo = dsMsg.split("\\s");

                    //parse the number of capable servers to add to our list
                    int numberOfRecords = Integer.parseInt(dsServerInfo[1]);

                    //send ok to get server details
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    //iterate through all server records returned by server, parsing to convert to ServerState object for convenience
                    List<Server> dsServerList = generateServerList(din, numberOfRecords);

                    //send ok
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();
                    din.readLine(); //do nothing with response from server (should be "OK"): optimise: handle case when not OK

                    String serverType = null;
                    int serverid = 0;

                    //get number of available cores per server to determine smallest / biggest available cores

                    for (Server ser : dsServerList) {
                        String runningJobsquery = "LSTJ" + ser.type + " " + ser.serverID + "\n";
                        dout.write(runningJobsquery.getBytes(StandardCharsets.UTF_8));
                        dout.flush();
                        String dataMsg = din.readLine();

                        dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                        dout.flush();

                        List<SchedulingTrackerItem> lstis = getRunningWaitingJobs(
                                din,
                                Integer.parseInt(dataMsg.split(" ")[1]),
                                ser.type,
                                ser.serverID);
                        ser.setJobs(lstis);
                    }

                    //get estimated wait time for each server
                    for (Server ser : dsServerList) {
                        String timeCommand = "EJWT " + ser.type + " " + ser.serverID + "\n";
                        dout.write(timeCommand.getBytes(StandardCharsets.UTF_8));
                        dout.flush();
                        int waitTime = Integer.parseInt(din.readLine());
                        ser.setWaitTime(waitTime);
                    }

                    //sort the list based on best fit algorithm
                    Collections.sort(dsServerList, Server::compareTo);
                    //see if enough cores in best fit server
                    for (Server ser : dsServerList) {
                        if (ser.getAvailableCores(submitTime) >= coreCountRequired) {
                            serverType = ser.type;
                            serverid = ser.serverID;
                        }
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

    //jobID jobState submitTime startTime estRunTime core memory disk.
    public static List<SchedulingTrackerItem> getRunningWaitingJobs(DataInputStream din, int numberOfItems, String serverType, int serverID) throws IOException {
        List<SchedulingTrackerItem> items = new ArrayList<>();
        for (int i = 0; i < numberOfItems; i++) {
            String[] deets = din.readLine().split("\s");
            int startTime = Integer.parseInt(deets[3]);
            items.add(new SchedulingTrackerItem(String.valueOf(deets[0]),
                    serverType,
                    serverID,
                    Integer.parseInt(deets[2]), //submitTime
                    (startTime > -1 ? (startTime - Integer.parseInt(deets[2])) : -1), //waitTime
                    startTime, //startTime
                    Integer.parseInt(deets[4]), //estRunTime
                    Integer.parseInt(deets[5]), //core
                    (startTime + Integer.parseInt(deets[4])) //turnAround
            ));
        }
        return items;
    }

    public static Server getServerState(String[] serverInfo) {
        return new Server(
                serverInfo[0],
                Integer.parseInt(serverInfo[1]), //ID
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



