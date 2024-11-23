package com.symphony.bdk.workflow;

import com.symphony.bdk.gen.api.model.UserSystemInfo;
import com.symphony.bdk.gen.api.model.V2UserAttributes;
import com.symphony.bdk.gen.api.model.V2UserCreate;
import com.symphony.bdk.gen.api.model.V2UserDetail;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersIntegrationTest extends IntegrationTest {

  @Test
  void createUser() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/user/create-user.swadl.yaml"));

    when(userService.create(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));
    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/create-user"));

    ArgumentCaptor<V2UserCreate> userCreate = ArgumentCaptor.forClass(V2UserCreate.class);
    verify(userService, timeout(5000)).create(userCreate.capture());

    assertThat(userCreate.getValue()).satisfies(user -> {
      assertThat(user.getUserAttributes().getEmailAddress()).isEqualTo("john@mail.com");
      assertThat(user.getUserAttributes().getFirstName()).isEqualTo("John");
      assertThat(user.getUserAttributes().getLastName()).isEqualTo("Lee");
    });

    verify(userService, timeout(5000)).updateStatus(any(), any());
    verify(userService, timeout(5000)).updateFeatureEntitlements(any(), any());

    assertThat(workflow).isExecuted();
  }

  @Test
  void createSystemUser() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/create-system-user.swadl.yaml"));

    when(userService.create(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));
    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/create-user"));

    ArgumentCaptor<V2UserCreate> userCreate = ArgumentCaptor.forClass(V2UserCreate.class);
    verify(userService, timeout(5000)).create(userCreate.capture());

    assertThat(userCreate.getValue()).satisfies(user -> {
      assertThat(user.getUserAttributes().getCurrentKey().getAction()).isEqualTo("SAVE");
      assertThat(user.getUserAttributes().getCurrentKey().getKey()).isEqualTo("abc");
      assertThat(user.getUserAttributes().getCurrentKey().getExpirationDate()).isEqualTo(1629210917000L);
    });

    assertThat(workflow).isExecuted();
  }

  @Test
  void updateUser() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/user/update-user.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-user"));

    ArgumentCaptor<V2UserAttributes> userUpdate = ArgumentCaptor.forClass(V2UserAttributes.class);
    verify(userService, timeout(5000)).update(any(), userUpdate.capture());

    assertThat(userUpdate.getValue()).satisfies(user -> {
      assertThat(user.getEmailAddress()).isEqualTo("john@mail.com");
      assertThat(user.getFirstName()).isEqualTo("John");
      assertThat(user.getLastName()).isEqualTo("Lee");
    });

    verify(userService, timeout(5000)).updateStatus(any(), any());
    verify(userService, timeout(5000)).updateFeatureEntitlements(any(), any());

    assertThat(workflow).isExecuted();
  }

  @Test
  void updateUserContact() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/update-user-contact.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-user-contact"));

    ArgumentCaptor<V2UserAttributes> userUpdate = ArgumentCaptor.forClass(V2UserAttributes.class);
    verify(userService, timeout(5000)).update(any(), userUpdate.capture());

    assertThat(userUpdate.getValue()).satisfies(user -> {
      assertThat(user.getMobilePhoneNumber()).isEqualTo("123456789");
      assertThat(user.getWorkPhoneNumber()).isEqualTo("123456789");
      assertThat(user.getTwoFactorAuthPhone()).isEqualTo("123456789");
      assertThat(user.getSmsNumber()).isEqualTo("123456789");
    });

    assertThat(workflow).isExecuted();
  }

  @Test
  void updateUserBusiness() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/update-user-business.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-user-business"));

    ArgumentCaptor<V2UserAttributes> userUpdate = ArgumentCaptor.forClass(V2UserAttributes.class);
    verify(userService, timeout(5000)).update(any(), userUpdate.capture());

    assertThat(userUpdate.getValue()).satisfies(user -> {
      assertThat(user.getCompanyName()).isEqualTo("abc");
      assertThat(user.getDepartment()).isEqualTo("IT");
      assertThat(user.getDivision()).isEqualTo("div");
      assertThat(user.getJobFunction()).isEqualTo("Developer");
      assertThat(user.getAssetClasses()).isNotNull();
      assertThat(user.getAssetClasses().size()).isEqualTo(1);
      assertThat(user.getAssetClasses().get(0)).isEqualTo("Equities");
      assertThat(user.getLocation()).isEqualTo("SA");
      assertThat(user.getTitle()).isEqualTo("BackEnd Engineer");
      assertThat(user.getJobFunction()).isEqualTo("Developer");
      assertThat(user.getFunction()).isNotNull();
      assertThat(user.getFunction().size()).isEqualTo(1);
      assertThat(user.getFunction().get(0)).isEqualTo("Allocation");
      assertThat(user.getIndustries()).isNotNull();
      assertThat(user.getIndustries().size()).isEqualTo(1);
      assertThat(user.getIndustries().get(0)).isEqualTo("Technology");
      assertThat(user.getInstrument()).isNotNull();
      assertThat(user.getInstrument().size()).isEqualTo(1);
      assertThat(user.getInstrument().get(0)).isEqualTo("Securities");
      assertThat(user.getResponsibility()).isNotNull();
      assertThat(user.getResponsibility().size()).isEqualTo(1);
      assertThat(user.getResponsibility().get(0)).isEqualTo("BAU");
    });

    assertThat(workflow).isExecuted();
  }

  @Test
  void updateSystemUser() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/update-system-user.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail().userSystemInfo(new UserSystemInfo()));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-user"));

    ArgumentCaptor<V2UserAttributes> userUpdate = ArgumentCaptor.forClass(V2UserAttributes.class);
    verify(userService, timeout(5000)).update(any(), userUpdate.capture());

    assertThat(userUpdate.getValue()).satisfies(user ->
      assertThat(user.getDisplayName()).isEqualTo("Changed"));

    assertExecuted(workflow);
  }

  @Test
  void updateUser_statusOnly() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/update-user-status.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-user"));

    verify(userService, timeout(5000)).updateStatus(any(), any());
    verify(userService, never()).update(any(), any());

    assertThat(workflow).isExecuted();
  }

  @Test
  void addUserRole() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/add-user-role.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-user"));

    verify(userService, timeout(5000).times(2)).addRole(any(), any());
    assertThat(workflow).isExecuted();
  }

  @Test
  void removeUserRole() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/remove-user-role.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-user"));

    verify(userService, timeout(5000).times(2)).removeRole(any(), any());
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUser() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/user/get-user.swadl.yaml"));

    when(userService.getUserDetail(any())).thenReturn(new V2UserDetail());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-user"));

    verify(userService, timeout(5000)).getUserDetail(123L);
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUsersIds() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/get-users-ids.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-users"));

    verify(userService, timeout(5000)).listUsersByIds(List.of(123L, 456L), true, false);
    assertThat(workflow).isExecuted();
  }

  @ParameterizedTest
  @CsvSource({"/user/obo/get-users-ids-obo-valid-username.swadl.yaml, /get-users-ids-obo-valid-username",
      "/user/obo/get-users-ids-obo-valid-userid.swadl.yaml, /get-users-ids-obo-valid-userid"})
  void getUsersIdsObo(String workflowFile, String command) throws  Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboUserService, timeout(5000)).listUsersByIds(List.of(123L, 456L), true, false);
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUsersIdsOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/user/obo/get-users-ids-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-users-ids-obo-unauthorized"));

    assertThat(workflow).executed("getUsersIdsOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void getUsersEmails() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/get-users-emails.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-users"));

    verify(userService, timeout(5000)).listUsersByEmails(List.of("bob@mail.com", "eve@mail.com"), true, false);
    assertThat(workflow).isExecuted();
  }

  @ParameterizedTest
  @CsvSource({"/user/obo/get-users-emails-obo-valid-username.swadl.yaml, /get-users-emails-obo-valid-username",
      "/user/obo/get-users-emails-obo-valid-userid.swadl.yaml, /get-users-emails-obo-valid-userid"})
  void getUsersEmailsObo(String workflowFile, String command) throws  Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboUserService, timeout(5000)).listUsersByEmails(List.of("bob@mail.com", "eve@mail.com"), true, false);
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUsersEmailsOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/user/obo/get-users-emails-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-users-emails-obo-unauthorized"));

    assertThat(workflow).executed("getUsersEmailsOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void getUsersUsernames() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/user/get-users-usernames.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-users"));

    verify(userService, timeout(5000)).listUsersByUsernames(List.of("bob", "eve"), false);
    assertThat(workflow).isExecuted();
  }

  @ParameterizedTest
  @CsvSource({"/user/obo/get-users-usernames-obo-valid-username.swadl.yaml, /get-users-usernames-obo-valid-username",
      "/user/obo/get-users-usernames-obo-valid-userid.swadl.yaml, /get-users-usernames-obo-valid-userid"})
  void getUsersUsernamesObo(String workflowFile, String command) throws  Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboUserService, timeout(5000)).listUsersByUsernames(List.of("bob", "eve"), false);
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUsersUsernamesOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/user/obo/get-users-usernames-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-users-usernames-obo-unauthorized"));

    assertThat(workflow).executed("getUsersUsernamesOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void getUsersInvalidWorkflow() {
    assertThatThrownBy(
        () -> SwadlParser.fromYaml(getClass().getResourceAsStream("/user/get-users-invalid-workflow.swadl.yaml")))
        .describedAs("Should fail at validation time because none of user-ids, username or emails are given")
        .isInstanceOf(SwadlNotValidException.class);
  }
}
