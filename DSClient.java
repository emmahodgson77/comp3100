import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DSClient {
    public static final String NEWLINE_CHAR = "\n";
    public static final String AUTH_EMMA = "AUTH emma" + NEWLINE_CHAR;
    public static final String SCHD = "SCHD";
    static DataInputStream din;
    static DataOutputStream dout;
    static List<Server> originalServers;

    public static void main(String[] args) {

        int PORT = 50000;
        try {
            //make a socket connection with ds-server
            Socket s = new Socket("127.0.0.1", PORT);

            din = new DataInputStream(s.getInputStream());
            dout = new DataOutputStream(s.getOutputStream());

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

            //send REDY
            dout.write("REDY\n".getBytes(StandardCharsets.UTF_8));
            dout.flush();
            dsMsg = din.readLine();
            //get original server list
            originalServers = getOriginalServerStates();

            while (!dsMsg.equals("NONE")) {
                //send REDY
                dout.write("REDY\n".getBytes(StandardCharsets.UTF_8));
                dout.flush();
                dsMsg = din.readLine();

                //if Job sent by ds-server is a JOBN, capture details & schedule
                if (dsMsg.contains("JOBN")) {
                    Job job = parseJob(dsMsg);

                    //send server a message to get a list of servers capable of running the job
                    String query = "GETS Capable " + job.coreCountRequired + " " + job.memoryRequired + " " + job.diskRequired + "\n";
                    dout.write(query.getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    //parse the response to find the number of capable servers
                    int numberOfRecords = Integer.parseInt(din.readLine().split("\\s")[1]);

                    //send ok to get servers (list of all capable)
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    List<Server> dsServerList = generateServerList(numberOfRecords);

                    //send ok
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();
                    din.readLine();

                    String serverType;
                    int serverid;
                    boolean serverFound = false;

                    //sort servers by cores available, smallest servers to biggest to minimise cost
//                    dsServerList.sort(Server::compareTo);
                    Server bestSever = dsServerList.get(0);
                    //run through available servers to get smallest server without jobs running/waiting
                    for (Server server : dsServerList) {
                        if (serverIsFree(server)) {
                            bestSever = server;
                            serverFound = true;
                            break;
                        }else{
                            if (availableCoresBySubmissionTime(server)) {
                                bestSever = server;
                                serverFound = true;
                                break;
                            }
                        }
                    }
                    if (!serverFound) {
                        //all servers are already busy - work out which has most available cores by submission time
                        bestSever = getBestFitServerByAvailableCores(dsServerList, job.submitTime);
                    }
                    serverid = bestSever.serverID;
                    serverType = bestSever.type;

                    //schedule job
                    String schCommand = SCHD + " " + job.jobID + " " + serverType + " " + serverid + NEWLINE_CHAR;
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

    private static List<Server> getOriginalServerStates() throws IOException {
        //send server a message to get a list of servers capable of running the job
        String query = "GETS All\n";
        dout.write(query.getBytes(StandardCharsets.UTF_8));
        dout.flush();

        //parse the response to find the number of capable servers
        int numberOfRecords = Integer.parseInt(din.readLine().split("\\s")[1]);

        //send ok to get servers (list of all capable)
        dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
        dout.flush();
        List<Server> servers = generateServerList(numberOfRecords);

        //send ok
        dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
        dout.flush();
        din.readLine();
        return servers;
    }

    private static Job parseJob(String dsMsg) {
        String[] jobDetails = dsMsg.split("\\s");
        int submitTime = Integer.parseInt(jobDetails[1]);
        String jobID = jobDetails[2];
        int estRuntime = Integer.parseInt(jobDetails[3]);
        int coreCountRequired = Integer.parseInt(jobDetails[4]);
        int memoryRequired = Integer.parseInt(jobDetails[5]);
        int diskRequired = Integer.parseInt(jobDetails[6]);
        return new Job(jobID, submitTime, estRuntime, coreCountRequired, memoryRequired, diskRequired);

    }

    public static void setScheduledJobsForServer(Server server) throws IOException {
        List<SchedulingTrackerItem> jobs = new ArrayList<>();
        String runningJobsquery = "LSTJ " + server.type + " " + server.serverID + "\n";
        dout.write(runningJobsquery.getBytes(StandardCharsets.UTF_8));
        dout.flush();
        String dataMsg = din.readLine();

        dout.write("OK\n".getBytes(StandardCharsets.UTF_8));//this triggers the job list to be issued by server
        dout.flush();

        int numberOfRunningJobs = Integer.parseInt(dataMsg.split(" ")[1]);
        for (int i = 0; i < numberOfRunningJobs; i++) {
            String s = din.readLine();
            jobs.add(parseRunningJob(s));
        }
        if (numberOfRunningJobs <= 0) {
            String f = din.readLine();
        }else {

            dout.write("OK\n".getBytes(StandardCharsets.UTF_8));//this triggers the job list to be issued by server
            dout.flush();
            String hold = din.readLine();
        }
        server.setJobs(jobs);
    }

    public static boolean availableCoresBySubmissionTime(Server server) throws IOException {
        boolean free = true;
        setScheduledJobsForServer(server);
        int totalWaitingTime = 0;
        for (SchedulingTrackerItem job : server.jobs) {
            if (job.state >= 0) {
                if (job.startTime < job.submissionTime && job.endTime < job.submissionTime) {
                    free = false;
                }
            }
        }
        return free;
    }

    public static SchedulingTrackerItem parseRunningJob(String dataResponse) {
        // server response format: "jobID jobState submitTime startTime estRunTime core memory disk"
        String[] runningJob = dataResponse.split("\\s");
        int state = Integer.parseInt(runningJob[1]);
        int submissionTime = Integer.parseInt(runningJob[2]);
        int startTime = Integer.parseInt(runningJob[3]);
        int estimatedRunTime = Integer.parseInt(runningJob[4]);
        int coresRequiredForJob = Integer.parseInt(runningJob[5]);
        return new SchedulingTrackerItem(startTime, submissionTime, estimatedRunTime, coresRequiredForJob, state);

    }

    public static Server getBestFitServerByAvailableCores(List<Server> servers, int submissionTime) throws IOException {
        setAvailableCoresBasedOnJobs(servers, submissionTime);
        servers.sort(Comparator.comparing(Server::getAvailableCores));
        return servers.get(0);
    }

    private static void setAvailableCoresBasedOnJobs(List<Server> servers, int submissionTime) throws IOException {
        for (Server server : servers) {
            Server originalState = findServer(server);
            setScheduledJobsForServer(server);

            // GETS Capable list returns how many free cores - this may not map to how many total cores the machine has.
            int totalCores = originalState == null ? 0: originalState.getCoreCount();
            int serverCoresAvailable = totalCores;

            for (SchedulingTrackerItem job : server.jobs) {
                totalCores += job.coresRequiredForJob;
                if (job.startTime < 0 || job.endTime > submissionTime) {   //job hasn't started yet - for now mark required cores unavailable TODO make this better
                    serverCoresAvailable -= job.coresRequiredForJob;

                }
            }
            server.setAvailableCores(serverCoresAvailable);
        }
    }

    public static Server findServer(Server server){
        for(Server s: originalServers){
            if(s.serverID==server.serverID && s.type.equalsIgnoreCase(server.type))return s;
        }
        return null;
    }

    public static List<Server> generateServerList(int numberOfRecords) throws
            IOException {
        List<Server> dsServerList = new ArrayList<>();
        for (int i = 0; i < numberOfRecords; i++) {
            //add each server to list
            String[] serverInfo = din.readLine().split("\\s");
            Server server = getServerState(serverInfo);
            dsServerList.add(server);
            if(originalServers!=null&& !originalServers.isEmpty()){
                Server original = findServer(server);
                if(original!=null){
                    server.setOriginalCoreCount(original.getCoreCount());
                }
            }else{
                server.setOriginalCoreCount(server.getCoreCount());
            }
        }
        return dsServerList;
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
    private static void setWaitTimes(List<Server> servers) throws IOException {
        for (Server ser : servers) {
            String timeCommand = "EJWT " + ser.type + " " + ser.serverID + "\n";
            dout.write(timeCommand.getBytes(StandardCharsets.UTF_8));
            dout.flush();
            int waitTime = Integer.parseInt(din.readLine());
            ser.setWaitTime(waitTime);
        }
    }

    private static boolean serverIsFree(Server server) {
        return server.numberOfRunningJobs == 0 && server.numberOfWaitingJobs == 0;
    }
}
