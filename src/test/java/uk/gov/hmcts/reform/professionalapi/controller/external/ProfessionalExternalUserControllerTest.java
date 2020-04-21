package uk.gov.hmcts.reform.professionalapi.controller.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;
import uk.gov.hmcts.reform.professionalapi.controller.constants.TestConstants;
import uk.gov.hmcts.reform.professionalapi.controller.feign.UserProfileFeignClient;
import uk.gov.hmcts.reform.professionalapi.controller.request.InvalidRequest;
import uk.gov.hmcts.reform.professionalapi.controller.request.validator.OrganisationCreationRequestValidator;
import uk.gov.hmcts.reform.professionalapi.controller.request.validator.ProfessionalUserReqValidator;
import uk.gov.hmcts.reform.professionalapi.controller.request.validator.impl.OrganisationIdentifierValidatorImpl;
import uk.gov.hmcts.reform.professionalapi.controller.response.NewUserResponse;
import uk.gov.hmcts.reform.professionalapi.domain.Organisation;
import uk.gov.hmcts.reform.professionalapi.domain.OrganisationStatus;
import uk.gov.hmcts.reform.professionalapi.domain.ProfessionalUser;
import uk.gov.hmcts.reform.professionalapi.domain.SuperUser;
import uk.gov.hmcts.reform.professionalapi.service.OrganisationService;
import uk.gov.hmcts.reform.professionalapi.service.ProfessionalUserService;

public class ProfessionalExternalUserControllerTest {

    private OrganisationService organisationServiceMock;
    private ProfessionalUserService professionalUserServiceMock;
    private Organisation organisation;
    private ProfessionalUserReqValidator profExtUsrReqValidator;
    private OrganisationIdentifierValidatorImpl organisationIdentifierValidatorImpl;
    private OrganisationCreationRequestValidator organisationCreationRequestValidator;
    private ResponseEntity<Object> responseEntity;
    private ProfessionalUser professionalUser;
    private UserProfileFeignClient userProfileFeignClient;


    @InjectMocks
    private ProfessionalExternalUserController professionalExternalUserController;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        organisation = new Organisation("Org-Name", OrganisationStatus.PENDING, "sra-id", "companyN", false, "www.org.com");
        professionalUser = new ProfessionalUser("fName", "lName", "user@test.com", organisation);
        organisationServiceMock = mock(OrganisationService.class);
        professionalUserServiceMock = mock(ProfessionalUserService.class);
        organisationIdentifierValidatorImpl = mock(OrganisationIdentifierValidatorImpl.class);
        profExtUsrReqValidator = mock(ProfessionalUserReqValidator.class);
        organisationCreationRequestValidator = mock(OrganisationCreationRequestValidator.class);
        responseEntity = mock(ResponseEntity.class);
        userProfileFeignClient = mock(UserProfileFeignClient.class);

