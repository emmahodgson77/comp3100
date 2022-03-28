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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DSClient {
    static String DS_SERVER_FILEPATH="/home/emma/ds-sim/src/pre-compiled/ds-system.xml";

    public static void main(String[] args) throws Exception {
        int PORT = 50305;
        try {
            Socket s = new Socket("127.0.0.1", PORT);

            DataInputStream din = new DataInputStream(s.getInputStream());
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());

            String str = "";

            //say hello (initiate comms)
            dout.write("HELO\n".getBytes());
            dout.flush();
            System.out.println(din.readLine());

            //send auth
            dout.write("AUTH emma\n".getBytes());
            dout.flush();
            System.out.println(din.readLine());

            //send REDY - find JBN and parse to determine DATA
            dout.write("REDY\n".getBytes());
            dout.flush();
            str = din.readLine();
            System.out.println(str);
            String[] jbnParts = str.split("\\s");
            String JBN = jbnParts[4] + " " + jbnParts[5] + " " + jbnParts[6];
            String getsCommand = "GETS Capable " + JBN + "\n";

            //parse system info xml
            List<Server> servers = parseDSSystemXML();
            //send GETS
            dout.write(getsCommand.getBytes());
            dout.flush();
            str = din.readLine();
            System.out.println("from gets command:" + str);
            String[] capableServers = str.split("\\s");
            Integer jobCount = Integer.valueOf(capableServers[1]);
//while there are still jobs to be scheduled, loop
//            while()
            dout.write("OK\n".getBytes());
            dout.flush();
            str = din.readLine();
            System.out.println("ok after gets: " + str);
            dout.write("OK\n".getBytes());
            dout.flush();
            System.out.println(jobCount);
            //loop through number of available jobss
            for (int i = 0; i < jobCount; i++) {
                str = din.readLine();
                System.out.println(i + ":" + str + "_____\n");
                if (str.length() > 1) {
                    String[] jobDetails = str.split("\\s");
                    String schCommand = "SCHD " + jobDetails[1] + " " + jobDetails[0] + " " + jobDetails[8] + "\n";
                    System.out.println(schCommand + "\n");
                    dout.write(schCommand.getBytes());
                    dout.flush();
                    dout.write("OK\n".getBytes());
                    dout.flush();
                    dout.write("OK\n".getBytes());
                    dout.flush();
                }
            }
            dout.write("OK\n".getBytes());
            dout.flush();

            System.out.println("about to quit");

            //send QUIT
            dout.write("QUIT\n".getBytes());
            dout.flush();
            System.out.println(din.readLine());

            dout.close();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
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
        System.out.println("server count: "+nodeList.getLength());
        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            Node node = nodeList.item(itr);
            Element e = (Element)node;
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
