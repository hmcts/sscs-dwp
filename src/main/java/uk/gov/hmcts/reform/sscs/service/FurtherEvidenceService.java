package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.DWP_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@Service
public class FurtherEvidenceService {

    private CoverLetterService coverLetterService;

    private SscsDocumentService sscsDocumentService;

    private PrintService bulkPrintService;

    private final String furtherEvidenceOriginalSenderTemplateName;

    private final String furtherEvidenceOtherPartiesTemplateName;

    public FurtherEvidenceService(@Value("${docmosis.template.609-97.name}") String furtherEvidenceOriginalSenderTemplateName,
                                  @Value("${docmosis.template.609-98.name}") String furtherEvidenceOtherPartiesTemplateName,
                                  @Autowired CoverLetterService coverLetterService,
                                  @Autowired SscsDocumentService sscsDocumentService,
                                  @Autowired PrintService bulkPrintService) {
        this.furtherEvidenceOriginalSenderTemplateName = furtherEvidenceOriginalSenderTemplateName;
        this.furtherEvidenceOtherPartiesTemplateName = furtherEvidenceOtherPartiesTemplateName;
        this.coverLetterService = coverLetterService;
        this.sscsDocumentService = sscsDocumentService;
        this.bulkPrintService = bulkPrintService;

    }

    public void issue(List<SscsDocument> sscsDocuments, SscsCaseData caseData, DocumentType documentType,
                      List<FurtherEvidenceLetterType> allowedLetterTypes) {
        List<Pdf> pdfs = sscsDocumentService.getPdfsForGivenDocTypeNotIssued(sscsDocuments, documentType);
        if (pdfs != null && pdfs.size() > 0) {
            send609_97_OriginalSender(caseData, documentType, pdfs, allowedLetterTypes);
            send609_98_OtherParty(caseData, documentType, pdfs, allowedLetterTypes);
        }
    }

    private void send609_97_OriginalSender(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs, List<FurtherEvidenceLetterType> allowedLetterTypes) {
        String docName = "609-97-template (original sender)";
        final FurtherEvidenceLetterType letterType = findLetterType(documentType);
        if (allowedLetterTypes.contains(letterType)) {
            byte[] bulkPrintList60997 = buildPdfsFor609_97(caseData, letterType, docName);
            bulkPrintService.sendToBulkPrint(buildPdfs(bulkPrintList60997, pdfs, docName), caseData);
        }
    }

    private void send609_98_OtherParty(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs, List<FurtherEvidenceLetterType> allowedLetterTypes) {

        List<FurtherEvidenceLetterType> otherPartiesList = buildOtherPartiesList(caseData, documentType);

        for (FurtherEvidenceLetterType letterType: otherPartiesList) {
            String docName = letterType == DWP_LETTER ? "609-98-template (DWP)" : "609-98-template (other parties)";

            if (allowedLetterTypes.contains(letterType)) {
                byte[] bulkPrintList60998 = buildPdfsFor609_98(caseData, letterType, docName);

                List<Pdf> pdfs60998 = buildPdfs(bulkPrintList60998, pdfs, docName);
                bulkPrintService.sendToBulkPrint(pdfs60998, caseData);
            }
        }
    }

    private List<FurtherEvidenceLetterType> buildOtherPartiesList(SscsCaseData caseData, DocumentType documentType) {
        List<FurtherEvidenceLetterType> otherPartiesList = new ArrayList<>();

        if (documentType != APPELLANT_EVIDENCE) {
            otherPartiesList.add(APPELLANT_LETTER);
        }
        if (documentType != REPRESENTATIVE_EVIDENCE && checkRepExists(caseData)) {
            otherPartiesList.add(REPRESENTATIVE_LETTER);
        }
        if (documentType != DWP_EVIDENCE) {
            otherPartiesList.add(DWP_LETTER);
        }

        return otherPartiesList;
    }

    private boolean checkRepExists(SscsCaseData caseData) {
        Representative rep = caseData.getAppeal().getRep();
        return null != rep && "yes".equalsIgnoreCase(rep.getHasRepresentative());
    }

    private FurtherEvidenceLetterType findLetterType(DocumentType documentType) {
        if (documentType == APPELLANT_EVIDENCE) {
            return APPELLANT_LETTER;
        } else if (documentType == REPRESENTATIVE_EVIDENCE) {
            return REPRESENTATIVE_LETTER;
        } else {
            return DWP_LETTER;
        }
    }

    private List<Pdf> buildPdfs(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint, String pdfName) {
        List<Pdf> pdfs = new ArrayList<>(pdfsToBulkPrint);
        coverLetterService.appendCoverLetter(coverLetterContent, pdfs, pdfName);
        return pdfs;
    }

    private byte[] buildPdfsFor609_97(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String pdfName) {
        return coverLetterService.generateCoverLetter(caseData, letterType, furtherEvidenceOriginalSenderTemplateName, pdfName);
    }

    private byte[] buildPdfsFor609_98(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String pdfName) {
        return coverLetterService.generateCoverLetter(caseData, letterType, furtherEvidenceOtherPartiesTemplateName, pdfName);
    }

    public boolean canHandleAnyDocument(List<SscsDocument> sscsDocumentList) {
        return null != sscsDocumentList && sscsDocumentList.stream()
            .anyMatch(this::canHandleDocument);
    }

    private boolean canHandleDocument(SscsDocument sscsDocument) {
        return sscsDocument != null && sscsDocument.getValue() != null
            && "No".equals(sscsDocument.getValue().getEvidenceIssued())
            && null != sscsDocument.getValue().getDocumentType();
    }
}
