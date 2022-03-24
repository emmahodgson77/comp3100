package src.main.java;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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

            //send REDY
            dout.write("REDY\n".getBytes());
            dout.flush();
            System.out.println(din.readLine());

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
}
