import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

                    List<ServerState> dsServerList = new ArrayList<>();
                    //iterate through all server records returned by server, parsing to convert to ServerState object for convenience
                    for (int i = 0; i < numberOfRecords; i++) {
                        //add each server to list
                        String[] serverInfo = din.readLine().split("\\s");
                        ServerState server = getServerState(serverInfo);
                        dsServerList.add(server);
                    }

                    //send ok
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();
                    din.readLine(); //do nothing with response from server (should be "OK"): optimise: handle case when not OK

                    //todo determine which alg to apply: ff, bf, wf
                    Comparator<ServerState> c = Comparator.comparing(ServerState::getCore);
                    if (arg != null && !arg.isEmpty()) {
                        c = switch (arg) {
                            case "wf" -> c.reversed();
                            case "ff" -> null;
                            default -> Comparator.comparing(ServerState::getCore);
                        };
                    }

                    String serverType = null;
                    int serverid = 0;

                    //determine best availability, applying the comparitor for sorting based on args passed (ff, bf, wf)
                    if (c != null) Collections.sort(dsServerList, c); //if c is null, no sort (use first fit)

                    SchedulingTrackerItem tracker = new SchedulingTrackerItem();
                    ServerState server = null;
                    for (ServerState ss : dsServerList) {
                        if (ss.numberOfRunningJobs == 0) {
                            server = ss; //if no jobs running use this server
                            break;
                        }
                    }
                    if (server == null) { // server wasn't selected because all servers have jobs running
                        //get estimated wait time for each server
                        for (ServerState ser : dsServerList) {
                            String timeCommand = "EJWT " + ser.type + " " + ser.serverID + "\n";
                            dout.write(timeCommand.getBytes(StandardCharsets.UTF_8));
                            dout.flush();
                            ser.setWaitTime(Integer.parseInt(din.readLine()));
                        }

                        //sort servers by how many jobs are queued
                        c = switch (arg) {
                            case "bf" -> Comparator.comparing(ServerState::getCore).thenComparing(ServerState::getWaitTime);
                            case "wf" -> Comparator.comparing(ServerState::getCore).reversed().thenComparing(ServerState::getWaitTime);
                            default -> Comparator.comparing(ServerState::getWaitTime);//ff will apply
                        };
                        Collections.sort(dsServerList, c);
                        server = dsServerList.get(0);
                    }
                    //set params for SCHD command
                    serverid = server.serverID;
                    serverType = dsServerList.get(0).type;

                    //track server and job resources
                    tracker.waitTime = server.getWaitTime();
                    tracker.submissionTime = submitTime;
                    tracker.serverID = serverid;
                    tracker.serverType = serverType;
                    tracker.jobID = jobID;
                    tracker.setCalculatedTimes(estRuntime);
                    trackers.add(tracker);

                   /* if(serverType == null || serverType.isBlank()){

                        //sort by least number of waiting jobs
                        Collections.sort(dsServerList, Comparator.comparing(ServerState::getNumberOfWaitingJobs));
                        serverType = dsServerList.get(0).type;
                        serverid = dsServerList.get(0).serverID;
                    }*/
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
            System.out.println("something wend wrong: " + e.getMessage());
        }

    }

    /*static void getAlgType(){
        if (arg != null && !arg.isEmpty()) {
            switch (arg) {
                case "fc":
                    applyFirstFitSorting();
                    break;
                case "bf":
                    applyBestFitSorting();
                    break;
                case "wf":
                    applyWorstFitSorting();
                    break;
            }
        } else{
            applyFirstFitSorting();
        }
    }*/
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



