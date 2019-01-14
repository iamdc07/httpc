/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assignment_1;

import java.io.*;
import java.lang.reflect.Array;
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
public class PostRequest {
    private static Logger logger = Logger.getLogger("PostRequest");

    public static String process_Request(String data, Long nextSeqNumber) throws IOException {
        try {

            SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 7896);

            byte[] content = data.getBytes();
            HashMap<Long, Packet> buffer = new HashMap<>();
            ArrayList<Long> ackQueue = new ArrayList<>();
            double packetSize = content.length;
            double noOfPackets = 1;
            if (packetSize > 1013) {
                System.out.println("PACKET: " + packetSize / 1013);
                noOfPackets = Math.floor(packetSize / 1013);
                logger.info("Number of packets: " + noOfPackets + "\n");
            }

//            byte[] buffer = new byte[1013];

            try (DatagramChannel channel = DatagramChannel.open()) {
                double i = 0;
                int from = 0, to = 1014;
                while (i < noOfPackets) {
                    byte[] thePayload = Arrays.copyOfRange(content, from, to);
                    Packet p = new Packet.Builder()
                            .setType(0)
                            .setSequenceNumber(nextSeqNumber)
                            .setPortNumber(7896)
                            .setPeerAddress(serverAddress.getAddress())
                            .setPayload(thePayload)
                            .create();
                    channel.send(p.toBuffer(), routerAddress);
                    from = to++;
                    to = from + 1013;

                    buffer.put(nextSeqNumber, p);

                    logger.info("Sending " + data + " to router at " + routerAddress + "\n");
                }
                channel.configureBlocking(false);
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);
                while (true) {
                    logger.info("Waiting for the response" + "\n");
                    selector.select(5000);

                    Set<SelectionKey> keys = selector.selectedKeys();
                    if (keys.isEmpty()) {
                        logger.warning("No response after timeout" + "\n");
                        logger.info("Resending the packet..." + "\n");
                        // Timeout, resend the packet
                        for (Long element : ackQueue) {
                            if (!(buffer.containsKey(element))) {
                                channel.send(buffer.get(element).toBuffer(), routerAddress);
                            }
                        }
                        continue;
                    } else {
                        // Get the response
                        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                        buf.clear();
                        SocketAddress router = channel.receive(buf);
                        buf.flip();
                        Packet resp = Packet.fromBuffer(buf);

//                        if(bufferPacket.getSequenceNumber() == resp.getSequenceNumber()){
//                            continue; // If duplicate packet, discard
//                        }

//                        System.out.println("RESPONSE TYPE: " + resp.getType());
                        if (resp.getType() == 3) {
                            ackQueue.add(resp.getSequenceNumber());
                            buffer.remove(resp.getSequenceNumber());
                            logger.info("ACK received for Packet-" + resp.getSequenceNumber() + "\n");
                            logger.info("Packet: " + resp + "\n");
                            logger.info("Router: " + router + "\n");
                            keys.clear();
                            continue;
                        } else {
                            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                            logger.info("Packet:  " + resp + "\n");
                            logger.info("Router:  " + router + "\n");
                            keys.clear();
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

    public void post_request(String url, int port, boolean verbose, ArrayList<String> header_keys, ArrayList<String> header_values, String data, Long nextSeqNumber, String[] cmd, boolean filewrite) throws IOException {
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
                file = new File(cmd[cmd.length - 4]);
            } else {
                file = new File(cmd[cmd.length - 2]);
            }

            int len;
            len = data.length();


            if (host.indexOf("localhost") != -1) {
                String request = "";
                request = request.concat("POST " + URLEncoder.encode(u.getPath(), "UTF-8") + " HTTP/1.1\r\n");
                request = request.concat("Content-Length: " + len + "\r\n");
                request = request.concat("Host: " + host + "\r\n");
                if (!(header_keys.isEmpty())) {
                    for (int k = 0; k < header_keys.size(); k++) {
                        request = request.concat(header_keys.get(k) + ":" + " " + header_values.get(k) + "\r\n");
                    }
                }
                request = request.concat("\r\n");
                request = request.concat(data);
//                System.out.println("REQUEST STRING: " + request);
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
//            System.out.print("POST " + URLEncoder.encode(u.getPath(), "UTF-8") + " HTTP/1.1");
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
//            System.out.print(data);
                wr.write(data);
                wr.flush();

                //BufferedReader having the server response
                BufferedReader bufRead = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String outStr, temp, editdata;


                if (filewrite) {
                    if (cmd[cmd.length - 2].equalsIgnoreCase("-o")) {
                        System.out.println("FILENAME: " + cmd[cmd.length - 1]);
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
//                System.out.println("Check");
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
                            temp = data;
                            editdata = temp.substring(1, temp.length() - 1);
                            String[] str = editdata.split(":");
                            jsondata.put(str[0].substring(1, str[0].length() - 1), str[1]);
                            rawobject.put(str[0].substring(1, str[0].length() - 1), str[1]);
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
            System.out.println("EXCEPTION: " + e);
        }
    }
}
