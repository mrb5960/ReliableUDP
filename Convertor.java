import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by mrb5960 on 11/9/2016.
 */

// class that has all the conversion methods
public class Convertor {

    // method to convert integer to byte array
    public byte[] intToByteArray(int input){
        // allocate 4 bytes for storing an int
        byte[] output = new byte[4];
        // use right shift zero fill to bring required 8 bits into a byte
        output[0] = (byte) ((input) >>> 24);
        output[1] = (byte) ((input) >>> 16);
        output[2] = (byte) ((input) >>> 8);
        output[3] = (byte) (input);
        return output;
    }

    // method to convert byte array to integer
    public int byteArrayToInt(byte[] input){
        int output = input[0] << 24 | (input[1] & 0xFF) << 16 | (input[2] & 0xFF) << 8 | (input[3] & 0xFF);
        return output;
    }

    // method to get the CRC32 checksum of a byte array
    public byte[] getChecksum(byte[] input){
        Checksum crc32 = new CRC32();
        crc32.update(input, 0, input.length);
        long longchecksum = crc32.getValue();
        // allocates a new byte buffer of given capacity
        ByteBuffer bf = ByteBuffer.allocate(8);
        // store a long value in the byte buffer and then convert it to a byte array
        byte[] checksum = bf.putLong(longchecksum).array();
        return checksum;
    }
}
