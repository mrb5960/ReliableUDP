import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by mrb5960 on 11/2/16.
 */

public class CreateSegments {

    int MSS, sequence = 0, full_segments, length, startindex = 0, last_byte_index, last_segment_size;
    String filepath;
    ArrayList<byte[]> segments = new ArrayList<>();
    byte[] seq_no = new byte[4], checksum, payload, packet, temp;
    String client_md5hash;
    Convertor convertor = new Convertor();

    public CreateSegments(String path, int segment_size){
        this.MSS = segment_size;
        this.filepath = path;

        try {
            // read file
            FileInputStream fis = new FileInputStream(filepath);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // get the data into a single byte array
            byte[] data = Files.readAllBytes(new File(filepath).toPath());

            // MessageDigest class to calculate hash of entire file
            MessageDigest md = MessageDigest.getInstance("MD5");

            // number of bytes in the file
            length = data.length;
            System.out.println("File size in bytes " + length);
            // calculating the number of segments
            full_segments = length/MSS;
            //System.out.println("Number of full segments " + full_segments);

            // split the byte array into segments of equal size
            for(int i = 0; i < full_segments; i++) {
                // byte array of sequence number
                ByteBuffer bf = ByteBuffer.allocate(4);
                seq_no = bf.putInt(sequence++).array();

                // byte array of payload
                payload = Arrays.copyOfRange(data, startindex, startindex + MSS);
                out.write(payload, 0, payload.length);

                // byte array of checksum
                checksum = convertor.getChecksum(payload);

                // merging sequence number, checksum and payload into a single byte array
                packet = new byte[seq_no.length + checksum.length + payload.length];
                System.arraycopy(seq_no, 0, packet, 0, seq_no.length);
                System.arraycopy(checksum, 0, packet, seq_no.length, checksum.length);
                System.arraycopy(payload, 0, packet, seq_no.length + checksum.length, payload.length);

                // store byte array packet into arraylist 'segments'
                segments.add(packet);
                startindex = startindex + MSS;
            }

            // for last byte if length is not a multiple of MSS
            if(length % MSS != 0) {
                // calculate the start index for the last byte
                last_byte_index = length - length % MSS - 1;

                // byte array of sequence number
                ByteBuffer bf = ByteBuffer.allocate(4);
                seq_no = bf.putInt(sequence++).array();

                // byte array of payload
                payload = Arrays.copyOfRange(data, last_byte_index, length - 1);
                out.write(payload, 0, payload.length);

                // byte array of checksum
                checksum = convertor.getChecksum(payload);

                // merging sequence number and checksum
                temp = new byte[seq_no.length + checksum.length];
                System.arraycopy(seq_no, 0, temp, 0, seq_no.length);
                System.arraycopy(checksum, 0, temp, seq_no.length, checksum.length);

                // merging sequence number, checksum and payload
                packet = new byte[temp.length + payload.length];
                last_segment_size = packet.length;
                System.arraycopy(temp, 0, packet, 0, temp.length);
                System.arraycopy(payload, 0, packet, temp.length, payload.length);

                // store byte array packet into arraylist 'segments'
                segments.add(packet);
            }

            //calculating hash of entire file
            client_md5hash = new BigInteger(1, md.digest(out.toByteArray())).toString(16);

            // display the sequence number, checksum and payload for each segment
            /*for(int i = 0; i < segments.size(); i++) {
                packet = segments.get(i);
                sequence = new BigInteger(Arrays.copyOfRange(packet, 0, seq_no.length)).intValue();
                checksum = Arrays.copyOfRange(packet, seq_no.length, seq_no.length + 16);
                System.out.println("Sequence " + sequence + " Checksum " + new BigInteger(1,checksum).toString(16));
            }*/
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
