package uk.gov.hmcts.reform.sscs.callback.handlers;

import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {
        return true;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {

    }
}