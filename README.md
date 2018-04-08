# Reliable UDP

**Overview**

The goal of the project was to develop a reliable transfer protocol like TCP using an unreliable data transfer protocol as UDP. Some of the features that are implemented though this project are sequence numbers to check packet duplicity, checksum for packet validation and acknowledgements for the received packets.

---

**Test Cases**

I used a software called 'clumsy' to simulate unreliable network. It can be downloaded from here: https://jagt.github.io/clumsy/
1. **Ideal network:** Transmission of data on a network withhout any of the following mentioned problems.
2. **Network with loss:** Acknowledgements are used to handle network with loss. If a packet is lost, the sequence number of last acknowledged packet is sent as the acknowledgement. This informs the client to retransmit the segment
3. **Network with corruption:** Checksum is calculated which ensures the data integrity of the segment. If the server checksum and the client checksum don’t match, the sequence number of last acknowledged packet is sent as the acknowledgement. This informs the client to retransmit the segment
4. **Network with excessive delay:** Timeouts are used to handle excessive delay. If an acknowledgement is not received within a time limit, client retransmits the segment.
5. **Network with re-ordering:** Sequence numbers are used to handle re-ordering. If an unintended segment is received by the server, it asks the client to retransmit the segment ensuring the order of the segments is maintained.
6. **Network having all the above problems:** This can be handled as all of the above mechanisms work simultaneously.

---

**Files**

1. **fcntp.java:** acts as a command line argument interpreter. Server and client can be started depending upon the arguments provided. Following are examples:
* `java fcntp -s 9999` starts server on port 9999
* `java fcntp -c -t 1000 -f path_of_file 127.0.0.1 9999` starts client on port 9999, IPaddress 127.0.0.1, timeout 1000 and path_of_file as input

2. **Client:** takes a file as an input. Sends segments created by ‘CreateSegments’ class to the server and waits for an acknowledgement. Retransmits the segments in case of network problems.
3. **Server:** Accepts the incoming segments from the client. Checks sequence numbers. Checks if checksum are same. Sends acknowledgements. Also, calculates the hash of the entire file once received.
4. **CreateSegments:** Filepath is given as the input. It then divides the files based on the MSS. Assigns sequence numbers and calculates checksum of the payload. Combines them into a single byte array and stores these byte arrays in an arraylist.
5. **Convertor:** Includes methods to convert byte array to integer value, integer value to byte array and calculate checksum using CRC32
