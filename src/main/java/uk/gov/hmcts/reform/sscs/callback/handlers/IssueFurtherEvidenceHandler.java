package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;

import java.util.List;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;

public class IssueFurtherEvidenceHandler implements CallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {
        requireNonNull(callback, "callback must not be null");
        return canHandleAnyDocument(callback.getCaseDetails().getCaseData().getSscsDocument());
    }

    private boolean canHandleAnyDocument(List<SscsDocument> sscsDocument) {
        return null != sscsDocument && sscsDocument.stream().anyMatch(this::canHandleDocument);
    }

    private boolean canHandleDocument(SscsDocument sscsDocument) {

        return sscsDocument != null && sscsDocument.getValue() != null
            && "No".equals(sscsDocument.getValue().getEvidenceIssued())
            && DocumentType.APPELLANT_EVIDENCE.getValue().equals(sscsDocument.getValue().getDocumentType());
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {

    }
}
