/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assignment_1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

/**
 *
 * @author Dc
 */
public class Redirection {

    public void redirect(String url, int port) {
        try {
            String[] cmd = url.split(" ");
            String redirect = "";

            URL u = new URL(cmd[cmd.length - 1]);
            String host = u.getHost();

            Socket s = new Socket(host, port);

            PrintWriter pw = new PrintWriter(s.getOutputStream(), false);

            System.out.print("GET " + u.getPath() + " HTTP/1.0\r\n" + "Host: " + host + "\r\n");
            pw.print("GET " + u.getPath() + " HTTP/1.0\r\n");
            pw.print("Host: " + host + "\r\n");
            pw.print("\r\n");
            pw.flush();

            BufferedReader bufRead = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String outStr;

            ArrayList<String> output = new ArrayList<>();

            while ((outStr = bufRead.readLine()) != null) {
                output.add(outStr);
                System.out.println(outStr);
            }
            String[] word = output.get(0).split(" ");
            if (word[1].equalsIgnoreCase("301")) {

                for (String str : output) {
                    word = str.split(" ");
                    if (word[0].equalsIgnoreCase("location:")) {
                        redirect = word[1] + "/";
                        break;
                    }
                }
                if (!redirect.equals("")) {
                    System.out.println("REDIRECTING...");

                    URL url1 = new URL(redirect);
                    String host1 = url1.getHost();

                    Socket socket = new Socket(host1, port);

                    PrintWriter printw = new PrintWriter(socket.getOutputStream(), false);

//                    System.out.print(redirect);
//                    System.out.print("GET " + url1.getPath() + " HTTP/1.0\r\n" + "Host: " + host1 + "\r\n");
                    printw.print("GET " + url1.getPath() + " HTTP/1.0\r\n");
                    printw.print("Host: " + host1 + "\r\n");
                    printw.print("\r\n");
                    printw.flush();

                    BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String os;

                    while ((os = bf.readLine()) != null) {
                        System.out.println(os);
                    }
                    socket.close();
                    printw.close();
                    bf.close();
                }
//                System.out.println("LOOP BROKEN STILL IN IF CONDITION");
            }
            bufRead.close();
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e);
        }
    }
}
