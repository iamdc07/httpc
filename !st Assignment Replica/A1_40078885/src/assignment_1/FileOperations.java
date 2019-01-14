/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assignment_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import schema.Packet;

import static java.nio.channels.SelectionKey.OP_READ;

/**
 * @author Dc
 */
public class FileOperations {
    private static Logger logger = Logger.getLogger("FileOperations");
    private static Long nextSeq;

    public static String process_Request(String data, Long nextSeqNumber) throws IOException {
        try {

            SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 7896);

            byte[] content = data.getBytes();

            HashMap<Long, Packet> buffer = new HashMap<>();
            ArrayList<Long> ackQueue = new ArrayList<>();
            double packetSize = content.length;
            double noOfPackets = 1;
            nextSeq = nextSeqNumber;

            if (packetSize > 1013) {
                System.out.println("PACKET: " + packetSize / 1013);
                noOfPackets = Math.ceil(packetSize / 1013);
                logger.info("Number of packets: " + noOfPackets + "\n");
            }


            try (DatagramChannel channel = DatagramChannel.open()) {
                double i = 0;
                int from = 0, to = 1013;
                while (i < noOfPackets) {
                    byte[] thePayload = Arrays.copyOfRange(content, from, to);
                    Packet p = new Packet.Builder()
                            .setType(0)
                            .setSequenceNumber(nextSeq)
                            .setPortNumber(7896)
                            .setPeerAddress(serverAddress.getAddress())
                            .setPayload(thePayload)
                            .create();
                    channel.send(p.toBuffer(), routerAddress);
                    from = to++;
                    to = from + 1013;

                    if (i == packetSize - 1)
                        to = (int) packetSize - from;

                    buffer.put(nextSeq, p);
                    System.out.println("FROM BUFFER: " + buffer.get(nextSeq).getSequenceNumber());

                    logger.info("Sending " + thePayload + " to the router at " + routerAddress + "\n");
                    i++;
                    nextSeq++;
                }

                // Try to receive a packet within timeout.
                channel.configureBlocking(false);
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);

                nextSeq--;
                while (true) {
                    logger.info("Waiting for the response" + "\n");
                    selector.select(5000);

                    Set<SelectionKey> keys = selector.selectedKeys();
                    if (keys.isEmpty()) {
                        logger.warning("No response after timeout" + "\n");
                        logger.info("Resending the packet..." + "\n");
                        // Timeout, resend the packet
//                        for (Long element : ackQueue) {
//                            System.out.println("ACK Q: " + element);
//                            if (!(buffer.containsKey(element))) {
//                                System.out.println("BUFFER Q: " + buffer.get(element).getSequenceNumber());
//                            }
//                        }
//                        System.out.println("PACKET NO: ");

                        if(buffer.isEmpty()){
                            System.out.println("ADD: " + serverAddress.getAddress());
                            Packet ackPacket = new Packet.Builder()
                                    .setType(4)
                                    .setSequenceNumber(nextSeq)
                                    .setPeerAddress(serverAddress.getAddress())
                                    .setPortNumber(7896)
                                    .setPayload("Send the data Packet".getBytes())
                                    .create();
                            channel.send(ackPacket.toBuffer(), routerAddress);
                            continue;
                        }

                        for (Map.Entry<Long, Packet> entry : buffer.entrySet()) {
                            System.out.println("PACKET NO: " + entry.getKey());
                            channel.send(entry.getValue().toBuffer(), routerAddress);
                        }
                    } else {
                        // Get the response
                        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                        buf.clear();
                        SocketAddress router = channel.receive(buf);
                        buf.flip();
                        Packet resp = Packet.fromBuffer(buf);

                        if (resp.getType() == 3) {
                            ackQueue.add(resp.getSequenceNumber());
                            buffer.remove(resp.getSequenceNumber());
                            logger.info("ACK received for Packet -" + resp.getSequenceNumber() + "\n");
                            logger.info("Packet: " + resp + "\n");
                            logger.info("Router: " + router + "\n");
                            keys.clear();
                            continue;
                        } else {
//                            Packet ackPacket = new Packet.Builder()
//                                    .setType(3)
//                                    .setSequenceNumber(nextSeq)
//                                    .setPeerAddress(resp.getPeerAddress())
//                                    .setPortNumber(resp.getPeerPort())
//                                    .setPayload("Got the Data Packet".getBytes())
//                                    .create();
//                            channel.send(ackPacket.toBuffer(), router);

//                            if (buffer.containsKey(resp.getSequenceNumber()))


                            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                            logger.info("Packet: " + resp + "\n");
                            logger.info("Router: " + router + "\n");
                            keys.clear();
                            nextSeq++;
                            return payload;
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: " + e);
        }
        return "Error opening Datagram Channel";
    }

    public void post_request(String url, int port, boolean verbose, ArrayList<
            String> header_keys, ArrayList<String> header_values, Long nextSeqNumber, String[] cmd, boolean filewrite) throws
            IOException {
        ArrayList<String> data = new ArrayList<>();
        try {

            URL u;
            File file;
            if (filewrite) {
                u = new URL(cmd[cmd.length - 3]);
            } else {
                u = new URL(url);
            }

            String host = u.getHost();

            if (filewrite) {
                file = new File("D:\\Courses\\Comp 6461-f18 Comp Networks\\Assignments\\Code\\A3_40078885\\!st Assignment Replica\\A1_40078885\\" + cmd[cmd.length - 4]);
            } else {
                file = new File("D:\\Courses\\Comp 6461-f18 Comp Networks\\Assignments\\Code\\A3_40078885\\!st Assignment Replica\\A1_40078885\\" + cmd[cmd.length - 2]);
            }

            System.out.println("URL: " + u.getPath());
            System.out.println("filewrite: " + filewrite);
            System.out.println("FILENAME: " + file.getName());

//            file = new File(cmd[cmd.length - 2]);
            BufferedReader br = new BufferedReader(new FileReader(file));

            int m = 0, len = 0;
            String finalstr = "", str;

            while ((str = br.readLine()) != null) {
                String[] temp = str.split(" ");

                for (String temp1 : temp) {
                    data.add(temp1.trim());
                    finalstr = finalstr.concat(temp1.trim()).concat(" ");
                }
                finalstr = finalstr.substring(0, finalstr.length() - 1);
                len = finalstr.length();
            }


            if (host.indexOf("localhost") != -1) {
                String request = "";
                request = request.concat("POST " + URLEncoder.encode(u.getPath(), "UTF-8") + " HTTP/1.1\r\n");
                request = request.concat("Content-Length : " + len + "\r\n");
                request = request.concat("Host: " + host + "\r\n");
                if (!(header_keys.isEmpty())) {
                    for (int k = 0; k < header_keys.size(); k++) {
                        request = request.concat(header_keys.get(k) + ":" + " " + header_values.get(k) + "\r\n");
                    }
                }
                request = request.concat("\r\n");
                request = request.concat(finalstr);
                System.out.println("REQUEST STRING: " + request);
                String response = process_Request(request, nextSeqNumber);
                System.out.println("RESPONSE: " + response); // if timeout, send the packet again(Apply selective repeat)

                if (filewrite) {
                    if (cmd[cmd.length - 2].equalsIgnoreCase("-o")) {
//                        System.out.println("FILENAME: " + cmd[cmd.length - 1]);
                        BufferedWriter out = new BufferedWriter(new FileWriter(cmd[cmd.length - 1]));

//                            System.out.println(outStr);
                        out.write(response);
                        out.flush();
                        out.close();
                    }
                }

            } else {
                Socket s = new Socket(host, port);

                BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));

                wr.write("POST " + URLEncoder.encode(u.getPath(), "UTF-8") + " HTTP/1.1\r\n");
//            System.out.print("POST " + URLEncoder.encode(u.getPath(), "UTF-8") + " HTTP/1.1" + " Length: " + len);
                wr.write("Content-Length: " + len + "\r\n");
                wr.write("Accept:application/json\r\n");
                wr.write("Content-Type:application/json\r\n");
                wr.write("Host: " + host + "\r\n");
                if (!(header_keys.isEmpty())) {
                    for (int k = 0; k < header_keys.size(); k++) {
                        wr.write(header_keys.get(k) + ":" + " " + header_values.get(k) + "\r\n");
                    }
                }
                wr.write("\r\n");
                wr.write(finalstr);
                wr.flush();

                //BufferedReader having the server response
                BufferedReader bufRead = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String outStr, editdata;

                if (filewrite) {
                    if (cmd[cmd.length - 2].equalsIgnoreCase("-o")) {
//                        System.out.println("FILENAME: " + cmd[cmd.length - 1]);
                        BufferedWriter out = new BufferedWriter(new FileWriter(cmd[cmd.length - 1]));
                        while ((outStr = bufRead.readLine()) != null) {
//                            System.out.println(outStr);
                            out.write(outStr + "\r\n");
                            out.flush();
                        }
                        out.close();
                    }
                } else {
                    if (verbose) {
                        while ((outStr = bufRead.readLine()) != null) {
                            //Print response
                            System.out.println(outStr);
                        }
                    }

                    JSONObject json = new JSONObject();

                    JSONObject jsondata = new JSONObject();
                    JSONObject rawobject = new JSONObject();

                    if (!host.equalsIgnoreCase("localhost")) {
                        if (!data.isEmpty()) {
                            for (String str1 : data) {
                                editdata = str1.substring(1, str1.length() - 1);
                                String[] element = editdata.split(":");
                                jsondata.put(element[0].substring(1, element[0].length() - 1), element[1]);
                                rawobject.put(element[0].substring(1, element[0].length() - 1), element[1]);
                            }
                        }
                        json.put("data", jsondata);

                        JSONObject hd = new JSONObject();
                        hd.put("Host", u.getHost());
                        if (!header_keys.isEmpty()) {
                            for (int i = 0; i < header_keys.size(); i++) {
                                hd.put(header_keys.get(i), header_values.get(i));
                            }
                        }
                        json.put("Headers", hd);
                        json.put("json", rawobject);

                        json.put("Url", u.toString());
                        System.out.println(json);
                    }
                }
                bufRead.close();
                s.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EXCEPTION1: " + e);
        }
    }


    public void writeonfile(String url, int port) {
        try {
            String[] cmd = url.split(" ");

//            System.out.println("check");
            URL u = new URL(cmd[cmd.length - 3]);
            String host = u.getHost();

            Socket s = new Socket(host, port);

            PrintWriter pw = new PrintWriter(s.getOutputStream(), false);

            System.out.print("GET " + u.getFile() + " HTTP/1.0\r\n" + "Host: " + host + "\r\n");
            pw.print("GET " + u.getFile() + " HTTP/1.0\r\n");
            pw.print("Host: " + host + "\r\n");
            pw.print("\r\n");
            pw.flush();

            BufferedReader bufRead = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String outStr;
            if (cmd[cmd.length - 2].equalsIgnoreCase("-o")) {
                BufferedWriter out = new BufferedWriter(new FileWriter(cmd[cmd.length - 1]));
                while ((outStr = bufRead.readLine()) != null) {
                    System.out.println(outStr);
                    out.write(outStr + "\r\n");
                    out.flush();
                }
                out.close();
            }
            pw.close();
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e);
        }
    }


//    public ArrayList<String> post_request(String url, int port, Socket s, ArrayList<String> header_keys, ArrayList<String> header_values, String[] cmd) throws IOException {
//        ArrayList<String> data = new ArrayList<>();
//        try {
//            File file = new File(cmd[cmd.length - 2]);
//            BufferedReader br = new BufferedReader(new FileReader(file));
//
//            int m = 0, len = 0;
//            String finalstr = "", str;
//
//            while ((str = br.readLine()) != null) {
//                String[] temp = str.split(" ");
//
//                for (String temp1 : temp) {
//                    data.add(temp1.trim());
//                    finalstr = finalstr.concat(temp1.trim()).concat("&");
//                }
//                finalstr = finalstr.substring(0, finalstr.length() - 1);
//                len = finalstr.length();
//            }
//
////            System.out.println(finalstr);
//
//            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
//            URL u = new URL(url);
//            String host = u.getHost();
//
//            wr.write("POST " + URLEncoder.encode(u.getPath(), "UTF-8") + " HTTP/1.1\r\n");
////            System.out.print("POST " + URLEncoder.encode(u.getPath(), "UTF-8") + " HTTP/1.1" + " Length: " + len);
//            wr.write("Content-Length: " + len + "\r\n");
//            wr.write("Accept:application/json\r\n");
//            wr.write("Content-Type:application/json\r\n");
//            wr.write("Host: " + host + "\r\n");
//            if (!(header_keys.isEmpty())) {
//                for (int k = 0; k < header_keys.size(); k++) {
//                    wr.write(header_keys.get(k) + ":" + " " + header_values.get(k) + "\r\n");
//                }
//            }
//            wr.write("\r\n");
//            wr.write(finalstr);
//            wr.flush();
//
//            return data;
//        } catch (Exception e) {
//            System.out.println("EXCEPTION: " + e);
//        }
//        return data;
//    }


//    public void receive_response(String url, int port, Socket s, boolean verbose, ArrayList<String> header_keys, ArrayList<String> header_values, ArrayList<String> data) {
//        try {
//            URL u = new URL(url);
//            String host = u.getHost();
//
//            //BufferedReader having the server response
//            BufferedReader bufRead = new BufferedReader(new InputStreamReader(s.getInputStream()));
//            String outStr, editdata;
//
//            if (verbose) {
//                while ((outStr = bufRead.readLine()) != null) {
//                    //Print response
//                    System.out.println(outStr);
//                }
//            }
//            JSONObject json = new JSONObject();
//
//            JSONObject jsondata = new JSONObject();
//            JSONObject rawobject = new JSONObject();
//
//            System.out.println("CHECK IN RECEIVE");
//            if (!data.isEmpty()) {
//                for (String str1 : data) {
//                    editdata = str1.substring(1, str1.length() - 1);
//                    String[] str = editdata.split(":");
//                    jsondata.put(str[0].substring(1, str[0].length() - 1), str[1]);
//                    rawobject.put(str[0].substring(1, str[0].length() - 1), str[1]);
//                }
//            }
//            json.put("data", jsondata);
//
//            JSONObject hd = new JSONObject();
//            hd.put("Host", u.getHost());
//            if (!header_keys.isEmpty()) {
//                for (int i = 0; i < header_keys.size(); i++) {
//                    hd.put(header_keys.get(i), header_values.get(i));
//                }
//            }
//            json.put("Headers", hd);
//            json.put("json", rawobject);
//
//            json.put("Url", u.toString());
//            System.out.println(json);
//            bufRead.close();
//
//        } catch (Exception e) {
//            System.out.println("EXCEPTION: " + e);
//        }
}

