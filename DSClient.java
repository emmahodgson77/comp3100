import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
                    List<ServerState> dsServerList = generateServerList(din,numberOfRecords);

                    //send ok
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();
                    din.readLine(); //do nothing with response from server (should be "OK"): optimise: handle case when not OK

                    String serverType = null;
                    int serverid = 0;

                    //get number of available cores per server to determine smallest / biggest available cores
                    for (ServerState ser : dsServerList) {
                        String runningJobsquery = "LSTJ" + ser.type + " " + ser.serverID + "\n";
                        dout.write(runningJobsquery.getBytes(StandardCharsets.UTF_8));
                        dout.flush();
                        String dataMsg = din.readLine();
                        List<ServerState> lstis = generateServerList(din,Integer.parseInt(dataMsg.split(" ")[1]));
                    }

                    //get estimated wait time for each server
                    for (ServerState ser : dsServerList) {
                        String timeCommand = "EJWT " + ser.type + " " + ser.serverID + "\n";
                        dout.write(timeCommand.getBytes(StandardCharsets.UTF_8));
                        dout.flush();
                        int waitTime = Integer.parseInt(din.readLine());
                        ser.setWaitTime(waitTime);
                    }
                    //sort the list based on alg preferences(bf,wf) then wait time to optimise (implemented in compareTo)
                    Collections.sort(dsServerList);
                    serverType = dsServerList.get(0).type;
                    serverid = dsServerList.get(0).serverID;

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

    public static List<ServerState> generateServerList(DataInputStream din, int numberOfRecords) throws IOException {
        List<ServerState> dsServerList = new ArrayList<>();
        for (int i = 0; i < numberOfRecords; i++) {
            //add each server to list
            String[] serverInfo = din.readLine().split("\\s");
            ServerState server = getServerState(serverInfo);
            dsServerList.add(server);
        }
        return dsServerList;
    }

    public static ServerState getServerState(String[] serverInfo) {
        return new ServerState(
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



