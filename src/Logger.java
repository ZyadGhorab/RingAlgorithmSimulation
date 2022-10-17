import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

  private BufferedWriter fileWriter;
  private int uuid;

  public Logger(int uuid) {
    this.uuid = uuid;
    try {
      fileWriter = new BufferedWriter(new FileWriter(String.format("Node_%d.txt", uuid), false));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void log(String s) {
    append("Algorithm Log: " + s);
  }

  public void logInternal(String s) {
    append("Internal Log: " + s);
  }

  private void append(String message) {
    try {
      String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
      String line = String.format("[%s | Process %d ] %s", timestamp, this.uuid, message);

      System.out.println(line);
      fileWriter.write(line);
      fileWriter.newLine();
      fileWriter.flush();

    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
