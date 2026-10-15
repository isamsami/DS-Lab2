import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the file service.
 * Every method a client can call remotely must be declared here
 * and must throw RemoteException.
 */
public interface FileInterface extends Remote {

    /** Returns the contents of the named file, or null if it cannot be read. */
    byte[] downloadFile(String fileName) throws RemoteException, FileServiceException;

    // add uploadfile method
    void uploadFile(String fileName, byte[] content) throws RemoteException;

    //list files method
    String[] listFiles() throws RemoteException;
}
