package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.OTHER_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;

@RunWith(JUnitParamsRunner.class)
public class SscsDocumentToPdfServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @InjectMocks
    private SscsDocumentToPdfService sscsDocumentToPdfService;

    @Test
    @Parameters({
        "APPELLANT_EVIDENCE,appellantEvidenceDoc",
        "REPRESENTATIVE_EVIDENCE,repsEvidenceDoc",
        "OTHER_DOCUMENT,otherEvidenceDoc"
    })
    public void getPdfsForGivenDocType(DocumentType documentType, String expectedDocName) {

        given(evidenceManagementService.download(ArgumentMatchers.any(URI.class), eq("sscs")))
            .willReturn(new byte[]{'a'});

        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl("http://documentUrl")
            .build();
        SscsDocument sscsDocumentAppellantType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("appellantEvidenceDoc")
                .documentType(APPELLANT_EVIDENCE.getValue())
                .documentLink(documentLink)
                .build())
            .build();
        SscsDocument sscsDocumentRepsType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("repsEvidenceDoc")
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .documentLink(documentLink)
                .build())
            .build();
        SscsDocument sscsDocumentOtherType = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("otherEvidenceDoc")
                .documentType(OTHER_DOCUMENT.getValue())
                .documentLink(documentLink)
                .build())
            .build();

        List<Pdf> actualPdfs = sscsDocumentToPdfService.getPdfsForGivenDocType(
            Arrays.asList(sscsDocumentAppellantType, sscsDocumentRepsType, sscsDocumentOtherType), documentType);

        assertThat(actualPdfs, hasSize(1));
        assertThat(actualPdfs, hasItems(
            new Pdf(new byte[]{'a'}, expectedDocName)
        ));

    }
}
