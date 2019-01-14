package assignment_1;

import org.json.simple.JSONObject;
import schema.Packet;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;

/**
 * @author Dc
 */
public class GetRequest {
    private static Logger logger = Logger.getLogger("GetRequest");
    private static Packet bufferPacket;

    public static String process_Request(String data, Long nextSeqNumber) throws IOException {
        try {

            SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 7896);

            try (DatagramChannel channel = DatagramChannel.open()) {
//                String msg = "Hello World what's up with you";
                Packet p = new Packet.Builder()
                        .setType(0)
                        .setSequenceNumber(nextSeqNumber)
                        .setPortNumber(7896)
                        .setPeerAddress(serverAddress.getAddress())
                        .setPayload(data.getBytes())
                        .create();
                channel.send(p.toBuffer(), routerAddress);

                System.out.println("Byte Array size " + data.getBytes().length);
                logger.info("Sending " + data + " to router at " + routerAddress + "\n");

                // Try to receive a packet within timeout.
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
                        channel.send(p.toBuffer(), routerAddress);  // Timeout, resend the packet
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
                            bufferPacket = resp;
                            logger.info("ACK received for Packet-" + resp.getSequenceNumber() + "\n");
                            logger.info("Packet: " + resp + "\n");
                            logger.info("Router: " + router + "\n");
                            keys.clear();
                            continue;
                        } else {
                            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                            logger.info("Packet: " + resp + "\n");
                            logger.info("Router: " + router + "\n");
                            keys.clear();
                            return payload;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e);
        }
        return "Error opening Datagram Channel";
    }

    public void get_request(String url, int port, boolean verbose, ArrayList<String> header_keys, ArrayList<String> header_values, Long nextSeqNumber, String[] cmd, boolean filewrite) throws IOException {
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

//            System.out.println("URL: " + u.getPath());
//            System.out.println("filewrite: " + filewrite);
//            System.out.println("FILENAME: " + file.getName());


            if (host.indexOf("localhost") != -1) {
                String request = "", response = "";
                request = request.concat("GET " + u.getFile() + " HTTP/1.0\r\n");
                request = request.concat("Host: " + host + "\r\n");
                if (!(header_keys.isEmpty())) {
                    for (int k = 0; k < header_keys.size(); k++) {
                        request = request.concat(header_keys.get(k) + ":" + " " + header_values.get(k) + "\r\n");
                    }
                }
                request = request.concat("\r\n");

                response = process_Request(request, nextSeqNumber);

                System.out.println("RESPONSE: " + response);

                if (filewrite) {
                    if (cmd[cmd.length - 2].equalsIgnoreCase("-o")) {
                        BufferedWriter out = new BufferedWriter(new FileWriter(cmd[cmd.length - 1]));
                        out.write(response);
                        out.flush();
                        out.close();
                    }
                }

            } else {
                Socket s = new Socket(host, port);
                PrintWriter pw = new PrintWriter(s.getOutputStream(), false);
                //            System.out.print("GET " + u.getFile() + " HTTP/1.0\r\n" + "Host: " + host + "\r\n");
                pw.print("GET " + u.getFile() + " HTTP/1.0\r\n");
                pw.print("Host: " + host + "\r\n");
                if (!(header_keys.isEmpty())) {
                    for (int k = 0; k < header_keys.size(); k++) {
                        pw.print(header_keys.get(k) + ":" + " " + header_values.get(k) + "\r\n");
                    }
                }
                pw.print("\r\n");
                pw.flush();


                //BufferedReader for fetching the server response
                BufferedReader bufRead = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String outStr;

                outStr = bufRead.readLine();
                if (outStr != null && !(outStr.equals(""))) {
                    String[] temp = outStr.split(" ");
                    if (!temp[1].equals("200")) {
                        System.out.println(outStr);
                        while ((outStr = bufRead.readLine()) != null) {
                            System.out.println(outStr);
                        }
                    } else { // Create JSON object & print the response

                        if (filewrite) {
                            if (cmd[cmd.length - 2].equalsIgnoreCase("-o")) {
//                        System.out.println("FILENAME: " + cmd[cmd.length - 1]);
                                BufferedWriter out = new BufferedWriter(new FileWriter(cmd[cmd.length - 1]));
                                out.write(outStr);
                                while ((outStr = bufRead.readLine()) != null) {
//                            System.out.println(outStr);
                                    out.write(outStr + "\r\n");
                                    out.flush();
                                }
                                out.close();
                            }
                        } else {
                            //Print verbose
                            if (verbose) {
                                System.out.println(outStr);
                                while ((outStr = bufRead.readLine()) != null) {
                                    System.out.println(outStr);
                                }
                            }

                            //Initializing main JSON object
                            JSONObject json = new JSONObject();

                            if (u.getQuery() != null) {

                                //Initializing JSON object for arguments
                                JSONObject arg = new JSONObject();

                                String query = u.getQuery();
                                String[] pairs = query.split("&");

                                for (String pair : pairs) {
                                    String[] arg1 = pair.split("=");
                                    arg.put(arg1[0], arg1[1]);
                                    json.put("args", arg);
                                }

                            }

                            //Initializing Header JSON object
                            JSONObject hd = new JSONObject();
                            hd.put("Host", host);
                            if (!header_keys.isEmpty()) {
                                for (int i = 0; i < header_keys.size(); i++) {
                                    hd.put(header_keys.get(i), header_values.get(i));
                                }
                            }
                            json.put("Headers", hd);

                            json.put("Url", u.toString());
                            System.out.println(json); //Print the Main JSON object
                        }
                    }
                    bufRead.close();
                }
                s.close();
            }

        } catch (MalformedURLException e) {
            logger.warning("Exception" + e);
        }
    }

}
