import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the remote interface.
 *
 * Note: this class does NOT extend UnicastRemoteObject. It is made
 * remote by FileServer, which calls UnicastRemoteObject.exportObject().
 */
public class FileImpl implements FileInterface {

    //added feilds and imports to declare a directory for uploads, a counter for the number of uploads, 
    // and a map to lock file names during upload.
    private static final Path UPLOAD_DIR = Path.of("uploads");
    private final AtomicInteger uploadCount = new AtomicInteger();
    private final ConcurrentHashMap<String, Object> nameLocks = new ConcurrentHashMap<>();

    private String clientIp() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "unknown";
        }
    }

    @Override
    public byte[] downloadFile(String fileName) {
        String ip = clientIp();
        System.out.println("Client " + ip + " requested file: '" + fileName + "'");
        try {
            byte[] data = Files.readAllBytes(Path.of(fileName));
            System.out.println("Served '" + fileName + "' (" + data.length + " bytes) to " + ip);
            return data;
        } catch (IOException e) {
            System.out.println("FileImpl: could not read '" + fileName + "': " + e);
            return null;
        }
    }

    // add uploadfile method
    @Override 
    public void uploadFile(String fileName, byte[] content) throws RemoteException {
        String clientIP = clientIp();
       
        
        if (fileName == null || content == null || fileName.isEmpty()) {
            throw new RemoteException("File name cannot be null or empty");
        }

         // Keep only the base name so a client can't write outside "uploads"
        String baseName = Path.of(fileName).getFileName().toString();
        System.out.println("Upload started: '" + baseName + "' from " + clientIP);
        
        // Lock the file name to prevent concurrent uploads of the same file
        Object lock = nameLocks.computeIfAbsent(baseName, k -> new Object());
        synchronized (lock) { 
            try {
                    Files.createDirectories(UPLOAD_DIR);
                    Path tmp = Files.createTempFile(UPLOAD_DIR, "upload-", ".tmp");
                    Files.write(tmp, content);
                    Files.move(tmp, UPLOAD_DIR.resolve(baseName),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                throw new RemoteException("Upload failed for '" + baseName + "'", e);
                }
        }

        int total = uploadCount.incrementAndGet();
        System.out.println("Upload completed: '" + baseName + "' from " + clientIP + " (total uploads: " + total + ")");
    }
}
