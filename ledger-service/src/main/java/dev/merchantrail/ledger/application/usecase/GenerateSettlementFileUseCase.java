package dev.merchantrail.ledger.application.usecase;

import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import dev.merchantrail.ledger.application.port.out.SftpClient;
import dev.merchantrail.ledger.domain.LedgerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates nightly settlement file in ISO 20022 XML format and uploads via SFTP.
 */
public class GenerateSettlementFileUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(GenerateSettlementFileUseCase.class);
    
    private final LedgerRepository ledgerRepository;
    private final SftpClient sftpClient;
    
    public GenerateSettlementFileUseCase(LedgerRepository ledgerRepository, SftpClient sftpClient) {
        this.ledgerRepository = ledgerRepository;
        this.sftpClient = sftpClient;
    }
    
    public void execute() {
        log.info("Starting settlement file generation");
        
        // Get all settled entries from yesterday
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<LedgerEntry> settledEntries = ledgerRepository.findSettledByDate(yesterday);
        
        if (settledEntries.isEmpty()) {
            log.info("No settled entries found for {}", yesterday);
            return;
        }
        
        // Generate ISO 20022 XML
        String xml = generateIso20022Xml(settledEntries, yesterday);
        
        // Upload via SFTP
        String filename = String.format("settlement_%s.xml", 
            yesterday.format(DateTimeFormatter.BASIC_ISO_DATE));
        
        sftpClient.upload(filename, xml.getBytes());
        
        log.info("Generated and uploaded settlement file: {}, {} entries", 
            filename, settledEntries.size());
    }
    
    private String generateIso20022Xml(List<LedgerEntry> entries, LocalDate date) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">\n");
        xml.append("  <CstmrCdtTrfInitn>\n");
        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>MERCHANTRAIL-").append(date).append("</MsgId>\n");
        xml.append("      <CreDtTm>").append(date).append("T00:00:00</CreDtTm>\n");
        xml.append("      <NbOfTxs>").append(entries.size()).append("</NbOfTxs>\n");
        
        long totalAmount = entries.stream()
            .mapToLong(LedgerEntry::getSignedAmount)
            .sum();
        xml.append("      <CtrlSum>").append(Math.abs(totalAmount) / 100.0).append("</CtrlSum>\n");
        xml.append("    </GrpHdr>\n");
        
        // Payment information for each entry
        for (LedgerEntry entry : entries) {
            xml.append("    <PmtInf>\n");
            xml.append("      <PmtInfId>").append(entry.getEntryId()).append("</PmtInfId>\n");
            xml.append("      <PmtMtd>TRF</PmtMtd>\n");
            xml.append("      <ReqdExctnDt>").append(date.plusDays(1)).append("</ReqdExctnDt>\n");
            xml.append("      <Dbtr>\n");
            xml.append("        <Nm>").append(entry.getMerchantId()).append("</Nm>\n");
            xml.append("      </Dbtr>\n");
            xml.append("      <DbtrAcct>\n");
            xml.append("        <Id><Othr><Id>").append(entry.getAccount()).append("</Id></Othr></Id>\n");
            xml.append("      </DbtrAcct>\n");
            xml.append("      <CdtTrfTxInf>\n");
            xml.append("        <PmtId>\n");
            xml.append("          <EndToEndId>").append(entry.getTransactionId()).append("</EndToEndId>\n");
            xml.append("        </PmtId>\n");
            xml.append("        <Amt>\n");
            xml.append("          <InstdAmt Ccy=\"").append(entry.getAmount().getCurrencyCode()).append("\">")
                .append(entry.getAmount().getAmount()).append("</InstdAmt>\n");
            xml.append("        </Amt>\n");
            xml.append("      </CdtTrfTxInf>\n");
            xml.append("    </PmtInf>\n");
        }
        
        xml.append("  </CstmrCdtTrfInitn>\n");
        xml.append("</Document>");
        
        return xml.toString();
    }
}
