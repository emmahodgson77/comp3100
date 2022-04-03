import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DSClient {
    public static final String NEWLINE_CHAR = "\n";
    public static final String AUTH_EMMA = "AUTH emma" + NEWLINE_CHAR;
    public static final String SCHD = "SCHD";
    public static String DS_SERVER_FILEPATH = "/home/emma/ds-sim/src/pre-compiled/ds-system.xml";
    private static int serverid = 0;
    private static int maxCores = 0;
    private static String maxCoreServerType = null;

    public static void main(String[] args) throws Exception {
        int PORT = 50000;
        boolean serverStatesFound = false;
        try {
            Socket s = new Socket("127.0.0.1", PORT);

            DataInputStream din = new DataInputStream(s.getInputStream());
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());

            String dsMsg = "";

            //say hello (initiate comms)
            dout.write("HELO\n".getBytes());
            dout.flush();
            System.out.println(din.readLine());

            //send auth
            dout.write(AUTH_EMMA.getBytes());
            dout.flush();
            List<ServerState> biggestServers = new ArrayList<>();

            dsMsg = din.readLine();
            List<Integer> jobIdsScheduled = new ArrayList<>();
            while (!dsMsg.equals("NONE")) {

                //send REDY - find JBN and parse to determine DATA
                dout.write("REDY\n".getBytes());
                dout.flush();

                String jobMessage = din.readLine();
                System.out.println(jobMessage);
                if (jobMessage.equalsIgnoreCase("NONE")) {
                    dsMsg = jobMessage;
                } else {
                    String[] jobDetails = jobMessage.split("\\s");

                    //if we haven't already retrieved available servers, do so now
                    if (!serverStatesFound) {
                        String getsCall = "GETS All\n";
                        dout.write(getsCall.getBytes());
                        dout.flush();
                        dsMsg = din.readLine(); //DATA 5 124
                        String[] dsServerInfo = dsMsg.split("\\s");
                        Integer numberOfRecords = Integer.valueOf(dsServerInfo[1]);

                        //send ok to get server state list
                        dout.write("OK\n".getBytes());
                        dout.flush();

                        List<ServerState> dsServerList = new ArrayList<>();
                        //iterate through all server records returned by server, parsing to convert to ServerState object so we can inspect the number of cores easily
                        for (int i = 0; i < numberOfRecords; i++) {
                            //add each server to list
                            String[] serverInfo = din.readLine().split("\\s");
                            ServerState server = new ServerState(
                                    serverInfo[0],
                                    Integer.parseInt(serverInfo[1]), //ID
                                    serverInfo[2], //state
                                    serverInfo[3], //starttime
                                    Integer.parseInt(serverInfo[4]), //core
                                    Integer.parseInt(serverInfo[5]), //memory
                                    Integer.parseInt(serverInfo[6])); //disk
                            dsServerList.add(server);
                        }
                        //sort the available servers so that those with the most cores are at the start of the list
                        Collections.sort(dsServerList);
                        //now we know the first list element has the largest core count - assign this server type and core count to our static 'holder' variables for comparison
                        maxCores = dsServerList.get(0).core;
                        maxCoreServerType = dsServerList.get(0).type;
                        biggestServers = new ArrayList<>();
                        //iterate through the servers to find all that match the biggest core count of same type as first server type (in list received) with largest core count
                        for (ServerState serverState : dsServerList) {
                            if (serverState.core == maxCores && serverState.type.equalsIgnoreCase(maxCoreServerType))
                                biggestServers.add(serverState);
                        }
                        //send ok
                        dout.write("OK\n".getBytes());
                        dout.flush();
                        dsMsg = din.readLine();
                        serverStatesFound = true;
                    }

                    //schedule job
                    Integer jobID = Integer.parseInt(jobDetails[2]);

                    if (!jobIdsScheduled.contains(jobID)) {
                        //if we've used all the biggest servers, restart from 0
                        if (serverid == biggestServers.size()) serverid = 0;

                        jobIdsScheduled.add(jobID);
                        String schCommand = SCHD + " " + jobID + " " + maxCoreServerType + " " + serverid + NEWLINE_CHAR;
                        serverid++;
                        dout.write(schCommand.getBytes());
                        dout.flush();
                        dsMsg = din.readLine();
                    }
                }
            }

            dout.write("OK\n".getBytes());
            dout.flush();


            //send QUIT
            dout.write("QUIT\n".getBytes(StandardCharsets.UTF_8));
            dout.flush();
            dsMsg = din.readLine();
            while (!dsMsg.equalsIgnoreCase("QUIT")) {
                dsMsg = din.readLine();
            }
            dout.write("OK\n".getBytes());
            dout.flush();

            dout.close();
            s.close();


        } catch (Exception e) {
            System.out.println("something wend wrong: " + e.getMessage());
        }
    }


}



