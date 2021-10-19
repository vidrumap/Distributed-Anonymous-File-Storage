package peerserver;

import util.ButtonClickListener;
import util.Hosts;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PeerServer {

    private JFrame mainFrame;
    private JLabel heading;
    private JLabel action;
    private JLabel nameLabel;
    private JLabel keyLabel;
    private JPanel controlPanel1;
    private JPanel controlPanel2;
    private JPanel controlPanel3;
    private JButton open;
    private JButton sent_files;
    private JTextField fileName;
    private JTextField key;
    private final String _sHostName;
    private final int _sPort;
    private final String _pHostName;
    private final int _port;
    private JButton send;
    private JButton retrieve;
    private JButton delete;

    public PeerServer(Hosts mainServer, Hosts peerServer) {
        _sHostName = mainServer.getHostName();
        _sPort = mainServer.getPort();
        _pHostName = peerServer.getHostName();
        _port = peerServer.getPort();
    }

    public void begin(String peerNumber) throws IOException {
        this.setupUI(peerNumber);
        this.displayUI();
        InetAddress address = InetAddress.getByName(_pHostName);
        SocketAddress sockadr = new InetSocketAddress(address, _port);
        ServerSocket sock_listen = new ServerSocket();
        sock_listen.setReuseAddress(true);
        sock_listen.bind(sockadr);
        Socket sock_conn;
        while (true) {
            try {
                // listening for requests from the main server, when peer server is playing server role
                sock_conn = sock_listen.accept();
                System.out.println("connected to the server to provide storage or retrieval");
                InputStream i = sock_conn.getInputStream();
                OutputStream o = sock_conn.getOutputStream();
                byte[] op = new byte[30];
                i.read(op);
                String operation = new String(op);
                o.write("operationReceived".getBytes());
                // spawning a new thread for each connection
                Thread t = new PeerServerHandler(sock_conn, i, o, operation.trim());
                t.start();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }
    }
    private void setupUI(String peerNumber) {
        mainFrame = new JFrame("Anonymous File Storage-System " + peerNumber);
        mainFrame.setSize(400, 400);
        // when peer server is in client mode
        mainFrame.setLayout(new GridLayout(5, 1));

        heading = new JLabel("", JLabel.CENTER);
        nameLabel = new JLabel("File Name", 10);
        keyLabel = new JLabel("Key", 10);
        open = new JButton("Select File");

        fileName = new JTextField(15);
        key = new JTextField(15);
        action = new JLabel("", JLabel.CENTER);
        sent_files = new JButton("Sent Files");

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        open.setActionCommand("o");
        open.addActionListener(new ButtonClickListener(action, fileName, key, _sHostName, _sPort));
        sent_files.setActionCommand("sent");
        sent_files.addActionListener(new ButtonClickListener(action, fileName, key, _sHostName, _sPort));

        // create file upload panel
        controlPanel1 = new JPanel();
        controlPanel1.add(nameLabel);
        controlPanel1.add(fileName);
        controlPanel1.add(open);

        // create sent files panel
        controlPanel2 = new JPanel();
        controlPanel2.add(keyLabel);
        controlPanel2.add(key);
        controlPanel2.add(sent_files);

        // create send, retrieve, delete buttons panel
        controlPanel3 = new JPanel();
        controlPanel3.setLayout(new FlowLayout());
        send = new JButton("SEND");
        retrieve = new JButton("RETRIEVE");
        delete = new JButton("DELETE");
        controlPanel3.add(send);
        controlPanel3.add(retrieve);
        controlPanel3.add(delete);

        mainFrame.add(heading);
        mainFrame.add(controlPanel1);
        mainFrame.add(controlPanel2);
        mainFrame.add(controlPanel3);

        mainFrame.add(action);
        mainFrame.setVisible(true);
    }

    private void displayUI() {
        heading.setText("Select whether to send or retrieve");

        send.setActionCommand("s");
        retrieve.setActionCommand("r");
        delete.setActionCommand("d");

        send.addActionListener(new ButtonClickListener(action, fileName, key, _sHostName, _sPort));
        retrieve.addActionListener(new ButtonClickListener(action, fileName, key, _sHostName, _sPort));
        delete.addActionListener(new ButtonClickListener(action, fileName, key, _sHostName, _sPort));

        mainFrame.setVisible(true);
    }
}
