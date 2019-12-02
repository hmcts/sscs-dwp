package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.DWP_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.util.Arrays;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.FurtherEvidenceService;

@RunWith(JUnitParamsRunner.class)
public class IssueFurtherEvidenceHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private FurtherEvidenceService furtherEvidenceService;

    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    @InjectMocks
    private IssueFurtherEvidenceHandler issueFurtherEvidenceHandler;

    @Captor
    ArgumentCaptor<SscsCaseData> captor;

    private SscsDocument sscsDocumentNotIssued = SscsDocument.builder()
        .value(SscsDocumentDetails.builder()
            .documentType(APPELLANT_EVIDENCE.getValue())
            .evidenceIssued("No")
            .build())
        .build();

    private SscsCaseData caseData = SscsCaseData.builder()
        .ccdCaseId("1563382899630221")
        .sscsDocument(Collections.singletonList(sscsDocumentNotIssued))
        .appeal(Appeal.builder().build())
        .build();

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        issueFurtherEvidenceHandler.handle(callbackType,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"REISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenEventTypeIsNotIssueFurtherEvidence_willThrowAnException(EventType eventType) {
        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, eventType));
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_shouldThrowException() {
        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        issueFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        issueFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    public void givenHandleMethodIsCalled_shouldThrowExceptionIfCanNotBeHandled() {
        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(false);

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void givenExceptionWhenIssuingFurtherEvidence_shouldHandleItAppropriately() {
        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(true);
        doThrow(RuntimeException.class).when(furtherEvidenceService).issue(any(), any(), any(), any());

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED, buildTestCallbackForGivenData(caseData,
            INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));

        verifyNoInteractions(ccdService);
        assertEquals("hmctsDwpState has incorrect value", "failedSendingFurtherEvidence",
            caseData.getHmctsDwpState());
    }

    @Test
    public void givenIssueFurtherEvidenceCallback_shouldIssueEvidenceForAppellantAndRepAndDwp() {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(true);

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(APPELLANT_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER)));
        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(REPRESENTATIVE_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER)));
        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(DWP_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER)));

        verify(ccdService).updateCase(captor.capture(), any(Long.class), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
            any(), any(), any(IdamTokens.class));

        assertEquals("Yes", captor.getValue().getSscsDocument().get(0).getValue().getEvidenceIssued());
    }
}
