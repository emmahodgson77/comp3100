package src.main.java;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DSClient {
    public static final String NEWLINE_CHAR = "\n";
    public static final String AUTH_EMMA = "AUTH emma" + NEWLINE_CHAR;
    public static final String OK = "OK" + NEWLINE_CHAR;
    public static final String SCHD = "SCHD";
    public static String DS_SERVER_FILEPATH = "/home/emma/ds-sim/src/pre-compiled/ds-system.xml";
    private static int serverid = 0;
    private static int maxCores = 0;
    private static String maxCoreServerType = null;

    public static void main(String[] args) throws Exception {
        int PORT = 50305;
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
            while (!dsMsg.equals("NONE")) {
                //send REDY - find JBN and parse to determine DATA
                dout.write("REDY\n".getBytes());
                dout.flush();

                String jobMessage = din.readLine();
                System.out.println(jobMessage);
                if(jobMessage.equalsIgnoreCase("NONE")) {
                    dsMsg = jobMessage;
                }else{
                    String[] jobDetails = jobMessage.split("\\s");


                    //send GETS
//                String getsCall = "GETS Capable " + jobDetails[4] + " " + jobDetails[5] + " " + jobDetails[6] + "\n";
                    if (!serverStatesFound) {
                        String getsCall = "GETS All\n";
                        dout.write(getsCall.getBytes());
                        dout.flush();
                        dsMsg = din.readLine(); //DATA 5 124
                        String[] dsServerInfo = dsMsg.split("\\s");
                        Integer numberOfRecords = Integer.valueOf(dsServerInfo[1]);
                        System.out.println("RECORD COUNT:" + numberOfRecords);
                        System.out.println("gets all response:" + dsMsg);

                        //send ok to get server state list
                        dout.write("OK\n".getBytes());
                        dout.flush();

                        List<ServerState> dsServerList = new ArrayList<>();

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
                            System.out.println("loop " + i + ": " + server.type);
                        }
                        System.out.println("OUT OF LOOP");
                        Collections.sort(dsServerList);
                        maxCores = dsServerList.get(0).core;
                        maxCoreServerType = dsServerList.get(0).type;
                        biggestServers = new ArrayList<>();
                        for (ServerState serverState : dsServerList) {
                            if (serverState.core == maxCores) biggestServers.add(serverState);
                        }
                        //send ok
                        dout.write("OK\n".getBytes());
                        dout.flush();
                        dsMsg = din.readLine();
                        serverStatesFound = true;
                    }

                    //schedule job
                    String schCommand = SCHD + " " + jobDetails[2] + " " + maxCoreServerType + " " + serverid + NEWLINE_CHAR;
                    serverid++;
                    if (serverid > biggestServers.size()) serverid = 0;
                    dout.write(schCommand.getBytes());
                    dout.flush();


                    dsMsg = din.readLine();
                }
            }

            dout.write("OK\n".getBytes());
            dout.flush();
            System.out.println("about to quit");

            //send QUIT
            dout.write("QUIT\n".getBytes());
            dout.flush();

            dout.write("OK\n".getBytes());
            dout.flush();

            dout.close();
            s.close();

        } catch (
                Exception e) {
        }
    }

    public static List<Server> parseDSSystemXML() throws ParserConfigurationException, IOException, SAXException {
        List<Server> dsServers = new ArrayList<Server>();

        File file = new File(DS_SERVER_FILEPATH);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("server");
        System.out.println("server count: " + nodeList.getLength());
        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            Node node = nodeList.item(itr);
            Element e = (Element) node;
            Server s = new Server(
                    e.getAttribute("type"),
                    Integer.parseInt(e.getAttribute("limit")),
                    Integer.parseInt(e.getAttribute("bootupTime")),
                    Double.parseDouble(e.getAttribute("hourlyRate")),
                    Integer.parseInt(e.getAttribute("cores")),
                    Integer.parseInt(e.getAttribute("memory")),
                    Integer.parseInt(e.getAttribute("disk"))
            );
            dsServers.add(s);
            Collections.sort(dsServers);
        }
        return dsServers;
    }


}



