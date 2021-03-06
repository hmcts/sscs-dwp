package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.JOINT_PARTY_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.DWP_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.JOINT_PARTY_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.model.PdfDocument;

@RunWith(JUnitParamsRunner.class)
public class FurtherEvidenceServiceTest {

    private static final List<FurtherEvidenceLetterType> ALLOWED_LETTER_TYPES = Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER, JOINT_PARTY_LETTER);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private CoverLetterService coverLetterService;
    @Mock
    private SscsDocumentService sscsDocumentService;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private DocmosisTemplateConfig docmosisTemplateConfig;

    private FurtherEvidenceService furtherEvidenceService;

    private SscsCaseData caseData;
    private Pdf pdf;
    private List<Pdf> pdfList;
    private List<PdfDocument> pdfDocumentList;

    private String furtherEvidenceOriginalSenderTemplateName = "TB-SCS-GNO-ENG-00068.doc";
    private String furtherEvidenceOriginalSenderWelshTemplateName = "TB-SCS-GNO-WEL-00469.docx";
    private String furtherEvidenceOriginalSenderDocName = "609-97-template (original sender)";
    private String furtherEvidenceOtherPartiesTemplateName = "TB-SCS-GNO-ENG-00069.doc";
    private String furtherEvidenceOtherPartiesWelshTemplateName = "TB-SCS-GNO-WEL-00470.docx";
    private String furtherEvidenceOtherPartiesDocName = "609-98-template (other parties)";
    private String furtherEvidenceOtherPartiesDwpDocName = "609-98-template (DWP)";
    Map<LanguagePreference, Map<String, Map<String, String>>> template =  new HashMap<>();

    @Before
    public void setup() throws Exception {
        Map<String, String> nameMap;
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        englishDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        englishDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00068.doc");
        englishDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00069.doc");
        englishDocs.put("d609-98", nameMap);

        Map<String, Map<String, String>> welshDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        welshDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        welshDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00469.docx");
        welshDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00470.docx");
        welshDocs.put("d609-98", nameMap);

        template.put(LanguagePreference.ENGLISH, englishDocs);
        template.put(LanguagePreference.WELSH, welshDocs);

        furtherEvidenceService = new FurtherEvidenceService(coverLetterService, sscsDocumentService, bulkPrintService,
                docmosisTemplateConfig);

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("myPdf.pdf"));
        pdf = new Pdf(pdfBytes, "some doc name");
        pdfList = Collections.singletonList(pdf);
        pdfDocumentList = Collections.singletonList(PdfDocument.builder().pdf(pdf).document(AbstractDocument.builder().build()).build());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndNoRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndNoRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyRepAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyRepAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOtherPartiesWelshTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenRepIssueFurtherEvidenceCallbackWithAppellantRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, REPRESENTATIVE_EVIDENCE, ALLOWED_LETTER_TYPES);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenRepIssueFurtherEvidenceCallbackWithAppellantRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyAppellantAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, REPRESENTATIVE_EVIDENCE, ALLOWED_LETTER_TYPES);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOriginalSenderWelshTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellant_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellant_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndRepAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), any(), any(), any());
    }

    @Test
    public void givenJointPartyIssueFurtherEvidenceCallbackWithAppellant_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, JOINT_PARTY_EVIDENCE);
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, JOINT_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(JOINT_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(JOINT_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellantAndRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAndOtherPartyAppellantAndRepAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER));

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOtherPartiesWelshTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
    }

    @Test
    @Parameters({"APPELLANT_LETTER", "REPRESENTATIVE_LETTER", "DWP_LETTER", "JOINT_PARTY_LETTER" })
    public void givenIssueForParty_shouldGenerateCoverLetterForSelectedParty(FurtherEvidenceLetterType furtherEvidenceLetterType) {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        withRep();
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE, Collections.singletonList(furtherEvidenceLetterType));

        String templateName = furtherEvidenceOtherPartiesTemplateName;
        String docName = furtherEvidenceOtherPartiesDocName;
        if (furtherEvidenceLetterType.equals(DWP_LETTER)) {
            templateName = furtherEvidenceOriginalSenderTemplateName;
            docName = furtherEvidenceOriginalSenderDocName;
        }
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(furtherEvidenceLetterType), eq(templateName), eq(docName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
        then(coverLetterService).shouldHaveNoMoreInteractions();
        then(bulkPrintService).shouldHaveNoMoreInteractions();
    }

    @Test
    @Parameters({"APPELLANT_LETTER", "REPRESENTATIVE_LETTER", "DWP_LETTER", "JOINT_PARTY_LETTER" })
    public void givenIssueForParty_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterForSelectedParty(FurtherEvidenceLetterType furtherEvidenceLetterType) {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE, Collections.singletonList(furtherEvidenceLetterType));

        String templateName = furtherEvidenceOtherPartiesWelshTemplateName;
        String docName = furtherEvidenceOtherPartiesDocName;
        if (furtherEvidenceLetterType.equals(DWP_LETTER)) {
            templateName = furtherEvidenceOriginalSenderWelshTemplateName;
            docName = furtherEvidenceOriginalSenderDocName;
        }
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(furtherEvidenceLetterType), eq(templateName), eq(docName));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any());
        then(coverLetterService).shouldHaveNoMoreInteractions();
        then(bulkPrintService).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndJointParty_shouldGenerateCoverLetterOriginalSenderAndOtherPartyAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false);
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(),caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(JOINT_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(JOINT_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE));
    }

    private void withJointParty() {
        caseData.setJointParty("Yes");
        caseData.setJointPartyName(JointPartyName.builder().lastName("Party").firstName("Joint").build());
        caseData.setJointPartyAddressSameAsAppellant("Yes");
    }

    private void createTestDataAndConfigureSscsDocumentServiceMock(String languagePreferenceFlag, boolean isConfidentialCase) {
        createTestDataAndConfigureSscsDocumentServiceMock(languagePreferenceFlag, isConfidentialCase, APPELLANT_EVIDENCE);
    }

    private void createTestDataAndConfigureSscsDocumentServiceMock(String languagePreferenceFlag, boolean isConfidentialCase, DocumentType documentType) {
        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(documentType.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221")
            .languagePreferenceWelsh(languagePreferenceFlag)
            .isConfidentialCase(isConfidentialCase ? YES : NO)
            .sscsDocument(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued))
            .appeal(Appeal.builder().build())
            .build();

        doReturn(pdfDocumentList).when(sscsDocumentService).getPdfsForGivenDocTypeNotIssued(
            eq(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued)), any(), eq(isConfidentialCase));

        when(sscsDocumentService.sizeNormalisePdfs(any())).thenReturn(pdfDocumentList);
    }

    private void withRep() {
        caseData.getAppeal().setRep(Representative.builder().hasRepresentative("Yes").build());
    }

    @Test
    @Parameters(method = "generateDifferentTestScenarios")
    public void givenDocList_shouldBeHandledUnderCertainConditions(List<SscsDocument> documentList,
                                                                   boolean expected) {

        boolean actual = furtherEvidenceService.canHandleAnyDocument(documentList);

        assertEquals(expected, actual);
    }

    @Test
    public void updateSscsCaseDocumentsWhichHaveResizedDocumentsAndMatchingDocTypeAndMatchingDocIdentifier() {

        DocumentLink resizedDocLink = DocumentLink.builder().documentUrl("resized.com").build();

        SscsDocument updatedDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .resizedDocumentLink(resizedDocLink)
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original.com").build())
                    .build()).build();

        SscsDocument updatedDoc2 = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .resizedDocumentLink(resizedDocLink)
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original2.com").build())
                    .build()).build();

        SscsDocument updatedDoc3 = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original2.com").build())
                    .build()).build();

        SscsWelshDocument updatedWelshDoc = SscsWelshDocument
            .builder()
            .value(
                SscsWelshDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .resizedDocumentLink(resizedDocLink)
                    .documentLink(DocumentLink.builder().documentBinaryUrl("welsh.com").build())
                    .build()).build();

        SscsWelshDocument originalWelshDoc = SscsWelshDocument
            .builder()
            .value(
                SscsWelshDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("welsh.com").build())
                    .build()).build();

        SscsDocument originalDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original.com").build())
                    .build()).build();

        SscsDocument differentTypeDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original.com").build())
                    .build()).build();

        SscsDocument differentLinkDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("not-original2.com").build())
                    .build()).build();

        SscsCaseData caseData = SscsCaseData
            .builder()
            .sscsDocument(Arrays.asList(originalDoc, differentTypeDoc, differentLinkDoc))
            .sscsWelshDocuments(Arrays.asList(originalWelshDoc))
            .build();

        DocumentType docType = APPELLANT_EVIDENCE;

        furtherEvidenceService.updateCaseDocuments(Arrays.asList(updatedDoc, updatedDoc2, updatedDoc3, updatedWelshDoc), caseData, docType);

        assertEquals(resizedDocLink, caseData.getSscsDocument().get(0).getValue().getResizedDocumentLink());
        assertEquals(null, caseData.getSscsDocument().get(1).getValue().getResizedDocumentLink());
        assertEquals(null, caseData.getSscsDocument().get(2).getValue().getResizedDocumentLink());
        assertEquals(null, caseData.getSscsWelshDocuments().get(0).getValue().getResizedDocumentLink());
    }

    @SuppressWarnings("unused")
    private Object[] generateDifferentTestScenarios() {

        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument2WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument3WithAppellantEvidenceAndYesIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("Yes")
                .build())
            .build();

        SscsDocument sscsDocument4WithRepEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        return new Object[]{
            //happy path sceanrios
            new Object[]{Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued), true},
            new Object[]{Collections.singletonList(sscsDocument3WithAppellantEvidenceAndYesIssued), false},
            new Object[]{Collections.singletonList(sscsDocument4WithRepEvidenceAndNoIssued), true},

            new Object[]{Arrays.asList(sscsDocument1WithAppellantEvidenceAndNoIssued,
                sscsDocument2WithAppellantEvidenceAndNoIssued), true},
            new Object[]{Arrays.asList(sscsDocument3WithAppellantEvidenceAndYesIssued,
                sscsDocument1WithAppellantEvidenceAndNoIssued), true},

            //edge scenarios
            new Object[]{null, false},
            new Object[]{Collections.singletonList(SscsDocument.builder().build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder().build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .documentType(null)
                    .build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .build())
                .build()), false},
            new Object[]{Arrays.asList(null, sscsDocument1WithAppellantEvidenceAndNoIssued), true}
        };
    }

}
