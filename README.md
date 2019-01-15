# httpc an HTTP library and httpfs is a file server
httpc is an client side library implemented using TCP Socket programming using HTTP as the communication protocol. It is a curl/telnet like HTTP
library used to demonstrate GET, POST & HEAD functionalities(HTTP). 

The following presents the options of the final command line.
httpc (get|post) [-v] (-h "k:v")* [-d inline-data] [-f file] URL

In the following, we describe the purpose of the expected httpc command options:
1. Option -v enables a verbose output from the command-line. Verbosity could be useful for testing and debugging stages where we need more information to do so.
2. URL determines the targeted HTTP server. 
3. To pass the headers value to HTTP operation, one can use -h option. The latter means setting the header of the request in the format "key: value." Notice that; you can have multiple headers by having the -h option before each header parameter.
4. -d gives the user the possibility to associate the body of the HTTP Request with the inline data, meaning a set of characters for standard input.
5. Similarly to -d, -f associate the body of the HTTP Request with the data from a given file.

httpfs is a simple file server.
usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]
-v Prints debugging messages.
-p Specifies the port number that the server will listen and serve at.
Default is 8080.
-d Specifies the directory that the server will use to read/write requested files. Default is the current directory when launching the application.

The client and server communication mimics the behaviour of TCP. From handshake to the dropping/delay of the packets, the server and client is 
configured in such a way that it can handle such situations and still manage to transfer the data sucessfully.
The data sent is in the form of packets and the file server is also configured in similiar way which will accept the data in form of packets.
