package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.robotics.domain.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.robotics.json.RoboticsJsonMapper;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RoboticsServiceIt {

    @Autowired
    private RoboticsService roboticsService;

    @Autowired
    private RoboticsJsonMapper mapper;

    private SscsCaseData caseData;

    private RoboticsWrapper roboticsWrapper;

    private CaseDetails<SscsCaseData> caseDetails;

    @MockBean
    private EmailService emailService;

    @Before
    public void setup() {
        caseData = SscsCaseData.builder()
            .ccdCaseId("123456")
            .regionalProcessingCenter(null)
            .evidencePresent("Yes")
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .receivedVia("paper")
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("RH5 6PO").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .contact(Contact.builder().mobile(null).email(null).build())
                    .build())
                .hearingOptions(HearingOptions.builder().wantsToAttend("Yes").other("My hearing").build())
                .build())
            .build();

        caseDetails = new CaseDetails<>(1234L, "sscs", APPEAL_CREATED, caseData, null);
    }

    @Test
    public void givenSscsCaseDataWithRep_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        caseData.getAppeal().setRep(Representative.builder()
            .hasRepresentative("Yes").name(Name.builder().title("Mrs").firstName("Wendy").lastName("Barker").build())
            .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("RH5 6PO").build())
            .contact(Contact.builder().mobile(null).email(null).build())
            .build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
    }

    @Test
    public void givenSscsCaseDataWithoutRepresentativeWithReadyToListFeatureFalse_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        ReflectionTestUtils.setField(mapper, "readyToListFeatureEnabled", false);

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertFalse(result.has("isReadyToList"));
    }

    @Test
    public void givenSscsCaseDataWithoutReadyToListFeatureTrue_makeValidRoboticsJsonThatValidatesAgainstSchemaWithReadyToListField() {
        ReflectionTestUtils.setField(mapper, "readyToListFeatureEnabled", true);

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
    }

    @Test
    public void givenSscsCaseDataWithoutHearingArrangements_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseData.getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertFalse(result.has("hearingArrangements"));
    }
}
