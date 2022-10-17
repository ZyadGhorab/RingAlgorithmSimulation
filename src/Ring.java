import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Ring {
  public static Logger logger;

  public boolean isCoordinator = false;
  public boolean isInitiator = false;

  public ObjectInputStream socketReader;
  public PrintWriter socketWriter;
  public static Node currentNode;

  public static final int TIMEOUT = 3000;

  public static ArrayList<Integer> activeList;

  public HashMap<Integer, Node> nodes;

  private void getCurrentNodeArgs(String[] args) {
    try {
      int nodeId = Integer.parseInt(args[0]);
      int nodePort = Integer.parseInt(args[1]);

      Ring.currentNode = new Node(nodeId, nodePort);
      nodes = getNodesConfiguration(args[2]);
      if (args.length > 3) {
        this.isInitiator = args[3].equalsIgnoreCase("Initiator");
        this.isCoordinator = args[3].equalsIgnoreCase("Coordinator");
      }

    } catch (Exception ex) {
      System.err.println(ex);
      System.exit(-1);
    }
  }

  private HashMap<Integer, Node> getNodesConfiguration(String file) throws Exception {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    HashMap<Integer, Node> nodes = new HashMap<>();

    String line;
    String[] data;

    while ((line = reader.readLine()) != null) {
      data = line.split(",");
      String nodeId = data[0];
      String nodePort = data[1];
      nodes.put(
          Integer.parseInt(nodeId), new Node(Integer.parseInt(nodeId), Integer.parseInt(nodePort)));
    }

    reader.close();
    return nodes;
  }

  public static void main(String[] args) {
    Ring ring = new Ring();
    ring.getCurrentNodeArgs(args);

    logger = new Logger(Ring.currentNode.getId());

    activeList = new ArrayList<>();
    ring.startServerListening();
  }

  public void startServerListening() {
    boolean listening = true;

    if (this.isInitiator) {
      startElection(activeList);
    }

    Socket clientSocket;
    try (ServerSocket serverSocket = new ServerSocket(Ring.currentNode.getPort())) {
      Ring.logger.log(String.format("Server started on port %d.", Ring.currentNode.getPort()));
      while (listening) {
        clientSocket = serverSocket.accept();
        clientSocket.setSoTimeout(Ring.TIMEOUT);
        receive(clientSocket);
      }
    } catch (IOException e) {
      System.err.println("Could not listen on port " + Ring.currentNode.getPort());
      System.exit(-1);
    }
  }

  private void receive(Socket clientSocket) {
    try {
      socketReader = new ObjectInputStream(clientSocket.getInputStream());
      socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);

      Message message = Message.valueOf(String.valueOf(socketReader.readObject()));

      if (message == Message.ELECT) {
        List<Integer> receivedActiveList = (List<Integer>) socketReader.readObject();

        socketWriter.println(Message.OK);
        Ring.logger.log(
            String.format(
                "Send OKAY to %d.", receivedActiveList.get(receivedActiveList.size() - 1)));

        startElection(receivedActiveList);

      } else if (message == Message.COORDINATOR) {

        Integer coordinatorId = (Integer) socketReader.readObject();
        Ring.logger.log(String.format("Received Coordinator message from %d.", coordinatorId));

        int currentNodeId = Ring.currentNode.getId();
        if (currentNodeId != coordinatorId) {
          int nextNodeId = currentNodeId % nodes.size() + 1;
          nodes.get(nextNodeId).sendCoordinatorMessage(coordinatorId);
        } else {
          Ring.logger.log(
              String.format(
                  "Coordinator message is sent from %d to all processes. System is in Idle State.",
                  coordinatorId));
          Node coordinatorNode = nodes.get(coordinatorId);
          coordinatorNode.setCoordinator(true);
          nodes.put(coordinatorId, coordinatorNode);
        }
      }

    } catch (IOException e) {
      System.err.println("Could not listen on port " + Ring.currentNode.getPort());
      System.exit(-1);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    try {
      clientSocket.close();
      socketReader = null;
      socketWriter = null;

    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  void startElection(List<Integer> receivedActiveList) {
    int currentNodeId = Ring.currentNode.getId();
    Ring.logger.logInternal(
        String.format("Triggering an election from process %d.", currentNodeId));
    if (receivedActiveList.contains(currentNodeId)) {
      int maxId = receivedActiveList.stream().max(Integer::compareTo).get();
      int nextNodeId = (maxId % nodes.size()) + 1;
      nodes.get(nextNodeId).sendCoordinatorMessage(maxId);
      return;
    }

    receivedActiveList.add(currentNodeId);

    int nextNodeId = (currentNodeId % nodes.size()) + 1;

    Node nextNode = nodes.get(nextNodeId);
    while (!nextNode.isAlive()) {
      Ring.logger.logInternal(
          String.format("Process %d is not alive. Skipping.", nextNode.getId()));
      nextNodeId = (nextNodeId % nodes.size()) + 1;
      nextNode = nodes.get(nextNodeId);
    }
    nextNode.elect(receivedActiveList);
  }
}
