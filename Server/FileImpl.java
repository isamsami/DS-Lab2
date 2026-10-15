import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

//imports for list files and file exception
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

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
    public byte[] downloadFile(String fileName) throws FileServiceException {
        String ip = clientIp();
        System.out.println("Client " + ip + " requested file: '" + fileName + "'");

        //nofile exception handling
        if (fileName == null || fileName.isEmpty()) {
            throw new FileServiceException("File name cannot be null or empty");
        }

        try {
            byte[] data = Files.readAllBytes(Path.of(fileName));
            System.out.println("Served '" + fileName + "' (" + data.length + " bytes) to " + ip);
            return data;
        } 
        //nosuchfileexception handling and edited IOexception handling 
        catch (NoSuchFileException e) {
            System.out.println("FileImpl: download failed '" + fileName + "': " + e);
            throw new FileServiceException("File not found: '" + fileName + "'");
        } catch (IOException e) {
            System.out.println("FileImpl: download failed '" + fileName + "': " + e);
            throw new FileServiceException("Error reading file: '" + fileName + "': " + e.getMessage());
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

    //list files method
    @Override
    public String[] listFiles() throws RemoteException {
        List<String> names = new ArrayList<>();
        addFiles(Path.of("."), "", names);          // files in the server folder
        addFiles(UPLOAD_DIR, "uploads/", names);    // uploaded files
        System.out.println("Client " + clientIp() + " requested the file list ("
            + names.size() + " files)");
        return names.toArray(new String[0]);
    }

    private void addFiles(Path dir, String prefix, List<String> names) throws RemoteException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String n = p.getFileName().toString();
                // skip folders, compiled files, and temporary upload files
                if (Files.isRegularFile(p) && !n.endsWith(".class") && !n.endsWith(".tmp")) {
                    names.add(prefix + n);
                }
            }
        } catch (IOException e) {
            throw new RemoteException("Could not list files", e);
        }
    }
}

