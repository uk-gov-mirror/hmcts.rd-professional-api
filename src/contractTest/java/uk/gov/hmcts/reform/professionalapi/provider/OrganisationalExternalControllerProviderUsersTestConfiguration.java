package uk.gov.hmcts.reform.professionalapi.provider;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.professionalapi.controller.external.OrganisationExternalController;
import uk.gov.hmcts.reform.professionalapi.controller.request.validator.UserProfileUpdateRequestValidator;
import uk.gov.hmcts.reform.professionalapi.repository.ContactInformationRepository;
import uk.gov.hmcts.reform.professionalapi.repository.DxAddressRepository;
import uk.gov.hmcts.reform.professionalapi.repository.IdamRepository;
import uk.gov.hmcts.reform.professionalapi.repository.OrganisationRepository;
import uk.gov.hmcts.reform.professionalapi.repository.PaymentAccountRepository;
import uk.gov.hmcts.reform.professionalapi.repository.PrdEnumRepository;
import uk.gov.hmcts.reform.professionalapi.service.FeatureToggleService;
import uk.gov.hmcts.reform.professionalapi.service.PaymentAccountService;
import uk.gov.hmcts.reform.professionalapi.service.ProfessionalUserService;
import uk.gov.hmcts.reform.professionalapi.service.UserAttributeService;
import uk.gov.hmcts.reform.professionalapi.service.impl.OrganisationServiceImpl;
import uk.gov.hmcts.reform.professionalapi.service.impl.PaymentAccountServiceImpl;

@Configuration
public class OrganisationalExternalControllerProviderUsersTestConfiguration extends ProviderTestConfiguration {

    @MockBean
    protected ProfessionalUserService professionalUserService;

    @MockBean
    protected ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    protected UserProfileUpdateRequestValidator userProfileUpdateRequestValidator;

    @MockBean
    FeatureToggleService featureToggleService;

    @MockBean
    ServiceAuthFilter serviceAuthFilter;

    @MockBean
    IdamRepository idamRepository;

    @MockBean
    OrganisationRepository organisationRepository;

    @MockBean
    PaymentAccountRepository paymentAccountRepository;
    @MockBean
    DxAddressRepository dxAddressRepository;
    @MockBean
    ContactInformationRepository contactInformationRepository;
    @MockBean
    PrdEnumRepository prdEnumRepository;
    @MockBean
    UserAttributeService userAttributeService;

    @Bean
    @Primary
    protected OrganisationServiceImpl organisationService() {
        return new OrganisationServiceImpl();
    }

    @Bean
    @Primary
    public PaymentAccountService paymentAccountService() {
        return new PaymentAccountServiceImpl(configuration, userProfileFeignClient,
            emf, professionalUserRepository, organisationService(),
            userAccountMapService);
    }

    @Bean
    @Primary
    public OrganisationExternalController organisationExternalController() {
        return new OrganisationExternalController();
    }


    @Bean
    @Primary
    public PactJwtGrantedAuthoritiesConverter pactJwtGrantedAuthoritiesConverter() {
        return new PactJwtGrantedAuthoritiesConverter(idamRepository);
    }
}
