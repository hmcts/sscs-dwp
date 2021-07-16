package uk.gov.hmcts.reform.sscs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;

@SpringBootApplication(
    scanBasePackages = {"uk.gov.hmcts.reform.sscs"})
@EnableCircuitBreaker
@EnableFeignClients(basePackages =
    {
        "uk.gov.hmcts.reform.sendletter",
        "uk.gov.hmcts.reform.sscs.idam",
        "uk.gov.hmcts.reform.sscs.document",
        "uk.gov.hmcts.reform.ccd.client",
        "uk.gov.hmcts.reform.idam"
    })
@EnableRetry
@ComponentScan(
    basePackages = {"uk.gov.hmcts.reform.sscs", "uk.gov.hmcts.reform.ccd.document.am"},
    basePackageClasses = SscsEvidenceShareApplication.class,
    lazyInit = true
)
@EnableScheduling
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class SscsEvidenceShareApplication {


    @Bean
    public CcdRequestDetails getRequestDetails(@Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
                                               @Value("${core_case_data.caseTypeId}") String coreCaseDataCaseTypeId) {
        return CcdRequestDetails.builder()
            .caseTypeId(coreCaseDataCaseTypeId)
            .jurisdictionId(coreCaseDataJurisdictionId)
            .build();
    }

    public static void main(final String[] args) {
        SpringApplication.run(SscsEvidenceShareApplication.class, args);
    }
}
