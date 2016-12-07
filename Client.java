import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by mrb5960 on 10/29/16.
 */
public class Client implements Runnable{

    int port_no, timeout = 1000;
    String path, ipaddress;
    boolean quiet;
    long start_time, end_time, time_taken;

    public Client(String ipaddress, int port_no, String file_path, int timeout, boolean quiet){
        this.ipaddress = ipaddress;
        this.port_no = port_no;
        this.path = file_path;
        this.timeout = timeout;
        this.quiet = quiet;
        System.out.println("\nClient started...\n");
    }


    public void run(){

        int MSS = 1001, cwnd = 1, start = 0, lastsent = 0, last_acknowledged = 0;
        String filepath = path;
        // arraylist that contains all the byte arrays of segments
        ArrayList<byte[]> segments;
        DatagramSocket socket;
        InetAddress server_address;
        Convertor convertor = new Convertor();

        // class that creates the segments containing the header information and the data or payload
        CreateSegments cs = new CreateSegments(filepath, MSS);
        segments = cs.segments;
        //System.out.println("Size of arraylist: " + segments.size());

        try {
            int server_port = port_no;
            socket = new DatagramSocket();
            server_address = InetAddress.getByName(ipaddress);
            byte[] in_buffer = new byte[2020], out_buffer;
            DatagramPacket inpacket, outpacket;

            // sending initial information to server
            String number_of_packets = String.valueOf(segments.size() + " " + MSS + " " + cs.last_segment_size + " " + cs.length + " " + cs.client_md5hash);
            out_buffer = number_of_packets.getBytes();
            outpacket = new DatagramPacket(out_buffer,out_buffer.length,server_address,server_port);
            start_time = System.currentTimeMillis();
            socket.send(outpacket);

            // receive the acknowledgement of the 'number of segments'
            inpacket = new DatagramPacket(in_buffer, in_buffer.length);
            //socket.setSoTimeout(timeout);
            socket.receive(inpacket);
            if(quiet == false)
            System.out.println(new String(inpacket.getData()).trim() + " Received acknowledgement");

            // variable that stores the sequence number
            int sequence_number = 0;
            // loop till all segments are sent successfully
            while(sequence_number < segments.size()){
                if(quiet == false)
                System.out.println("Current sequence number is " + sequence_number);
                // sending segments one by one by retrieving it from 'segments' arraylist
                out_buffer = segments.get(sequence_number);
                // send the segment
                outpacket = new DatagramPacket(out_buffer, out_buffer.length, server_address, server_port);
                socket.send(outpacket);
                if(quiet == false)
                System.out.println("Segment " + sequence_number + " sent");
                // receive the acknowledgement for each segment
                inpacket = new DatagramPacket(in_buffer, in_buffer.length);

                try {
                    int ack_seq;
                    // set timeout for the receiving acknowledgement
                    socket.setSoTimeout(timeout);
                    socket.receive(inpacket);

                    ack_seq = convertor.byteArrayToInt(inpacket.getData());

                    if(quiet == false)
                    System.out.println("Acknowledgement sequence number " + ack_seq);
                    // check if the received acknowledgement is the required one
                    if(ack_seq == sequence_number) {
                        if(quiet == false)
                        System.out.println("Acknowledgement for segment " + ack_seq + " received \n");
                        sequence_number++;
                    }

                    // if acknowledgement is corrupt then resend segment
                    else {
                        if(quiet == false)
                        System.out.println("Wrong acknowledgement...Resending segment " + sequence_number);
                        continue;
                    }

                    // if timeout occurs, resend the segment
                } catch (SocketTimeoutException e) {
                    if(quiet == false)
                    System.out.println("Client timeout...Resending segment " + sequence_number);
                    continue;
                }
            }

            System.out.println("Client md5hash " + cs.client_md5hash);
            end_time = System.currentTimeMillis();
            time_taken = end_time - start_time;
            System.out.println("Time taken : " + time_taken + " msec");

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
