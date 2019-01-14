package serverlib;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import schema.Packet;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;
import static schema.Packet.MIN_LEN;

public class Server {
    private static Logger logger = Logger.getLogger("Server");
    private static Packet responseBuffer;

    public static void main(String args[]) throws IOException {
        BufferedReader in;
        ArrayList<String> request = new ArrayList<>();
        String[] arr = new String[10];
        HashMap<Long, Packet> buffer = new HashMap<>();
        String content = "";
        int totalContentLength = 0;
        boolean readOnly = false;
        boolean flag = false;
        boolean ack = true;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try {
            System.out.println("Enter the Command: ");
            String input = br.readLine();

            String[] cmd = input.split(" ");

            int index = input.indexOf("-d") + 2;
            String path = input.substring(index);

            File file = new File(path.trim());

            String[] blackList = new String[4];
            blackList[0] = ".idea";
            blackList[1] = "out";
            blackList[2] = "src";
            blackList[3] = "A2_40078885.iml";

            for (String element : blackList) {
                if (path.indexOf(element) != -1) {
                    readOnly = true;
                }
            }


            if (!(file.exists()) || readOnly) {
                throw new FileNotFoundException("File Path invalid!");
            }


            try (DatagramChannel channel = DatagramChannel.open()) {
                channel.bind(new InetSocketAddress(7896));
                logger.info("Server is listening at " + channel.getLocalAddress());
                ByteBuffer buf = ByteBuffer
                        .allocate(Packet.MAX_LEN)
                        .order(ByteOrder.BIG_ENDIAN);


                for (; ; ) {
                    buf.clear();
                    SocketAddress router = channel.receive(buf);
//                    System.out.println("PACKET CONTENT: ");
                    // Parse a packet from the received raw data.
                    buf.flip();

                    if (buf.limit() < MIN_LEN)
                        continue;

                    Packet packet = Packet.fromBuffer(buf);
//                    System.out.println("PACKET CONTENT: " + packet.getPayload().toString());


                    if (packet.getType() == 4) {
                        channel.send(responseBuffer.toBuffer(), router);
                    } else {
                        if (packet.getType() == 1) {
                            logger.info("Handshake Packet received with sequence number " + packet.getSequenceNumber());
                            Packet outgoingPacket = new Packet.Builder()
                                    .setType(2)
                                    .setSequenceNumber(packet.getSequenceNumber() + 1)
                                    .setPeerAddress(packet.getPeerAddress())
                                    .setPortNumber(packet.getPeerPort())
                                    .setPayload("".getBytes())
                                    .create();
                            channel.send(outgoingPacket.toBuffer(), router);
                        } else {
                            Packet ackPacket = new Packet.Builder()
                                    .setType(3)
                                    .setSequenceNumber(packet.getSequenceNumber())
                                    .setPeerAddress(packet.getPeerAddress())
                                    .setPortNumber(packet.getPeerPort())
                                    .setPayload("".getBytes())
                                    .create();
                            channel.send(ackPacket.toBuffer(), router);

                            String data = new String(packet.getPayload(), StandardCharsets.UTF_8);

                            String[] lines = data.split("\r\n");
//
//                            if(data.indexOf("GET") != -1){
//
//                                for(String line : lines){
//                                    request.add(line);
//                                }
//
//                                String response = verify(request, file, readOnly, content);
//                                Packet outgoingPacket = new Packet.Builder()
//                                        .setType(0)
//                                        .setSequenceNumber(packet.getSequenceNumber())
//                                        .setPeerAddress(packet.getPeerAddress())
//                                        .setPortNumber(packet.getPeerPort())
//                                        .setPayload(response.getBytes())
//                                        .create();
//                                channel.send(outgoingPacket.toBuffer(), router);
//                                request.clear();
//                            }

                            if (!buffer.containsKey(packet.getSequenceNumber()) && !(data.indexOf("GET") != -1)) {
                                for (String element : lines) {
                                    System.out.println("ELEMENT:" + element);

                                    if (element.indexOf("Content-Length") != -1)
                                        totalContentLength = Integer.parseInt(element.split(" ")[2]);

                                    if (flag)
                                        content = content.concat(element);

                                    if (element.equalsIgnoreCase("")) {
                                        flag = true;
                                    }
                                    buffer.put(packet.getSequenceNumber(), packet);
                                }
                            } else {
                                for (String element : lines) {
                                    buffer.put(packet.getSequenceNumber(), packet);
                                }
                            }

                            System.out.println("Length of content received: " + content.length());
                            System.out.println("Total content length: " + totalContentLength);
                            System.out.println("Content: " + content);

                            if (content.length() < totalContentLength) {
                                continue;
                            }

//                            else if (buffer.containsKey(packet.getSequenceNumber())) {
//                                continue;}

                            for (Map.Entry<Long, Packet> entry : buffer.entrySet()) {
                                String payload = new String(entry.getValue().getPayload(), StandardCharsets.UTF_8);
                                System.out.println("Loop: " + Math.toIntExact(entry.getKey()));
                                arr[Math.toIntExact(entry.getKey())] = payload;
                            }

                            for (String item : arr) {
//                                System.out.println(item);
                                if (item != null) {
                                    request.add(item);
                                    System.out.println("REQUEST: " + item);
                                }
                            }

                            String response = verify(request, file, readOnly, content);

                            Packet outgoingPacket = new Packet.Builder()
                                    .setType(0)
                                    .setSequenceNumber(packet.getSequenceNumber())
                                    .setPeerAddress(packet.getPeerAddress())
                                    .setPortNumber(packet.getPeerPort())
                                    .setPayload(response.getBytes())
                                    .create();

                            String payload = new String(outgoingPacket.getPayload(), UTF_8);
                            logger.info("Packet: " + outgoingPacket);
                            logger.info("Payload: " + payload);
                            logger.info("Router: " + router);

//
//                    System.out.println("RESPONSE: " + response);
                            request.clear();
                            responseBuffer = outgoingPacket;
                            // Send the response to the router not the client.
                            // The peer address of the packet is the address of the client already.
                            channel.send(outgoingPacket.toBuffer(), router);

//                        sendACK(router, channel);

                            for (int k = 0; k < 10; k++) {
                                arr[k] = null;
                            }

                            System.out.println("CHECK");
                            content = "";
                            totalContentLength = 0;
                            buffer.clear();
                            flag = false;
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Listen socket:" + e.getMessage());
        }
    }

    private static String verify(ArrayList<String> request, File file, boolean readOnly, String content) {
        MethodOperations methodOperations = new MethodOperations(request, file, readOnly, content);
//        System.out.println(request.get(0));
        try {
            if (request.get(0).startsWith("GET")) {
                String response = methodOperations.getRequest();
                return response;
            } else if (request.get(0).substring(0, 4).startsWith("POST")) {
                String response = methodOperations.postRequest();
                return response;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Error in server!";
    }

}
