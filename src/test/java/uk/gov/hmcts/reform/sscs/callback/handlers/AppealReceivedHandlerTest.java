package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.callback.handlers.HandlerHelper.buildTestCallbackForGivenData;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class AppealReceivedHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    private AppealReceivedHandler handler;


    @Before
    public void setUp() {
        handler = new AppealReceivedHandler(ccdCaseService, idamService);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, INTERLOC_VALID_APPEAL));
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    public void givenAValidAppealReceivedEventForDigitalCase_thenReturnTrue(EventType eventType) {
        assertTrue(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), INTERLOCUTORY_REVIEW_STATE, eventType)));
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    public void givenAValidAppealReceivedEventForNonDigitalCase_thenReturnFalse(EventType eventType) {
        assertFalse(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, eventType)));
    }

    @Test
    public void givenANonAppealEvent_thenReturnFalse() {
        assertFalse(handler.canHandle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), INTERLOCUTORY_REVIEW_STATE, DECISION_ISSUED)));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        handler.canHandle(SUBMITTED, null);
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "VALID_APPEAL", "INTERLOC_VALID_APPEAL"})
    public void givenValidEventAndDigitalCase_thenTriggerAppealReceivedEvent(EventType eventType) {
        handler.handle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(READY_TO_LIST.getId()).build(), INTERLOCUTORY_REVIEW_STATE, eventType));

        verify(ccdCaseService).updateCase(any(), eq(1L), eq(EventType.APPEAL_RECEIVED.getCcdType()), eq("Appeal received"), eq("Appeal received event has been triggered from Evidence Share for digital case"), any());
    }

    @Test(expected = IllegalStateException.class)
    public void givenValidEventAndNonDigitalCase_thenThrowException() {
        handler.handle(SUBMITTED, buildTestCallbackForGivenData(SscsCaseData.builder().createdInGapsFrom(VALID_APPEAL.getId()).build(), INTERLOCUTORY_REVIEW_STATE, EventType.VALID_APPEAL_CREATED));

        verify(ccdCaseService, times(0)).updateCase(any(), eq(1L), eq(EventType.APPEAL_RECEIVED.getCcdType()), eq("Appeal received"), eq("Appeal received event has been triggered from Evidence Share for digital case"), any());
    }
}
