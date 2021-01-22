package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;


@Slf4j
@Service
public class BulkPrintServiceHelper {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM Y HH:mm");

    private static final ZoneId ZONE_ID_LONDON = ZoneId.of("Europe/London");

    @Autowired
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    public BulkPrintServiceHelper(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    protected boolean sendForReasonableAdjustMent(SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event) {
        if (sscsCaseData.getReasonableAdjustments() != null) {
            if (letterType.equals(APPELLANT_LETTER)) {
                if (sscsCaseData.getReasonableAdjustments().getAppellant() != null) {
                    if (YesNo.isYes(sscsCaseData.getReasonableAdjustments().getAppellant().getWantsReasonableAdjustment())) {
                        return true;
                    }
                }
            } else if (letterType.equals(REPRESENTATIVE_LETTER)) {
                if (sscsCaseData.getReasonableAdjustments().getRepresentative() != null) {
                    if (YesNo.isYes(sscsCaseData.getReasonableAdjustments().getRepresentative().getWantsReasonableAdjustment())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Correspondence> saveAsReasonableAdjustment(SscsCaseData sscsCaseData, List<Pdf> pdfs, FurtherEvidenceLetterType letterType, EventType event) {
        String name = "";
        if (letterType.equals(APPELLANT_LETTER)) {
            log.info("Adding a reasonable adjustment letter for the appellant on case {}", sscsCaseData.getCcdCaseId());
            name = sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
        } else if (letterType.equals(REPRESENTATIVE_LETTER)) {
            log.info("Adding a reasonable adjustment letter for the rep on case {}", sscsCaseData.getCcdCaseId());
            name = sscsCaseData.getAppeal().getRep().getName().getFullNameNoTitle();
        }
        final Correspondence correspondence = getLetterCorrespondence(name, event);

        return ccdNotificationsPdfService.getReasonableAdjustmentsCorrespondence(pdfs,
            Long.valueOf(sscsCaseData.getCcdCaseId()), correspondence);
    }

    private Correspondence getLetterCorrespondence(String name, EventType event) {
        return Correspondence.builder().value(
            CorrespondenceDetails.builder()
                .to(name)
                .correspondenceType(CorrespondenceType.Letter)
                .sentOn(LocalDateTime.now(ZONE_ID_LONDON).format(DATE_TIME_FORMATTER))
                .eventType("stoppedForReasonableAdjustment")
                .build()
        ).build();
    }

    public static SscsCaseData addReasonableAdjustments(List<Correspondence> newReasonableAdjustments, SscsCaseData sscsCaseData) {
        if (newReasonableAdjustments.size() > 0) {
            List<Correspondence> existingCorrespondence = sscsCaseData.getReasonableAdjustmentsLetters() == null ? new ArrayList<>() : sscsCaseData.getReasonableAdjustmentsLetters();
            List<Correspondence> allCorrespondence = new ArrayList<>(existingCorrespondence);
            allCorrespondence.addAll(newReasonableAdjustments);
            allCorrespondence.sort(Comparator.reverseOrder());
            sscsCaseData.setReasonableAdjustmentsLetters(allCorrespondence);
            sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        }
        return sscsCaseData;
    }

}
