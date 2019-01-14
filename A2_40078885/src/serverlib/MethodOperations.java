package serverlib;

import schema.Packet;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MethodOperations {
    private ArrayList<String> request;
    private int length = 0;
    private File file;
    private String content;
    private boolean readOnly;
    private static Logger logger = Logger.getLogger("Server");

    public MethodOperations(ArrayList<String> request, File file, boolean readOnly, String content) {
        this.request = request;
        this.length = 0;
        this.file = file;
        this.content = content;
        this.readOnly = readOnly;
    }

    public String generateVerbose(int length, ArrayList<String> data, boolean status) {
//        try {
        String verbose = "";
        for (String line : request) {
            length = length + line.length();
        }

        if (status && data.size() == 0) {
            verbose = verbose.concat("HTTP/1.1 404 File Not Found\r\n");
        } else if (status && data.size() == 1) {
            verbose = verbose.concat("HTTP/1.1 400 Bad Request\r\n");
        } else {
            verbose = verbose.concat("HTTP/1.1 200 OK\r\n");
        }
        verbose = verbose.concat("Date: " + LocalDateTime.now() + "\r\n");
        verbose = verbose.concat("Server: Dc/1.0\r\n");
        verbose = verbose.concat("Content-Length: " + length + "\r\n");
        verbose = verbose.concat("Content-Type: text/plain-text" + "\r\n");
        verbose = verbose.concat("\r\n");
        if (data.size() != 0) {
            for (String item : data) {
                verbose = verbose.concat(item + "\r\n");
            }
        }
        return verbose;
//        } catch (IOException exception) {
//            System.out.println("Exception: " + exception);
//        }
    }

    public String getRequest() throws IOException {

        HashMap<String, String> arguments = new HashMap<>();
        HashMap<String, String> headers = new HashMap<>();
        length = 0;
        boolean error = false;

        ArrayList<String> data = new ArrayList<>();


//        if (request.size() > 1) {
            for (String line : request) {
                length = length + line.length();
            }
            String requestLine[] = request.get(0).split(" ");

            for (int i = 1; i < request.size(); i++) {
                System.out.println("req arraylist: " + request.get(i));
                String header[] = request.get(i).split(":");
                System.out.println("HEADER KEY: " + header[0].trim());
                System.out.println("HEADER VALUE: " + header[1].trim());
                headers.put(header[0], header[1]);
            }

            if (requestLine[1].equalsIgnoreCase("/")) {
                if (!readOnly) {
                    if (file.exists()) {
                        String[] files = file.list();
                        for (int i = 0; i < files.length; i++) {
                            System.out.println("File " + data.add(files[i]));
                        }
                    } else {
                        System.out.println("File does not exist");
                        error = true;
                    }
                } else {
                    error = true;
                    data.add("Access Denied");
                }
            } else {
                File readFile = new File(file.getAbsolutePath() + "\\" + requestLine[1].substring(1));
                System.out.println("FILE PATH: " + file.getAbsolutePath() + "\\" + requestLine[1].substring(1));

                if (!readOnly) {
//                    System.out.println("CHECK READONLY");
                    if (readFile.exists()) {
                        BufferedReader br = new BufferedReader(new FileReader(readFile));
                        String line;
                        int counter = 0;
                        while ((line = br.readLine()) != null) {
                            data.add(line);
                        }
                    } else {
                        System.out.println("File does not exist");
                        error = true;
                    }
                } else {
                    error = true;
                    data.add("Access Denied");
                }
            }

//            System.out.println("Error flag: " + error);
            String verbose = generateVerbose(length, data, error);
            arguments.clear();
            headers.clear();
            request = null;
            return verbose;
//        }
//        return "Request is not valid";
    }

    public String postRequest() {
        HashMap<String, String> headers = new HashMap<>();
        length = 0;
        boolean error = false;
        ArrayList<String> data = new ArrayList<>();
        try {

            if (request.size() > 1) {
                for (String line : request) {
                    length = length + line.length();
//                System.out.println("REQUEST: " + line);
                }
                String requestLine[] = request.get(0).split(" ");
                String requestURI = URLDecoder.decode(requestLine[1], "UTF-8");
                String payload = content;
                System.out.println("REQ URI" + requestURI);
                System.out.println("Payload " + request.get(request.size() - 1));

                for (int i = 1; i < request.size() - 1; i++) {
                    String header[] = request.get(i).split(":");
                    if (header[0].equalsIgnoreCase("\r\n") || header[0].equalsIgnoreCase(" ") || header[0].equalsIgnoreCase("")) {
                        break;
                    } else {
//                    System.out.println("HEADER KEY: " + header[0].trim());
//                    System.out.println("HEADER VALUE: " + header[1].trim());
                        headers.put(header[0], header[1]);
                    }
                }

                if (payload != null && !(payload.equalsIgnoreCase(""))) {
                    if (!readOnly) {
                        File writeFile = new File(file.getAbsolutePath() + "\\" + requestURI.substring(1));
//                    System.out.println("FILE PATH: " + file.getAbsolutePath() + "\\" + requestURI.substring(1));

                        BufferedWriter bw = new BufferedWriter(new FileWriter(writeFile));
                        bw.write(payload);
                        bw.flush();
                        bw.close();
                    } else {
                        error = true;
                        data.add("Access Denied");
                    }
                }

                String verbose = generateVerbose(length, data, error);
                headers.clear();
                request = null;
                return verbose;
            }
        } catch (FileNotFoundException exception) {
            String v = generateVerbose(length, data, true);
            return v;
        } catch (IOException ioe) {
            System.out.println("Exception: " + ioe);
            return ioe.toString();
        }
        return "Request is not valid";
    }
}


// httpfs -v -p 7896 -d D:\Courses\Comp 6461-f18 Comp Networks\Assignments\Code\A3_40078885\A2_40078885\data
// httpfs -v -p 7896 -d D:\Courses\Comp 6461-f18 Comp Networks\Assignments\Code\A3_40078885\A2_40078885\abc
