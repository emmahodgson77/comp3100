package src.main.java;

import java.io.*;
import java.net.*;

public class MyDSServer {
    public static void main(String[] args) throws Exception{
        int PORT = 30305;
        ServerSocket ss=new ServerSocket(PORT);
        Socket s=ss.accept();
        DataInputStream din=new DataInputStream(s.getInputStream());
        DataOutputStream dout=new DataOutputStream(s.getOutputStream());
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));

        String str="",str2="";
        while(!str.equals("BYE")){
            str=din.readUTF();
            System.out.println("Client says: "+str);
            if(str.equalsIgnoreCase("HELO")){
                dout.writeUTF("G'DAY");
                dout.flush();
            }
            str= din.readUTF();
        }
        System.out.println("Client says:"+str);
        dout.writeUTF("BYE");
        dout.flush();
        din.close();
        s.close();
        ss.close();
    }}