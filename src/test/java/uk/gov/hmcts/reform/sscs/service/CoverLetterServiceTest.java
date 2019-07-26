package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.service.placeholders.OriginalSender60997PlaceholderService;

@RunWith(JUnitParamsRunner.class)
public class CoverLetterServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    @Mock
    private OriginalSender60997PlaceholderService originalSender60997PlaceholderService;
    @Mock
    private PdfGenerationService pdfGenerationService;
    @InjectMocks
    private CoverLetterService coverLetterService;

    @Test
    @Parameters(method = "generateNullScenarios")
    public void givenNullArgs_shouldThrowException(SscsCaseData caseData, List<Pdf> pdfsToBulkPrint) {
        try {
            coverLetterService.appendCoverLetter(caseData, pdfsToBulkPrint);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    private Object[] generateNullScenarios() {
        return new Object[]{
            new Object[]{null, buildPdfListWithOneDoc()},
            new Object[]{SscsCaseData.builder().build(), null}
        };
    }

    @Test
    public void appendCoverLetter() {
        SscsCaseData caseData = SscsCaseData.builder().build();

        given(originalSender60997PlaceholderService
            .populatePlaceHolders(eq(caseData))).willReturn(Collections.singletonMap("someKey", "someValue"));

        given(pdfGenerationService.generatePdf(any(DocumentHolder.class)))
            .willReturn(new byte[]{'l', 'e', 't', 't', 'e', 'r'});

        List<Pdf> pdfsToBulkPrint = buildPdfListWithOneDoc();
        coverLetterService.appendCoverLetter(caseData, pdfsToBulkPrint);

        then(originalSender60997PlaceholderService).should(times(1))
            .populatePlaceHolders(eq(caseData));
        assertCoverLetterIsFirstDocInList(pdfsToBulkPrint);
        assertEquals("doc", pdfsToBulkPrint.get(1).getName());
        assertEquals(Arrays.toString(new byte[]{'d', 'o', 'c'}), Arrays.toString(pdfsToBulkPrint.get(1).getContent()));
        assertArgumentsForPdfGeneration();

    }

    private void assertArgumentsForPdfGeneration() {
        ArgumentCaptor<DocumentHolder> argumentCaptor = ArgumentCaptor.forClass(DocumentHolder.class);
        then(pdfGenerationService).should(times(1)).generatePdf(argumentCaptor.capture());
        DocumentHolder documentHolder = argumentCaptor.getValue();
        assertEquals("TB-SCS-GNO-ENG-00068.doc", documentHolder.getTemplate().getTemplateName());
        assertEquals(Collections.singletonMap("someKey", "someValue").toString(),
            documentHolder.getPlaceholders().toString());
        assertTrue(documentHolder.isPdfArchiveMode());
    }

    private void assertCoverLetterIsFirstDocInList(List<Pdf> pdfsToBulkPrint) {
        assertEquals(2, pdfsToBulkPrint.size());
        assertEquals("609_97_OriginalSenderCoverLetter", pdfsToBulkPrint.get(0).getName());
        assertEquals(Arrays.toString(new byte[]{'l', 'e', 't', 't', 'e', 'r'}),
            Arrays.toString(pdfsToBulkPrint.get(0).getContent()));
    }

    private List<Pdf> buildPdfListWithOneDoc() {
        List<Pdf> docList = new ArrayList<>(1);
        docList.add(buildPdf());
        return docList;
    }

    private Pdf buildPdf() {
        byte[] content = new byte[]{'d', 'o', 'c'};
        return new Pdf(content, "doc");
    }
}