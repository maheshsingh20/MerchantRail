package dev.merchantrail.ledger.application.port.out;

/**
 * Port for SFTP file upload operations
 */
public interface SftpClient {
    
    /**
     * Upload settlement file to SFTP server
     * @param fileName the name of the file
     * @param content the file content
     */
    void uploadFile(String fileName, byte[] content);
    
    /**
     * Check if connection is available
     */
    boolean isConnected();
}
