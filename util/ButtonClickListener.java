package util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import static javax.swing.JFileChooser.FILES_ONLY;
import static util.ReadWriteUtil.writeRead;

public class ButtonClickListener implements ActionListener{

    private File sent = new File("sent_files.txt");
    private JFileChooser fileChooser;
    private JFrame newFrame;
    private JLabel action;
    private JTextField fileName;
    private JTextField key;
    private OutputStream file_s = null;

    private final String _sHostName;
    private final int _sPort;

    public ButtonClickListener(JLabel action, JTextField fileName, JTextField key, String sHostName, int sPort) {
        this.action = action;
        this.fileName = fileName;
        this.key = key;
        this._sHostName = sHostName;
        this._sPort = sPort;
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        // establishing connection with main server
        Socket sock;
        InputStream in = null;
        OutputStream out = null;
        if (command.equals("o")) {
            int result;
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(FILES_ONLY);
            result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                fileName.setText(selectedFile.getName());
                fileName.setEditable(false);
            }

        } else if (command.equals("s") || command.equals("r")) {
            try {
                sock = new Socket(_sHostName, _sPort);
                in = sock.getInputStream();
                out = sock.getOutputStream();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // code to read the filename and file key from UI
            String fname = fileName.getText();
            String fkey = key.getText();
            File file = new File(fname);
            fileName.setEditable(false);
            key.setEditable(false);
            // code to be executed when "SEND" button is clicked
            if (command.equals("s")) {
                try {
                    if (!sent.exists()) {
                        file_s = new FileOutputStream(sent, true);
                        file_s.close();
                    }
                    // writing to a file called "sent files" to keep track of the list of files sent
                    file_s = new FileOutputStream(sent, true);
                    file_s.write(fileName.getText().getBytes());
                    file_s.write('\n');
                    file_s.close();

                    InputStream ifile = new FileInputStream(file);
                    byte[] ack = new byte[50];
                    byte[] bytes = new byte[16 * 1024];
                    writeRead(in, out, ack, "store");
                    // sending file name and key to the main server
                    writeRead(in, out, ack, fname.trim());
                    writeRead(in, out, ack, fkey.trim());
                    int count;
                    // sending the file to main server
                    while ((count = ifile.read(bytes)) > 0) {
                        out.write(bytes, 0, count);
                    }
                    ifile.close();
                    // deleting a file from the machine once it is sent
                    file.delete();
                    in.close();
                    out.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                    System.exit(1);
                }
                action.setText("File " + fname + "sent successfully");
            }
            // code to be executed when RETRIEVE button is clicked
            else {
                boolean validFile = false;
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(sent));
                    String n;
                    fname = fileName.getText().trim();
                    while ((n = reader.readLine()) != null) {
                        n.trim();
                        if (fname.equals(n)) {
                            validFile = true;
                        }
                    }
                    if (!validFile) {
                        JOptionPane.showMessageDialog(null, "You never sent that file, please check sent files list");
                    }
                    OutputStream ofile = new FileOutputStream(file);
                    byte[] ack = new byte[50];
                    byte[] bytes = new byte[16 * 1024];
                    writeRead(in, out, ack, "retrieve");
                    // sending file name and key to the main server
                    writeRead(in, out, ack, fname.trim());
                    writeRead(in, out, ack, fkey.trim());
                    writeRead(in, out, ack, "ready to retrieve my file");
                    String message = new String(ack);
                    if (message.trim().equals("file name and key doesn't match")) {
                        out.write("dummy message".getBytes());
                        String prompt = message.trim();
                        JOptionPane.showMessageDialog(null, prompt);
                    } else {
                        int count;
                        out.write("dummy message".getBytes());
                        // reading the file from main server
                        while ((count = in.read(bytes)) > 0) {
                            ofile.write(bytes, 0, count);
                        }
                        ofile.close();
                        in.close();
                        out.close();
                    }
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

                action.setText("you chose to retrieve: " + fname);
            }
            fileName.setEditable(true);
            key.setEditable(true);
            fileName.setText("");
            key.setText("");
        }
        // code to display all the sent files when "Sent Files" button is clicked
        else if (command.equals("sent")) {
            try {
                newFrame = new JFrame("All sent Files");
                newFrame.setSize(400, 400);
                JTextArea ta = new JTextArea(30, 90);
                if (!sent.exists()) {
                    JOptionPane.showMessageDialog(null, "You haven't uploaded any files yet");
                }
                BufferedReader inReader = new BufferedReader(new FileReader(sent));
                String s;
                while ((s = inReader.readLine()) != null) {
                    ta.append(s + "\n\r");
                }
                ta.setEditable(false);
                newFrame.add(ta);
                newFrame.setVisible(true);
            } catch (IOException ex) {
                System.out.println(ex);
            }

        }
        // code to execute when DELETE button is clicked
        else {
            String fname = fileName.getText();
            String fkey = key.getText();
            try {
                sock = new Socket(_sHostName, _sPort);
                in = sock.getInputStream();
                out = sock.getOutputStream();
                byte[] ack = new byte[50];
                writeRead(in, out, ack, "delete");
                writeRead(in, out, ack, fname.trim());
                writeRead(in, out, ack, fkey.trim());
                writeRead(in, out, ack, "delete my file");
                String del_status = new String(ack);
                JOptionPane.showMessageDialog(null, del_status.trim());
                in.close();
                out.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
                System.exit(1);
            }
        }
    }
}
