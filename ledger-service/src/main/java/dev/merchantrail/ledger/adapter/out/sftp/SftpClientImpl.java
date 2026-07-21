package dev.merchantrail.ledger.adapter.out.sftp;

import dev.merchantrail.ledger.application.port.out.SftpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SFTP client implementation using Apache Commons Net
 * TODO: Implement actual SFTP connection for production
 */
@Component
public class SftpClientImpl implements SftpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(SftpClientImpl.class);
    
    @Override
    public void uploadFile(String fileName, byte[] content) {
        // TODO: Implement SFTP upload
        logger.info("Would upload file {} with {} bytes to SFTP server", fileName, content.length);
    }
    
    @Override
    public boolean isConnected() {
        // TODO: Check actual SFTP connection
        return false;
    }
}
