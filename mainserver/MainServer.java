package mainserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import util.Hosts;

import static util.ReadWriteUtil.readWrite;

public class MainServer {

    public static void main(String[] args) throws IOException {
        InetAddress address = InetAddress.getByName(Hosts.SERVER.getHostName());
        SocketAddress sockadr = new InetSocketAddress(address, Hosts.SERVER.getPort());
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(sockadr);
        Socket sock;
        while (true) {
            try {
                //listening for connections indefinitely
                System.out.println("listening for connections......");
                //accepting a connection
                sock = serverSocket.accept();
                System.out.println("A new client is connected: " + sock);
                //establishing input and output streams
                InputStream in = sock.getInputStream();
                OutputStream out = sock.getOutputStream();
                byte[] act = new byte[50];
                readWrite(in, out, act, "actionReceived");
                String action = new String(act);
                System.out.println("assigning new thread for this client");
                //spawning a thread for each new connection
                Thread t = new MainServerHandler(sock, in, out, action.trim());
                t.start();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }
    }
}


