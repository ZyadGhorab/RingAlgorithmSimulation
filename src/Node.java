import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class Node {
  private final int id;
  private final int port;

  private Socket socket;
  private ObjectOutputStream socketWriter;
  private BufferedReader socketReader;
  private boolean isAlive;
  private boolean isCoordinator;

  public Node(int id, int port) {
    this.id = id;
    this.port = port;
    this.isAlive = true;
  }

  public void sendCoordinatorMessage(int coordinatorId) {
    connect();

    Ring.logger.log(String.format("Send Coordinator from %d to %d.", coordinatorId, getId()));

    try {
      this.socketWriter.writeObject(Message.COORDINATOR);
      this.socketWriter.writeObject(coordinatorId);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    disconnect();
  }

  public boolean elect(List<Integer> activeList) {
    connect();

    boolean ok = false;

    Ring.logger.log(
        String.format("Send Elect from %d to %d.", activeList.get(activeList.size() - 1), getId()));

    try {
      socketWriter.writeObject(Message.ELECT);
      socketWriter.writeObject(activeList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (getMessage() == Message.OK) {
      ok = true;
      Ring.logger.logInternal(String.format("Received OKAY from %d.", getId()));
    }

    disconnect();

    return ok;
  }

  public void connect() {
    try {
      this.socket = new Socket("localhost", this.port);
      socket.setSoTimeout(Ring.TIMEOUT);
      this.socketWriter = new ObjectOutputStream(this.socket.getOutputStream());
      this.socketReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  private void disconnect() {
    try {
      this.socket.close();
      this.socket = null;
      this.socketWriter = null;
      this.socketReader = null;
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  private Message getMessage() {
    String line;
    try {
      while ((line = socketReader.readLine()) != null) {
        return Message.valueOf(line);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }

    return null;
  }

  public int getId() {
    return id;
  }

  public int getPort() {
    return port;
  }

  public void setAlive(boolean alive) {
    this.isAlive = alive;
  }

  public boolean isAlive() {
    return isAlive;
  }

  public void setCoordinator(boolean coordinator) {
    this.isCoordinator = coordinator;
  }

  public boolean isCoordinator() {
    return isCoordinator;
  }
}
