package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReadWriteUtil {

    public static void writeRead(InputStream inputStream, OutputStream outputStream, byte[] ack, String outputString) throws IOException {
        outputStream.flush();
        outputStream.write(outputString.getBytes());
        inputStream.read(ack);
    }

    public static void readWrite(InputStream inputStream, OutputStream outputStream, byte[] ack, String outputString) throws IOException {
        inputStream.read(ack);
        outputStream.flush();
        outputStream.write(outputString.getBytes());
    }

    public static void writeFlush(OutputStream outputStream, String outputString) throws IOException {
        outputStream.write(outputString.getBytes());
        outputStream.flush();
    }
}
