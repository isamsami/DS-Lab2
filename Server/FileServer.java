import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Starts the file service.
 *
 * Usage: java FileServer [port]
 *   port  registry port (default 1099)
 *
 * The server creates its own RMI registry, so there is no need to
 * run the separate rmiregistry tool.
 */
public class FileServer {

    // Kept in static fields so they are never garbage collected
    // while the server is running.
    private static FileImpl impl;
    private static Registry registry;

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;

        try {
            // 1. Create the remote object and export it.
            //    exportObject() returns the stub that clients will use.
            impl = new FileImpl();
            FileInterface stub = (FileInterface) UnicastRemoteObject.exportObject(impl, 0);

            // 2. Create a registry on this machine and bind the stub under a name.
            registry = LocateRegistry.createRegistry(port);
            registry.rebind("FileServer", stub);
            System.out.println("FileServer ready. Registry listening on port " + port + ".");
            // The JVM keeps running because the exported object is listening for calls.
        } catch (Exception e) {
            System.err.println("FileServer error: " + e);
            e.printStackTrace();
        }
    }
}