        organisation.setOrganisationIdentifier(UUID.randomUUID().toString());

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFindUsersByOrganisation() {
        final HttpStatus expectedHttpStatus = HttpStatus.OK;
        ProfessionalUser professionalUser = new ProfessionalUser("fName", "lastName", "emailAddress", organisation);

        List<SuperUser> users = new ArrayList<>();
        users.add(professionalUser.toSuperUser());
        organisation.setUsers(users);
        organisation.setStatus(OrganisationStatus.ACTIVE);

        Authentication authentication = mock(Authentication.class);
        GrantedAuthority grantedAuthority = mock(GrantedAuthority.class);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        when(grantedAuthority.getAuthority()).thenReturn(TestConstants.PUI_USER_MANAGER);
        authorities.add(grantedAuthority);

        ServiceAndUserDetails serviceAndUserDetails = mock(ServiceAndUserDetails.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(securityContext.getAuthentication().getPrincipal()).thenReturn(serviceAndUserDetails);
        when(serviceAndUserDetails.getAuthorities()).thenReturn(authorities);
        SecurityContextHolder.setContext(securityContext);

        organisation.setStatus(OrganisationStatus.ACTIVE);

        when(organisationServiceMock.getOrganisationByOrgIdentifier(organisation.getOrganisationIdentifier())).thenReturn(organisation);
        when(professionalUserServiceMock.findProfessionalUserProfileByEmailAddress("emailAddress")).thenReturn(professionalUser);
        when(professionalUserServiceMock.findProfessionalUsersByOrganisation(any(Organisation.class), any(String.class), any(Boolean.class), any(String.class))).thenReturn(responseEntity);
        when(organisationIdentifierValidatorImpl.ifUserRoleExists(authorities, TestConstants.PUI_USER_MANAGER)).thenReturn(true);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        doNothing().when(profExtUsrReqValidator).validateRequest(any(String.class), any(String.class), any(String.class));
        doNothing().when(organisationIdentifierValidatorImpl).validate(any(Organisation.class), any(OrganisationStatus.class), any(String.class));
        doNothing().when(organisationCreationRequestValidator).validateOrganisationIdentifier(any(String.class));

        ResponseEntity<?> actual = professionalExternalUserController.findUsersByOrganisation(organisation.getOrganisationIdentifier(), "true", "", null, null);

        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode().value()).isEqualTo(expectedHttpStatus.value());

        verify(organisationServiceMock, times(1)).getOrganisationByOrgIdentifier(organisation.getOrganisationIdentifier());
        verify(professionalUserServiceMock, times(1)).findProfessionalUsersByOrganisation(organisation, "true", true, "");
    }

    @Test
    public void testFindUsersByOrganisationWithPuiCaseManager() {
        final HttpStatus expectedHttpStatus = HttpStatus.OK;
        ProfessionalUser professionalUser = new ProfessionalUser("fName", "lastName", "emailAddress", organisation);

        List<SuperUser> users = new ArrayList<>();
        users.add(professionalUser.toSuperUser());
        organisation.setUsers(users);
        organisation.setStatus(OrganisationStatus.ACTIVE);

        Authentication authentication = mock(Authentication.class);
        GrantedAuthority grantedAuthority = mock(GrantedAuthority.class);
        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        when(grantedAuthority.getAuthority()).thenReturn(TestConstants.PUI_USER_MANAGER);
        authorities.add(grantedAuthority);

        ServiceAndUserDetails serviceAndUserDetails = mock(ServiceAndUserDetails.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(securityContext.getAuthentication().getPrincipal()).thenReturn(serviceAndUserDetails);
        when(serviceAndUserDetails.getAuthorities()).thenReturn(authorities);
        SecurityContextHolder.setContext(securityContext);

        organisation.setStatus(OrganisationStatus.ACTIVE);

        when(organisationServiceMock.getOrganisationByOrgIdentifier(organisation.getOrganisationIdentifier())).thenReturn(organisation);
        when(professionalUserServiceMock.findProfessionalUserProfileByEmailAddress("emailAddress")).thenReturn(professionalUser);
        when(professionalUserServiceMock.findProfessionalUsersByOrganisation(any(Organisation.class), any(String.class), any(Boolean.class), any(String.class))).thenReturn(responseEntity);
        when(organisationIdentifierValidatorImpl.ifUserRoleExists(authorities, TestConstants.PUI_CASE_MANAGER)).thenReturn(true);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        doNothing().when(profExtUsrReqValidator).validateRequest(any(String.class), any(String.class), any(String.class));
        doNothing().when(organisationIdentifierValidatorImpl).validate(any(Organisation.class), any(OrganisationStatus.class), any(String.class));
        doNothing().when(organisationCreationRequestValidator).validateOrganisationIdentifier(any(String.class));

        ResponseEntity<?> actual = professionalExternalUserController.findUsersByOrganisation(organisation.getOrganisationIdentifier(), "true", "", null, null);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode().value()).isEqualTo(expectedHttpStatus.value());

        verify(organisationServiceMock, times(1)).getOrganisationByOrgIdentifier(organisation.getOrganisationIdentifier());
        verify(professionalUserServiceMock, times(1)).findProfessionalUsersByOrganisation(organisation, "true", true, "Active");
    }

