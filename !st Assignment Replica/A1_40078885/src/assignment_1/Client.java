package assignment_1;

import schema.Packet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

import static java.nio.channels.SelectionKey.OP_READ;

/**
 * @author Dc
 */
public class Client {
    private static Logger logger = Logger.getLogger("Client");
    private static boolean trip = false;
    private static Long nextSeqNumber = 0L;

    public static void main(String[] args) {
        try {
            Client cl = new Client();
            GetRequest getrequest = new GetRequest();
            PostRequest postrequest = new PostRequest();
            FileOperations fo = new FileOperations();
            Redirection r = new Redirection();

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));



            while (true) {
                System.out.println("Enter the Command: ");
                String input = in.readLine();

                if (input.indexOf("localhost") != -1 && trip == false) {
                    nextSeqNumber = handShake();
                    if (nextSeqNumber == 0L) {
                        System.out.println("Error performing handshake");
                        return;
                    }
                }


                String u, host;
                Boolean inline_data = false, file_read = false, verbose = false, header = false, file_write = false, redirect = false, serverReq = false;

                ArrayList<String> header_keys = new ArrayList<>();
                ArrayList<String> header_values = new ArrayList<>();

                String[] cmd = input.split(" ");

                if (cmd[0].equalsIgnoreCase("httpc")) {
                    int port;

                    if (input.indexOf("localhost") != -1) {
                        port = 7896;
                    } else {
                        System.out.println("Enter the Port number: ");
                        port = Integer.parseInt(in.readLine());
                    }


                    if (input.indexOf("-o") != -1) {
                        u = cmd[cmd.length - 3];
                    } else {
                        u = cmd[cmd.length - 1];
                    }

                    URL url = new URL(u);
                    host = url.getHost();

                    int j = 0, hstartpos = 3, instartpos = 0, fstartpos = 0, vstartpos = 2, headercount = 0;

                    if (input.indexOf("-v") != -1) {
                        verbose = true;
                    }
                    if (input.indexOf("-h") != -1) {
                        header = true;
                        headercount++;
                    }
                    if (input.indexOf("-f") != -1) {
                        file_read = true;
                    }
                    if (input.indexOf("-d") != -1) {
                        inline_data = true;
                    }
                    if (input.indexOf("-o") != -1) {
                        file_write = true;
                    }
                    if (input.indexOf("301") != -1) {
                        redirect = true;
                    }
                    if (input.indexOf("localhost") != -1) {
                        serverReq = true;
                    }

                    if (verbose) {
                        if (header) {
                            fstartpos = (hstartpos + headercount * 2);
                            instartpos = (hstartpos + headercount * 2);
                        } else {
                            fstartpos = vstartpos + 1;
                            instartpos = vstartpos + 1;
                        }
                    } else {
                        if (header) {
                            if (file_read || inline_data) {
                                hstartpos = 2;
                                fstartpos = (hstartpos + headercount * 2);
                                instartpos = (hstartpos + headercount * 2);
                            }
                        } else {
                            fstartpos = 2;
                            instartpos = 2;
                        }
                    }

                    if (header) {
                        if (cmd[hstartpos].equalsIgnoreCase("-h")) {

                            for (int i = 4; i < cmd.length - 1; i++) {
                                String[] temp = cmd[i].split(":");
                                header_keys.add(temp[0]);
                                header_values.add(temp[1]);
                                if (cmd[i + 1].equalsIgnoreCase("-h")) {
                                    i++;
                                    continue;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    if (cmd[1].equalsIgnoreCase("get")) {
                        //Process a GET Request
//                        System.out.println("CHECK");
                        getrequest.get_request(u, port, verbose, header_keys, header_values, nextSeqNumber, cmd, file_write);
                        nextSeqNumber++;
                    } else if (cmd[1].equalsIgnoreCase("post")) {
                        String data;

                        if (inline_data) {
                            data = cmd[instartpos + 1];
                            postrequest.post_request(u, port, verbose, header_keys, header_values, data, nextSeqNumber, cmd, file_write);
//                            nextSeqNumber++;
                        } else {
                            fo.post_request(u, port, verbose, header_keys, header_values, nextSeqNumber, cmd, file_write);
//                            nextSeqNumber++;
//                        fo.receive_response(u, port, s, verbose, header_keys, header_values, file_data);
                        }
                    } else if (redirect) {
                        r.redirect(input, port);
                    }
                } else {
                    System.out.println("Incorrect Syntax! Please Try again");
                }
                System.out.println();
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static Long handShake() {

        SocketAddress routerAddress = new InetSocketAddress("localhost", 3000);
        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 7896);

        try (DatagramChannel channel = DatagramChannel.open()) {
//                String msg = "Hello World what's up with you";
            Packet p = new Packet.Builder()
                    .setType(1)
                    .setSequenceNumber(1L)
                    .setPortNumber(7896)
                    .setPeerAddress(serverAddress.getAddress())
                    .setPayload("".getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddress);

            System.out.println("Server address " + serverAddress.getAddress() + "\n");
            logger.info("Sending SYN to router at " + routerAddress + "\n");

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
                }

                // Get the response.
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = channel.receive(buf);
                buf.flip();
                Packet resp = Packet.fromBuffer(buf);
                Long nextSeqNumber = resp.getSequenceNumber();

                if (resp.getType() == 2) {
                    logger.info("Acknowledgement received" + "\n");
                    logger.info("Next Sequence Number: " + resp.getSequenceNumber() + "\n");
                    logger.info("Router: " + router + "\n");
                    trip = true;
                    keys.clear();
                    channel.close();
                    return nextSeqNumber;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: " + e);
        }
        return 0L;
    }
}

//GET -> httpc get -v http://httpbin.org/get?course=networking&assignment=1
//GET with header -> httpc get -v -h User-Agent:newass http://httpbin.org/get?course=networking&assignment=1
//GET and store response in file -> httpc get -v -h User-Agent:newass http://httpbin.org/get?course=networking&assignment=1 -o dc.txt
//POST with header and JSON -> httpc post -v -h User-Agent:newass -d {"Assignment":1} http://httpbin.org/post
//POST read data from file -> httpc post -v -f filename.txt http://httpbin.org/post CHECK IF FILE EXISTS WITH DATA!!!
//POST read data from file and write response back to the file -> httpc post -v -h User-Agent:newass -f myfile.txt http://httpbin.org/post -o hello.txt
//Print output in file -> httpc -v http://httpbin.org/get?course=networking&assignment=1 -o hello.txt
//URL FOR REDIRECTION -> httpc https://httpstat.us/301
//http://httpbin.org/get?course=networking&assignment=1
//http://httpbin.org/status/418
//http://example.com/
//http://demo2268214.mockable.io/get?course=networking&assignment=1
//http://mockbin.org/bin/91b61a8d-e135-4883-86a7-61f2712874a9/view

//POST REQUEST SKELETON -> httpc post -v DATATOPASS http://localhost/FILENAME.ext
//POST REQUEST To sent data into the file -> httpc post -v -f myfile.txt http://localhost/
//GET REQUEST to fetch file list -> httpc get -v http://localhost/
//GET REQUEST to read a file -> httpc get -v http://localhost/hello.txt
//GET REQUEST to store response in client file -> httpc get -v http://localhost/sample.txt -o hello.txt
//D:\Courses\Comp 6461-f18 Comp Networks\Assignments\Code\A2_40078885\src\serverlib