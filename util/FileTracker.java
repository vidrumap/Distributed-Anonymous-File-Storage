package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Scanner;

public class FileTracker {
    private String host;
    private int hport;
    private File file;

    public FileTracker(String host, int hport, String fileName) throws FileNotFoundException {
        this.host = host;
        this.hport = hport;
        this.file = new File(fileName);
    }

    public String getHost() {
        return host;
    }

    public int getHport() {
        return hport;
    }

    public File getFile() {
        return file;
    }

    public FileOutputStream getFileout(boolean append) throws FileNotFoundException {
        return new FileOutputStream(file, append);
    }

    public Scanner getFileIn() throws FileNotFoundException {
        return new Scanner(file);
    }
}