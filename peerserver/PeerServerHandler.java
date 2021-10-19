package peerserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import static util.ReadWriteUtil.writeRead;
import static util.ReadWriteUtil.writeFlush;

public class PeerServerHandler extends Thread {
    final InputStream in;
    final OutputStream out;
    final Socket s;
    final String operation;
    String enc_key;

    public PeerServerHandler(Socket s, InputStream in, OutputStream out, String operation) {
        this.in = in;
        this.out = out;
        this.s = s;
        this.operation = operation;
    }

    public void run() {
        byte[] fn = new byte[50];
        byte[] fk = new byte[32];
        byte[] bytes = new byte[16 * 1024];
        File file = null;
        String filename, filekey;
        System.out.println("thread entered run");

        if (operation.equals("store")) {
            try {
                System.out.println("thread entered store block");
                in.read(fn);
                filename = new String(fn);
                out.write("filenameReceived".getBytes());

                in.read(fk);
                filekey = new String(fk);
                out.write("filekeyReceived".getBytes());
                enc_key = filekey;
                file = new File(filename.trim());
                String encfilename = "enc_" + filename.trim();
                File enc_file = new File(encfilename);
                OutputStream ofile = new FileOutputStream(file);
                int count;
                while ((count = in.read(bytes)) > 0)
                    ofile.write(bytes, 0, count);
                encdec_file(Cipher.ENCRYPT_MODE, enc_key, file, enc_file);
                System.out.println("file with name " + filename + " stored successfully");
                ofile.close();
                this.in.close();
                this.out.close();
                this.s.close();

            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            } finally {
                boolean flag = file.delete();
                System.out.println("file deleted: " + flag);
            }
        }
        else if (operation.equals("retrieve")) {
            File decrypted_file = null;
            try {
                System.out.println("thread entered retrieve block");
                byte[] ack = new byte[50];
                in.read(fn);
                filename = new String(fn);
                String encfilename = "enc_" + filename.trim();
                writeRead(in, out, fk, "filenameReceived");
                writeRead(in, out, ack, "filekeyReceived");
                filekey = new String(fk);
                enc_key = filekey;
                
                File encrypted_file = new File(encfilename);
                decrypted_file = new File(filename.trim());
                // encrypting the file
                encdec_file(Cipher.DECRYPT_MODE, enc_key, encrypted_file, decrypted_file);

                InputStream ifile = new FileInputStream(decrypted_file);
                int count;
                while ((count = ifile.read(bytes)) > 0)
                    out.write(bytes, 0, count);
                ifile.close();
                this.out.close();
                this.in.close();
                this.s.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            } finally {
                boolean flag = decrypted_file.delete();
                System.out.println("file deleted: " + flag);
            }

        } else {
            // code to delete the encrypted file when  the owner of the file requests the main server to delete it
            File filetodel;
            byte[] ack = new byte[50];
            try {
                System.out.println("thread entered delete block");
                in.read(fn);
                filename = new String(fn);
                String encfilenamed = "enc_" + filename.trim();
                filetodel = new File(encfilenamed);
                writeRead(in, out, ack, "filenameReceived");
                filetodel.delete();
                writeFlush(out, "file deleted successfully");
                this.in.close();
                this.out.close();
                this.s.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }

    }

    public void encdec_file(int cipherMode, String key, File inputFile, File outputFile) {
        try {
            Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, secretKey);

            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);

            inputStream.close();
            outputStream.close();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException e) {
            System.out.println(e.getMessage());
        }
    }
}