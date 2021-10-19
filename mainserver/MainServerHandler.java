package mainserver;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import util.FileTracker;
import util.Hosts;

import static util.ReadWriteUtil.readWrite;
import static util.ReadWriteUtil.writeFlush;
import static util.ReadWriteUtil.writeRead;

public class MainServerHandler extends Thread {
    private final InputStream _inputStream;
    private final OutputStream _outputStream;
    private final Socket _socket;
    private final String _action;
    private final String _connHost;
    private final int _connPort;
    private int _counter;
    private FileTracker _server;
    private FileTracker[] _fileTrackers = new FileTracker[4];
    private Scanner filein = null;
    private OutputStream fileout = null;
    private BufferedReader _bufferedReader;

    public MainServerHandler(Socket socket, InputStream inputStream, OutputStream outputStream, String action) {
        _socket = socket;
        _inputStream = inputStream;
        _outputStream = outputStream;
        _action = action;
        _connHost = _socket.getInetAddress().getHostName();
        _connPort = _socket.getPort();
    }

    public void run() {
        try {
            // file tracker to keep track of the number of files stored over the network
            _server = new FileTracker(Hosts.SERVER.getHostName(), Hosts.SERVER.getPort(), Hosts.SERVER.getHostFileName());
            List<Hosts> peersList = Arrays.asList(Hosts.PEERSERVER1, Hosts.PEERSERVER2, Hosts.PEERSERVER3, Hosts.PEERSERVER4);
            for(int i = 0; i<4; i++) {
                // creating a file tracker for each peer server to keep track of the files it received
                Hosts currentHost = peersList.get(i);
                _fileTrackers[i] = new FileTracker(currentHost.getHostName(), currentHost.getPort(), currentHost.getHostFileName());
            }
            // code to handle file store
            if (_action.equals("store")) {
                storeFile();
            }
            // code to handle file retrieval
            else if (_action.equals("retrieve")) {
                retrieveFile();
            }
            // code to handle file deletion
            else if (_action.equals("delete")) {
                deleteFile();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void storeFile() throws IOException {
        String holder;
        FileOutputStream serverFileOut;
        try {
            // creating a counter file if it doesn't already exist
            if (!_server.getFile().exists()) {
                _counter = 1;
                serverFileOut = _server.getFileout(false);
                // writing 1 to it when it is initially created
                serverFileOut.write(String.valueOf(_counter).getBytes());
                serverFileOut.close();
            } else {
                // updating the count value in the counter file
                _bufferedReader = new BufferedReader(new FileReader(_server.getFile()));
                holder = _bufferedReader.readLine();
                _counter = Integer.parseInt(holder);
                System.out.println("Counter value read from file " + _counter);
                _counter += 1;
                System.out.println("Incremented counter value " + _counter);
                serverFileOut = _server.getFileout(false);
                serverFileOut.write(Integer.toString(_counter).getBytes());
                serverFileOut.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Server entered store block");
        int peerIndex;
        // checking which peer server is requesting file store
        if (_connHost.equals(Hosts.PEERSERVER1.getHostName()) && _connPort == Hosts.PEERSERVER1.getPort()) {
            // and opening the output stream of corresponding database file
            fileout = _fileTrackers[0].getFileout(true);
            // index position to keep track of the peer server position
            peerIndex = 0;
        } else if (_connHost.equals(Hosts.PEERSERVER2.getHostName()) && _connPort == Hosts.PEERSERVER2.getPort()) {
            fileout = _fileTrackers[1].getFileout(true);
            peerIndex = 1;
        } else if (_connHost.equals(Hosts.PEERSERVER3.getHostName()) && _connPort == Hosts.PEERSERVER3.getPort()) {
            fileout = _fileTrackers[2].getFileout(true);
            peerIndex = 2;
        } else {
            fileout = _fileTrackers[3].getFileout(true);
            peerIndex = 3;
        }
        FileTracker[] writeTrackers = new FileTracker[2];
        if (_counter % 4 == 1 || _counter % 4 == 3) {
            // making sure that the file is not sent to the machine that it is coming from
            if (peerIndex % 2 == 0) {
                writeTrackers[0] = _fileTrackers[0];
                writeTrackers[1] = _fileTrackers[2];
            } else {
                writeTrackers[0] = _fileTrackers[1];
                writeTrackers[1] = _fileTrackers[3];
            }
        } else {
            if (peerIndex % 2 != 0) {
                writeTrackers[0] = _fileTrackers[1];
                writeTrackers[1] = _fileTrackers[3];
            } else {
                writeTrackers[0] = _fileTrackers[0];
                writeTrackers[1] = _fileTrackers[2];
            }
        }
        Socket socket1, socket2;
        try {
            // establishing socket connections with the other peer servers to store the file
            socket1 = new Socket(writeTrackers[0].getHost(), writeTrackers[0].getHport());
            System.out.println("connected to helper socket1");

            socket2 = new Socket(writeTrackers[1].getHost(), writeTrackers[1].getHport());
            System.out.println("connected to helper socket2");

            InputStream in1 = socket1.getInputStream();
            // creating socket streams
            OutputStream out1 = socket1.getOutputStream();

            InputStream in2 = socket2.getInputStream();
            OutputStream out2 = socket2.getOutputStream();

            byte[] fn = new byte[50];
            byte[] fk = new byte[50];
            byte[] ack = new byte[40];
            byte[] bytes = new byte[16 * 1024];
            // receiving file name from the user
            readWrite(_inputStream, _outputStream, fn, "filenameReceived");
            // receiving file key
            readWrite(_inputStream, _outputStream, fk, "filekeyReceived");
            String name = new String(fn);
            String key = new String(fk);
            System.out.println(name);
            System.out.println(key);
            String comb = name.trim() + key.trim();
            System.out.println(comb);
            byte[] func;
            func = comb.getBytes();
            int sum = 0;
            // calculating pseudo name for the file
            for (int i = 0; i < func.length; i++)
                sum += (int) func[i];
            System.out.println("calculated hash value: " + sum);
            // writing to the tracker file associated with the connected peer server,
            // the pseudo name of the file along with the network addresses that stores the file
            String sadr = Integer.toString(sum);
            String toWrite = String.join(",", sadr, writeTrackers[0].getHost(),
                    Integer.toString(writeTrackers[0].getHport()), writeTrackers[1].getHost(),
                    Integer.toString(writeTrackers[1].getHport()));
            toWrite += "\n";
            fileout.write(toWrite.getBytes());

            System.out.println("file name: " + name + "with value " + sum);
            System.out.println("stored at " + writeTrackers[0].getHost());
            System.out.println("which is listening on " + writeTrackers[0].getHport());

            System.out.println("also the file " + name + " with value " + sum);
            System.out.println("stored at " + writeTrackers[1].getHost());
            System.out.println("which is listening on " + writeTrackers[1].getHport());

            // sending the file name and file key to the connected peer servers
            writeFlush(out1, "store");
            writeFlush(out2, "store");

            readWrite(in1, out1, ack, sadr);
            readWrite(in1, out1, ack, key.trim());
            in1.read(ack);

            readWrite(in2, out2, ack, sadr);
            readWrite(in2, out2, ack, key.trim());
            in2.read(ack);

            int count;
            // sending the file to the peer servers to store
            while ((count = _inputStream.read(bytes)) > 0) {
                out1.write(bytes, 0, count);
                out2.write(bytes, 0, count);
            }

            in1.close();
            out1.close();
            socket1.close();

            in2.close();
            out2.close();
            socket2.close();

            this._inputStream.close();
            this._outputStream.close();
            System.out.println("closing this connection...");
            this._socket.close();
            System.out.println("this connection is closed: " + _socket);
        }
        // catching the socket connection exception when the peer server is down
        catch (IOException e) {
            System.err.println(e.getMessage());
            System.out.println("file couldn't be stored at multiple places...host is down");
        }
    }

    private void retrieveFile() throws IOException {
        int sum = 0;
        System.out.println("server entered retrieve block");
        byte[] fname = new byte[50];
        byte[] fk = new byte[50];
        byte[] func = new byte[100];
        byte[] ack = new byte[50];
        byte[] bytes = new byte[16 * 1024];
        String line, fileName, comb;
        String fileKey = null;
        String hostr1 = null, hostr2 = null;
        int portr1 = 0 , portr2 = 0;
        try {
            // reading file name and file key that user entered
            readWrite(_inputStream, _outputStream, fname, "filenameReceived");
            readWrite(_inputStream, _outputStream, fk, "filekeyReceived");
            _inputStream.read(ack);
            fileName = new String(fname);
            fileKey = new String(fk);
            System.out.println("printing file name in retrieve block: " + fileName);
            comb = fileName.trim() + fileKey.trim();

            func = comb.getBytes();
            boolean flag = false;
            // calculating the pseudo name for the requested file
            for (int i = 0; i < func.length; i++)
                sum += func[i];
            System.out.println("calculated value for retrieving purposes " + sum);

            // checking which peer server is requesting file store
            if (_connHost.equals(Hosts.PEERSERVER1.getHostName())) {
                // assign corresponding peer server scanner
                filein = _fileTrackers[0].getFileIn();
            } else if (_connHost.equals(Hosts.PEERSERVER2.getHostName())) {
                filein = _fileTrackers[1].getFileIn();
            } else if (_connHost.equals(Hosts.PEERSERVER3.getHostName())) {
                filein = _fileTrackers[2].getFileIn();
            } else {
                filein = _fileTrackers[3].getFileIn();
            }

            // checking for the file by its pseudo name
            while (filein.hasNextLine()) {
                line = filein.nextLine();
                String[] parts = line.split(",");
                if (String.valueOf(sum).equals(parts[0])) {
                    flag = true;
                    // reading the network address and port number of the peer server that has stored the file
                    hostr1 = parts[1];
                    portr1 = Integer.parseInt(parts[2]);
                    hostr2 = parts[3];
                    portr2 = Integer.parseInt(parts[4]);
                    break;
                }
            }
            if (!flag) {
                writeFlush(_outputStream, "file name and key doesn't match");
            } else {
                writeRead(_inputStream, _outputStream, ack, "dummy message");
                retrieveFileHelper(hostr1, portr1, sum, fileKey, bytes);
            }
        }
        // code to handle the file retrieval if one of the peer servers is down
        catch (IOException e) {
            try {
                retrieveFileHelper(hostr2, portr2, sum, fileKey, bytes);
            } catch (IOException ex) {
                System.out.println("connection could not be established with the redundant host either" + ex.getMessage());
            }
        } finally {
            this._outputStream.close();
            this._inputStream.close();
            System.out.println("closing this connection with host in the retrieve.");
            this._socket.close();
            System.out.println("this connection with host closed in retrieve: " + _socket);
        }
    }

    private void retrieveFileHelper (String host, int port, int sum, String fileKey, byte[] bytes) throws IOException {
        byte[] ack = new byte[50];
        int count;
        Socket socket = new Socket(host, port);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        System.out.println("connection established with redundant host");

        writeRead(in, out, ack, "retrieve");
        writeRead(in, out, ack, String.valueOf(sum));
        writeRead(in, out, ack, fileKey.trim());
        writeFlush(out, "serverreadytoreceivefile");
        _outputStream.flush();
        while ((count = in.read(bytes)) > 0)
            _outputStream.write(bytes, 0, count);

        out.close();
        in.close();
        socket.close();
    }

    private void deleteFile() throws IOException {
        int sum = 0;
        System.out.println("server entered delete block");
        byte[] fname = new byte[50];
        byte[] fk = new byte[50];
        byte[] func = new byte[100];
        byte[] ack = new byte[50];
        byte[] delmsg = new byte[50];
        String line, fileName, comb;
        String fileKey = null;
        String hostr1 = null, hostr2 = null;
        int portr1 = 0 , portr2 = 0;
        try {
            // reading the file name and file key that user entered
            readWrite(_inputStream, _outputStream, fname, "filenameReceived");
            readWrite(_inputStream, _outputStream, fk, "filekeyReceived");
            _inputStream.read(ack);

            fileName = new String(fname);
            fileKey = new String(fk);
            System.out.println("printing file name in delete block: " + fileName);
            comb = fileName.trim() + fileKey.trim();
            // calculating the pseudo name
            func = comb.getBytes();

            for (int i = 0; i < func.length; i++)
                sum += (int) func[i];
            System.out.println("calculated value for deleting purposes " + sum);
            // checking which peer server is requesting file store
            if (_connHost.equals(Hosts.PEERSERVER1.getHostName())) {
                // assign corresponding peer server scanner
                filein = _fileTrackers[0].getFileIn();
            } else if (_connHost.equals(Hosts.PEERSERVER2.getHostName())) {
                filein = _fileTrackers[1].getFileIn();
            } else if (_connHost.equals(Hosts.PEERSERVER3.getHostName())) {
                filein = _fileTrackers[2].getFileIn();
            } else {
                filein = _fileTrackers[3].getFileIn();
            }

            // checking which all peer servers are currently storing the file
            while (filein.hasNextLine()) {
                line = filein.nextLine();
                String[] parts = line.split(",");
                if (String.valueOf(sum).equals(parts[0])) {
                    hostr1 = parts[1];
                    portr1 = Integer.parseInt(parts[2]);
                    hostr2 = parts[3];
                    portr2 = Integer.parseInt(parts[4]);
                    break;
                }
            }
            // connecting to these peer servers
            Socket socket1 = new Socket(hostr1, portr1);
            System.out.println("connected to helper socket1 to delete file");

            Socket socket2 = new Socket(hostr2, portr2);
            System.out.println("connected to helper socket2");

            // establishing socket streams
            InputStream in1 = socket1.getInputStream();
            OutputStream out1 = socket1.getOutputStream();
            writeRead(in1, out1, ack, "delete");
            writeRead(in1, out1, ack, String.valueOf(sum));
            writeRead(in1, out1, delmsg, "delete the file");
            String temp = new String(delmsg);
            System.out.println(temp);

            InputStream in2 = socket2.getInputStream();
            OutputStream out2 = socket2.getOutputStream();
            writeRead(in2, out2, ack, "delete");
            writeRead(in2, out2, ack, String.valueOf(sum));
            writeRead(in2, out2, delmsg, "delete the file");

            _outputStream.flush();
            _outputStream.write(delmsg);

            in1.close();
            out1.close();
            socket1.close();

            in2.close();
            out2.close();
            socket2.close();

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } finally {
            this._inputStream.close();
            this._outputStream.close();
            System.out.println("closing this connection...");
            this._socket.close();
            System.out.println("this connection is closed: " + _socket);
        }
    }
}