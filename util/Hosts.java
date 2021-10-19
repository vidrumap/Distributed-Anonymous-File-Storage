package util;

public enum Hosts {
	SERVER("localhost", 6010, "file_counter.txt"),
	PEERSERVER1("localhost", 6011, "store_info1.txt"),
	PEERSERVER2("localhost", 6012, "store_info2.txt"),
	PEERSERVER3("localhost", 6013, "store_info3.txt"),
	PEERSERVER4("localhost", 6014, "store_info4.txt");

	private final String hostName;
	private final int port;
	private final String hostFileName;

	Hosts(String hostName, int port, String hostFileName) {
		this.hostName = hostName;
		this.port = port;
		this.hostFileName = hostFileName;
	}

	public String getHostName() {
		return this.hostName;
	}

	public int getPort() {
		return this.port;
	}

	public String getHostFileName() {
		return this.hostFileName;
	}

	public static Hosts getPeerServer(int number) {
		switch (number) {
			case 1: return Hosts.PEERSERVER1;
			case 2: return Hosts.PEERSERVER2;
			case 3: return Hosts.PEERSERVER3;
			default: return Hosts.PEERSERVER4;
		}
	}
}
	
