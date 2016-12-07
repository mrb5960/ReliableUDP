import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by mrb5960 on 10/29/16.
 */
public class Server implements Runnable{

    int port_no;
    boolean quiet;

    public Server(int port_no, boolean quiet){
        this.port_no = port_no;
        this.quiet = quiet;
    }

    public Server(){}

    public void run(){
        int client_port, expected_sequence_number = 0, total_segments, MSS, last_segment_size, filesize, current_index = 0, last_ack = 0;
        byte[] in_buffer = new byte[2020];
        byte[] out_buffer;
        byte[]  temp, sequence, md5hash, file_bytes, received_segment;
        boolean expected_packet;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //ByteArrayInputStream in = new ByteArrayInputStream();

        Server server = new Server();
        DatagramPacket inpacket, outpacket;
        InetAddress client_address;
        ArrayList<byte[]> packet_structure;

        try {
            System.out.print("\nServer started... \n");
            MessageDigest md = MessageDigest.getInstance("MD5");

            DatagramSocket socket = new DatagramSocket(port_no);
            inpacket = new DatagramPacket(in_buffer, in_buffer.length);
            socket.receive(inpacket);

            String init = new String(inpacket.getData());
            String[] arr = init.split(" ");
            total_segments = Integer.parseInt(arr[0].trim());
            MSS = Integer.parseInt(arr[1].trim());
            last_segment_size = Integer.parseInt(arr[2].trim());
            filesize = Integer.parseInt(arr[3].trim());
            String client_md5hash = arr[4].trim();
            file_bytes = new byte[filesize];

            int segment = MSS + 12;
            client_address = inpacket.getAddress();
            client_port = inpacket.getPort();

            // send an acknowledgment for 'number of segments' variable
            out_buffer = new String("OK").getBytes();
            outpacket = new DatagramPacket(out_buffer, out_buffer.length, client_address, client_port);
            socket.send(outpacket);
            if(quiet == false)
            System.out.println("Acknowledgment for number of bytes sent");
            if(quiet == false)
            System.out.println("Total segments " + total_segments);

            while (expected_sequence_number < total_segments) {
                if (expected_sequence_number < total_segments - 1) {
                    // for all segments except the last segment
                    in_buffer = new byte[segment];
                } else
                    // for last segment with remaining bytes
                    in_buffer = new byte[last_segment_size];

                if(quiet == false)
                System.out.println("Waiting for segment " + expected_sequence_number);
                received_segment = server.receiveSegment(socket, in_buffer);
                if(quiet == false)
                System.out.println("Segment " + expected_sequence_number + " received");

                // separate the sequence number, checksum and payload from the received segment
                if (expected_sequence_number < total_segments - 1) {
                    // for all segments except the last segment
                    packet_structure = server.splitSegment(received_segment, false, last_segment_size);
                }
                else
                    // for last segment with remaining size
                    packet_structure = server.splitSegment(received_segment, true, last_segment_size);

                // if the arrived segment is the required segment
                if(server.checkPacketValidity(expected_sequence_number, packet_structure)) {
                    // send acknowledgement
                    server.sendAcknowledgement(socket, out_buffer, client_address, client_port, expected_sequence_number);
                    if(quiet == false)
                    System.out.println("Acknowledgement for segment " + expected_sequence_number + " sent \n");
                    temp = packet_structure.get(2);
                    // write payload to the byte array output stream to calculate the hash on server side
                    out.write(temp);
                    // store sequence number for the last successfully acknowledged segment
                    last_ack = expected_sequence_number;
                    // increment the sequence number
                    ++expected_sequence_number;
                }
                else {
                    if(quiet == false)
                    System.out.println("Incorrect segment!!!");
                    // if the segment is incorrect resend the acknowledgement of the last successfully acknowledged segment
                    server.sendAcknowledgement(socket, out_buffer, client_address, client_port, last_ack);
                    // continue the loop to receive the same segment
                    continue;
                }
            }

            // displaying size of the file in bytes
            System.out.println("File size: " + out.toByteArray().length);
            // get data from the byte array output stream and calculate the md5 hash
            md5hash = md.digest(out.toByteArray());
            System.out.println("Client md5hash is " + client_md5hash);
            System.out.println("Server md5hash is " + new BigInteger(1, md5hash).toString(16));
            //System.out.println(Arrays.toString(file_bytes));
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // method to split the segment received from the client into sequence number, checksum and payload
    public ArrayList<byte[]> splitSegment(byte[] input, boolean last_segment, int last_segment_size){

        byte[] sequence_no, checksum, payload;
        // arraylist to store byte arrays of sequence number, checksum and payload
        ArrayList<byte[]> temp = new ArrayList<>();

        //System.out.println(Arrays.toString(input));
        //System.out.println(input.length);

        // get integer sequence number i.e. 4 bytes
        sequence_no = Arrays.copyOfRange(input, 0, 4);
        // get long checksum i.e. 8 bytes
        checksum = Arrays.copyOfRange(input, 4, 12);

        // if the segment is last segment then the data range to be copied is changed
        if(last_segment == false)
            payload = Arrays.copyOfRange(input, 12, input.length);
        else
            payload = Arrays.copyOfRange(input, 12, last_segment_size);

        //System.out.println(Arrays.toString(payload));
        temp.add(sequence_no);
        temp.add(checksum);
        temp.add(payload);

        return temp;
    }

    // check the validity of the segment i.e. first check the sequence number and then the checksum
    // returns true if segment is valid else returns false
    // input is the expected sequence number and arraylist containing the sequence number, checksum and header of the segment
    public boolean checkPacketValidity(int expected_sequence_number, ArrayList<byte[]> input) {

        int server_sequence_no, client_sequence_no;
        byte[] payload, checksum_in_header, server_checksum, client_checksum, sequence_no_in_header;

        Convertor convertor = new Convertor();

        sequence_no_in_header = input.get(0);
        checksum_in_header = input.get(1);
        payload = input.get(2);

        server_sequence_no = expected_sequence_number;
        client_sequence_no = convertor.byteArrayToInt(sequence_no_in_header);

        //System.out.println("Payload: " + payload.length);

        // check if both sequence numbers are same
        if(server_sequence_no == client_sequence_no) {
            // check if checksum are same
            server_checksum = convertor.getChecksum(payload);
            client_checksum = checksum_in_header;
            //System.out.println("Server checksum " + Arrays.toString(server_checksum)
                   // + " Client checksum " + Arrays.toString(client_checksum));
            if (Arrays.equals(client_checksum, server_checksum))
                return true;
            else
                return false;
        }
        else
            return false;
    }

    // method to send the acknowledgement
    public void sendAcknowledgement(DatagramSocket socket, byte[] out_buffer, InetAddress client_address, int port, int sequence_number){

        /*if(quiet == false)
            System.out.println("Acknowledgement for segment " + sequence_number + " sent \n");*/

        DatagramPacket acknowledgement;
        Convertor convertor = new Convertor();
        out_buffer = convertor.intToByteArray(sequence_number);
        acknowledgement = new DatagramPacket(out_buffer, out_buffer.length, client_address, port);
        try {
            socket.send(acknowledgement);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method to receive segment
    public byte[] receiveSegment(DatagramSocket socket, byte[] in_buffer){
        DatagramPacket segment = new DatagramPacket(in_buffer, in_buffer.length);
        try {
            socket.receive(segment);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return segment.getData();
    }
}
