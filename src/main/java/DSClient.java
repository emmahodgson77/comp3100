package src.main.java;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DSClient {
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
            String JBN = jbnParts[4]+" "+ jbnParts[5] + " "+jbnParts[6];
            String getsCommand = "GETS Capable "+JBN+"\n";
            //send GETS
            dout.write(getsCommand.getBytes());
           dout.flush();
           System.out.println(din.readLine());
//TODO loop through DATA <THIS NUMBER> jobs to get number of jobs to schedule
            //send OK
            dout.write("OK\n".getBytes());
            dout.flush();
            System.out.println(din.readLine());

            //send


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
    public void parseDSSystemXML(String filePath) throws ParserConfigurationException, IOException, SAXException {
        List<Server> dsServers = new ArrayList<Server>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(filePath);
        doc.getDocumentElement().normalize();
        Element rootNode = doc.getDocumentElement();
        //loop through
        rootNode.getAttribute("cores");


        Collections.sort(dsServers);
    }


}
