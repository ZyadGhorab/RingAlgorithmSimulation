import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Main {
  public static List<Process> processes;

  private static void generateProcesses(File directory) {
    try {
      List<LinkedList<String>> commandLineArgs = getCommandLineArgs("initialize.txt");
      for (LinkedList<String> command : commandLineArgs) {
        command.add(0, "java");
        command.add(1, "Ring");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(directory.getCanonicalPath()));
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        processes.add(process);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws IOException {

    File directory = compileRingClass();
    processes = new ArrayList<>();

    generateProcesses(directory);

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      System.out.println("1. Exit And Kill All Processes");
      System.out.println("2. Kill Coordinator process");
      System.out.println("Enter your choice: ");
      String line = reader.readLine();
      if (line.equals("1")) {
        destroyProcesses();
        break;
      } else if (line.equals("2")) {
        destroyCoordinatorProcess(6);
      }
    }
  }

  private static void destroyCoordinatorProcess(int id) {
    //    TODO: Not Implemented Yet
  }

  private static void destroyProcesses() {
    for (Process process : processes) {
      process.destroy();
    }
  }

  private static List<LinkedList<String>> getCommandLineArgs(String file) throws Exception {
    BufferedReader reader = new BufferedReader(new FileReader(file));

    String line;
    String[] data;
    List<LinkedList<String>> commandLineArgs = new ArrayList<>();

    while ((line = reader.readLine()) != null) {
      data = line.split(",");
      commandLineArgs.add(new LinkedList<>(Arrays.asList(data)));
    }

    reader.close();
    return commandLineArgs;
  }

  private static File compileRingClass() throws IOException {
    File directory = new File("src");
    ProcessBuilder compileProcessBuilder = new ProcessBuilder();
    compileProcessBuilder.directory(new File(directory.getCanonicalPath()));
    compileProcessBuilder.command("javac", "Ring.java");
    Process compileProcess = compileProcessBuilder.start();
    int compileExitCode = 0;
    try {
      compileExitCode = compileProcess.waitFor();
      while (compileExitCode != 0) {
        System.out.println("Compilation failed. Retrying...");
        compileProcess = compileProcessBuilder.start();
        compileExitCode = compileProcess.waitFor();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    System.out.println("Compilation Exit code: " + compileExitCode);
    return directory;
  }
}
