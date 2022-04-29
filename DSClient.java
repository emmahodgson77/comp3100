import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DSClient {
    public static final String NEWLINE_CHAR = "\n";
    public static final String AUTH_EMMA = "AUTH emma" + NEWLINE_CHAR;
    public static final String SCHD = "SCHD";
    private static int serverid = 0;
    private static int maxCores = 0;
    private static String serverType = null;

    public static void main(String[] args) throws Exception {
        int PORT = 50000;
        boolean serverStatesFound = false;
        try {
            //make a socket connection with ds-server
            Socket s = new Socket("127.0.0.1", PORT);

            DataInputStream din = new DataInputStream(s.getInputStream());
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());

            String dsMsg = "";

            //initiate and authenticate

            //say hello (initiate comms)
            dout.write("HELO\n".getBytes(StandardCharsets.UTF_8));
            dout.flush();
            System.out.println(din.readLine());

            //send auth
            dout.write(AUTH_EMMA.getBytes(StandardCharsets.UTF_8));
            dout.flush();

            dsMsg = din.readLine(); //initially we just want this server response to be not "NONE" in order to use the value to initiate our while condition
            List<Integer> jobIdsScheduled = new ArrayList<>();

            while (!dsMsg.equals("NONE")) {
                //send REDY
                dout.write("REDY\n".getBytes(StandardCharsets.UTF_8));
                dout.flush();
                dsMsg = din.readLine();

                //if Job sent by ds-server is a JOBN, capture details & schedule
                if (dsMsg.contains("JOBN")) {
                    String[] jobDetails = dsMsg.split("\\s");

                    String getsCapable = "GETS Capable " + jobDetails[4] + " " + jobDetails[5] + " " + jobDetails[6] + "\n";

                    dout.write(getsCapable.getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    //capture the capable servers information response from ds server
                    dsMsg = din.readLine(); //eg. DATA 5 124
                    String[] dsServerInfo = dsMsg.split("\\s");

                    //parse the number of capable servers to add to our list
                    Integer numberOfRecords = Integer.valueOf(dsServerInfo[1]);

                    //send ok to get server details
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();

                    List<ServerState> dsServerList = new ArrayList<>();
                    //iterate through all server records returned by server, parsing to convert to ServerState object for convenience
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

                    //send ok
                    dout.write("OK\n".getBytes(StandardCharsets.UTF_8));
                    dout.flush();
                    din.readLine(); //do nothing with response from server (should be "OK"): optimise: handle case when not OK

                    //use first capable server in list to schedule job
                    serverType = dsServerList.get(0).type;
                    serverid = dsServerList.get(0).serverID;

                    //schedule job
                    Integer jobID = Integer.parseInt(jobDetails[2]);

                    if (!jobIdsScheduled.contains(jobID)) {
                        jobIdsScheduled.add(jobID);
                        String schCommand = SCHD + " " + jobID + " " + serverType + " " + serverid + NEWLINE_CHAR;
                        dout.write(schCommand.getBytes(StandardCharsets.UTF_8));
                        dout.flush();
                        din.readLine(); //do nothing with response from server (should be "OK"): optimise: handle case when not OK
                    }
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


        } catch (Exception e) {
            System.out.println("something wend wrong: " + e.getMessage());
        }
    }


}



