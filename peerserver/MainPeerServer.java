package peerserver;

import util.Hosts;

import java.io.IOException;

public class MainPeerServer {

    public static void main(String args[]) throws IOException {
        PeerServer peerServer = new PeerServer(Hosts.SERVER, Hosts.getPeerServer(Integer.valueOf(args[0])));
        peerServer.begin(args[0]);
    }
}
