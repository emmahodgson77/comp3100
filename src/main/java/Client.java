package src.main.java;

import java.io.*;
import java.net.*;
public class Client {
    public static void main(String[] args) throws Exception {
        try {
            Socket s = new Socket("127.0.0.1", 30305);

            DataInputStream din = new DataInputStream(s.getInputStream());
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());

            String str = "";
            dout.writeUTF("HELO");
            dout.flush();
            int count =0;
            while (!str.equals("BYE")) {

                count++;
                System.out.println("Server "+ (count) + " says: " + str);
                if (str.equalsIgnoreCase("G'DAY")) {
                    System.out.println("server. "+ (count) + ".. says ... "+str);
                    dout.writeUTF("BYE");
                    dout.flush();
                }
                str = din.readUTF();
            }

            dout.close();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