    @Test
    public void testFindUserByEmailWithPuiUserManager() {
        final HttpStatus expectedHttpStatus = HttpStatus.OK;

        ProfessionalUser professionalUser = new ProfessionalUser("fName", "lastName", "test@email.com", organisation);
        List<SuperUser> users = new ArrayList<>();
        users.add(professionalUser.toSuperUser());
        organisation.setUsers(users);
        organisation.setStatus(OrganisationStatus.ACTIVE);

        Authentication authentication = mock(Authentication.class);
        GrantedAuthority grantedAuthority = mock(GrantedAuthority.class);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        when(grantedAuthority.getAuthority()).thenReturn(TestConstants.PUI_USER_MANAGER);
        authorities.add(grantedAuthority);
        ServiceAndUserDetails serviceAndUserDetails = mock(ServiceAndUserDetails.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(securityContext.getAuthentication().getPrincipal()).thenReturn(serviceAndUserDetails);
        when(serviceAndUserDetails.getAuthorities()).thenReturn(authorities);
        SecurityContextHolder.setContext(securityContext);

        organisation.setStatus(OrganisationStatus.ACTIVE);

        when(organisationServiceMock.getOrganisationByOrgIdentifier(organisation.getOrganisationIdentifier())).thenReturn(organisation);
        when(professionalUserServiceMock.findProfessionalUserProfileByEmailAddress("testing@email.com")).thenReturn(professionalUser);
        when(organisationIdentifierValidatorImpl.ifUserRoleExists(authorities, TestConstants.PUI_USER_MANAGER)).thenReturn(true);
        when(responseEntity.getStatusCode()).thenReturn(HttpStatus.OK);

        doNothing().when(profExtUsrReqValidator).validateRequest(any(String.class), any(String.class), any(String.class));
        doNothing().when(organisationIdentifierValidatorImpl).validate(any(Organisation.class), any(OrganisationStatus.class), any(String.class));
        doNothing().when(organisationCreationRequestValidator).validateOrganisationIdentifier(any(String.class));

        Optional<ResponseEntity> actual = professionalExternalUserController.findUserByEmail(organisation.getOrganisationIdentifier(), "testing@email.com");
        assertThat(actual).isNotNull();
        assertThat(actual.get().getStatusCode().value()).isEqualTo(expectedHttpStatus.value());

        verify(professionalUserServiceMock, times(1)).findProfessionalUserProfileByEmailAddress("testing@email.com");
    }

    @Test
    public void testFindUserStatusByEmail() throws JsonProcessingException {
        organisation.setStatus(OrganisationStatus.ACTIVE);
        professionalUser.getOrganisation().setStatus(OrganisationStatus.ACTIVE);

        NewUserResponse newUserResponse = new NewUserResponse();
        newUserResponse.setUserIdentifier("a123dfgr46");
        newUserResponse.setIdamStatus("ACTIVE");
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(newUserResponse);

        ResponseEntity<NewUserResponse> responseEntity1 = new ResponseEntity<NewUserResponse>(newUserResponse, HttpStatus.OK);

        when(userProfileFeignClient.getUserProfileByEmail(anyString())).thenReturn(Response.builder().request(mock(Request.class)).body(body, Charset.defaultCharset()).status(200).build());
        when(professionalUserServiceMock.findUserStatusByEmailAddress(professionalUser.getEmailAddress())).thenReturn(responseEntity1);

        ResponseEntity<NewUserResponse> newResponse = professionalExternalUserController.findUserStatusByEmail(professionalUser.getEmailAddress());

        assertThat(newResponse).isNotNull();
        assertThat(newResponse.getBody()).isNotNull();
        assertThat(newResponse.getBody().getUserIdentifier()).isEqualTo("a123dfgr46");

        verify(professionalUserServiceMock, times(1)).findUserStatusByEmailAddress(professionalUser.getEmailAddress());
    }

    @Test(expected = InvalidRequest.class)
    public void testFindUserByEmailWithPuiUserManagerThrows400WithInvalidEmail() {
        Optional<ResponseEntity> actual = professionalExternalUserController.findUserByEmail(organisation.getOrganisationIdentifier(), "invalid-email");

        assertThat(actual).isNotNull();
        assertThat(actual.get().getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(organisationCreationRequestValidator, times(1)).validateEmail("invalid-email");
    }
}