import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Downloads a file from a FileServer.
 *
 * Usage: java FileClient fileName host [port]
 *   fileName  file to download (path as seen by the server)
 *   host      machine running the server, e.g. localhost or 192.168.1.20
 *   port      registry port (default 1099)
 */


public class FileClient {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java FileClient fileName host [port]");
            return;
        }
        String host = args[0];
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : Registry.REGISTRY_PORT;

        try {
            // 1. Locate the registry on the server machine and look up the stub.
            Registry registry = LocateRegistry.getRegistry(host, port);
            System.out.println("Names bound in registry:");
            for (String n : registry.list()) {
                System.out.println("  " + n);
            }
            
            // Look up the remote object by name in the registry
            FileInterface fi = (FileInterface) registry.lookup("FileServer");

            //after lookup is done we can get the class name
            System.out.println("FileServer class name: " + fi.getClass().getName());

            // Prompt the user for upload or download
            Scanner scanner = new Scanner(System.in);
             while (true) {
                System.out.print("Choose: (1) download, (2) upload, (3) quit: ");
                String choice = scanner.nextLine().trim();

                if (choice.equals("1")) {
                    System.out.print("File name to download: ");
                    String name = scanner.nextLine().trim();
                     //Call the remote method as if it were local.
                    byte[] data = fi.downloadFile(name);
                
                    if (data == null) {
                        System.err.println("The server could not provide '" + name + "'.");
                        } else {
                            //Save the file in the current directory under its base name.
                            Path target = Path.of(Path.of(name).getFileName().toString());
                            Files.write(target, data);
                            System.out.println("Downloaded and saved '" + target + "'");
                        }
                
                    } else if (choice.equals("2")) {
                    System.out.print("Local file to upload: ");
                    String name = scanner.nextLine().trim();
                    Path p = Path.of(name);
                    
                    if (!Files.exists(p)) {
                        System.err.println("File not found: " + name);
                        continue;
                    }
                    byte[] content = Files.readAllBytes(p);
                    System.out.println("Uploading '" + p.getFileName() + "' (" + content.length + " bytes)...");
                    fi.uploadFile(p.getFileName().toString(), content);
                    System.out.println("Upload complete: '" + p.getFileName() + "'");
                
                } else if (choice.equals("3")) {
                    break;
                } else {
                    System.out.println("Invalid choice. Please enter 1, 2, or 3.");
                }
            }
        }catch (Exception e) {
            System.err.println("FileClient error: " + e);
            e.printStackTrace();
        }
    }
} 


    

