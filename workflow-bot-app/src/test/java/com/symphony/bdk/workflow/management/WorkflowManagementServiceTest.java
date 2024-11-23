package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.management.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Properties;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkflowManagementServiceTest {

  @Mock
  VersionedWorkflowRepository versionRepository;

  @Mock
  ObjectConverter conveter;

  @Mock
  WorkflowEngine<CamundaTranslatedWorkflowContext> camundaEngine;

  @InjectMocks
  WorkflowManagementService workflowManagementService;

  static final String swadl = "id: test\n"
      + "activities:\n"
      + "  - send-message:\n"
      + "      id: msg\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: msg\n"
      + "      content: content";

  static Workflow workflow;
  static SwadlView swadlView;

  @BeforeAll
  static void setup() throws IOException, ProcessingException {
    workflow = SwadlParser.fromYaml(swadl);
    swadlView = SwadlView.builder().swadl(swadl).description("desc").createdBy(1234L).build();
  }

  @Test
  void testDeploy_existingActiveVersion_updateOldInsertNew() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(camundaEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn("id");
    VersionedWorkflow activeVersion = new VersionedWorkflow();
    when(versionRepository.save(any())).thenReturn(activeVersion);
    when(versionRepository.saveAndFlush(any())).thenReturn(activeVersion);
    when(versionRepository.findByWorkflowIdAndActiveTrue(eq("test"))).thenReturn(Optional.of(activeVersion));

    workflowManagementService.deploy(swadlView);

    ArgumentCaptor<VersionedWorkflow> newVersionedWorkflowCaptor = ArgumentCaptor.forClass(VersionedWorkflow.class);
    ArgumentCaptor<VersionedWorkflow> oldVersionedWorkflowCaptor = ArgumentCaptor.forClass(VersionedWorkflow.class);

    verify(camundaEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
    verify(versionRepository).save(newVersionedWorkflowCaptor.capture());
    verify(versionRepository).saveAndFlush(oldVersionedWorkflowCaptor.capture());

    assertThat(newVersionedWorkflowCaptor.getValue().getActive())
        .as("The new version is set as active")
        .isTrue();
    assertThat(newVersionedWorkflowCaptor.getValue().getSwadl()).isEqualTo(swadlView.getSwadl());
    assertThat(newVersionedWorkflowCaptor.getValue().getCreatedBy()).isEqualTo(swadlView.getCreatedBy());
    assertThat(newVersionedWorkflowCaptor.getValue().getDescription()).isEqualTo(swadlView.getDescription());

    assertThat(oldVersionedWorkflowCaptor.getValue().getActive())
        .as("The old version is not active anymore")
        .isFalse();
  }

  @Test
  void testDeploy_noActiveVersion_insertNew() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(camundaEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn("id");
    when(versionRepository.save(any(VersionedWorkflow.class))).thenReturn(null);
    when(versionRepository.findByWorkflowIdAndActiveTrue(eq("test"))).thenReturn(Optional.empty());

    workflowManagementService.deploy(swadlView);

    ArgumentCaptor<VersionedWorkflow> newVersionedWorkflowCaptor = ArgumentCaptor.forClass(VersionedWorkflow.class);

    verify(camundaEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
    verify(versionRepository).save(newVersionedWorkflowCaptor.capture());

    assertThat(newVersionedWorkflowCaptor.getValue().getActive())
        .as("The new version is set as active")
        .isTrue();
    assertThat(newVersionedWorkflowCaptor.getValue().getSwadl()).isEqualTo(swadlView.getSwadl());
    assertThat(newVersionedWorkflowCaptor.getValue().getCreatedBy()).isEqualTo(swadlView.getCreatedBy());
    assertThat(newVersionedWorkflowCaptor.getValue().getDescription()).isEqualTo(swadlView.getDescription());
  }

  @Test
  void testDeploy_existNoPublishedVersion_exceptionThrown() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow noPublishedVersion = new VersionedWorkflow();
    noPublishedVersion.setPublished(false);
    noPublishedVersion.setVersion(1234L);
    when(versionRepository.findByWorkflowIdAndPublishedFalse(anyString())).thenReturn(Optional.of(noPublishedVersion));

    assertThatThrownBy(() -> workflowManagementService.deploy(swadlView)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Version 1234 of workflow has not been published yet.");
  }

  @Test
  void testUpdate_latestNoPublishedVersion_publishSucceed() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow noPublishedVersion = new VersionedWorkflow();
    noPublishedVersion.setPublished(false);
    when(versionRepository.findTopByWorkflowIdOrderByVersionDesc(anyString())).thenReturn(
        Optional.of(noPublishedVersion));
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(camundaEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn("id");
    when(versionRepository.save(any(VersionedWorkflow.class))).thenReturn(noPublishedVersion);

    workflowManagementService.update(swadlView);

    assertThat(noPublishedVersion.getDeploymentId()).isEqualTo("id");
    assertThat(noPublishedVersion.getActive()).isTrue();
    assertThat(noPublishedVersion.getPublished()).isTrue();
    verify(camundaEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
  }

  @Test
  void testUpdate_latestNoPublishedVersion_updateOnlySucceed() {
    Properties properties = new Properties();
    properties.setPublish(false);
    workflow.setProperties(properties);
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow noPublishedVersion = new VersionedWorkflow();
    noPublishedVersion.setPublished(false);
    noPublishedVersion.setActive(false);
    when(versionRepository.findTopByWorkflowIdOrderByVersionDesc(anyString())).thenReturn(
        Optional.of(noPublishedVersion));
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(versionRepository.save(any(VersionedWorkflow.class))).thenReturn(noPublishedVersion);

    workflowManagementService.update(swadlView);

    assertThat(noPublishedVersion.getActive()).isFalse();
    assertThat(noPublishedVersion.getPublished()).isFalse();
    verify(camundaEngine, never()).deploy(any(CamundaTranslatedWorkflowContext.class));
  }

  @Test
  void testUpdate_updatePublishedWorkflow() {
    Properties properties = new Properties();
    properties.setPublish(true);
    workflow.setProperties(properties);
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow publishedVersion = new VersionedWorkflow();
    publishedVersion.setPublished(true);
    when(versionRepository.findTopByWorkflowIdOrderByVersionDesc(anyString())).thenReturn(
        Optional.of(publishedVersion));

    assertThatThrownBy(() -> workflowManagementService.update(swadlView))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Update on a published Workflow is forbidden.");
  }

  @Test
  void testUpdate_noActiveVersion_notFoundException() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);

    assertThatThrownBy(() -> workflowManagementService.update(swadlView)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void testDelete_existingVersion_succeed() {
    VersionedWorkflow workflow = new VersionedWorkflow();
    workflow.setActive(true);
    workflow.setWorkflowId("id");
    workflow.setDeploymentId("deploymentId");
    doNothing().when(camundaEngine).undeployByDeploymentId(anyString());
    doNothing().when(versionRepository).deleteByWorkflowIdAndVersion(anyString(), anyLong());
    when(versionRepository.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(Optional.of(workflow));

    workflowManagementService.delete("id", 1674651222294886L);

    verify(camundaEngine).undeployByDeploymentId(anyString());
    verify(versionRepository).deleteByWorkflowIdAndVersion(anyString(), anyLong());
  }

  @Test
  void testDelete_withoutVersion_succeed() {
    doNothing().when(camundaEngine).undeployByWorkflowId(anyString());
    doNothing().when(versionRepository).deleteByWorkflowId(anyString());

    workflowManagementService.delete("id", null);

    verify(camundaEngine).undeployByWorkflowId(anyString());
    verify(versionRepository).deleteByWorkflowId(anyString());
  }

  @Test
  void testSetActiveVersion_switchActiveWorkflow() {
    String workflowId = "workflowId";
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflowId);
    versionedWorkflow.setSwadl(swadl);
    versionedWorkflow.setPublished(true);
    VersionedWorkflow activeWorkflow = new VersionedWorkflow();
    activeWorkflow.setActive(true);

    when(versionRepository.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(
        Optional.of(versionedWorkflow));
    when(versionRepository.findByWorkflowIdAndActiveTrue(anyString())).thenReturn(Optional.of(activeWorkflow));
    when(conveter.convert(anyString(), eq(1674651222294886L), eq(Workflow.class))).thenReturn(workflow);
    String deploymentId = "ABC";
    when(camundaEngine.deploy(any(Workflow.class))).thenReturn(deploymentId);
    when(versionRepository.save(any())).thenReturn(versionedWorkflow);
    when(versionRepository.saveAndFlush(any())).thenReturn(activeWorkflow);

    workflowManagementService.setActiveVersion(workflowId, 1674651222294886L);

    verify(camundaEngine).deploy(any(Workflow.class));
    verify(versionRepository).save(any());
    assertThat(activeWorkflow.getActive()).isFalse();
    assertThat(versionedWorkflow.getActive()).isTrue();
    assertThat(versionedWorkflow.getDeploymentId()).isEqualTo(deploymentId);
  }

  @Test
  void testSetActiveVersion_workflowNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
      workflowManagementService.setActiveVersion("notFoundWorkflowId", 1674651222294886L)).satisfies(
        e -> assertThat(e.getMessage())
            .isEqualTo("Version 1674651222294886 of the workflow notFoundWorkflowId does not exist."));
  }

  @Test
  void testSetActiveVersion_workflowInDraft_illegalException() {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId("inactiveWorkflow");
    versionedWorkflow.setSwadl(swadl);
    versionedWorkflow.setPublished(false);
    when(versionRepository.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(
        Optional.of(versionedWorkflow));
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
      workflowManagementService.setActiveVersion("inactiveWorkflow", 1674651222294886L)).satisfies(
        e -> assertThat(e.getMessage())
            .isEqualTo("Version 1674651222294886 of the workflow inactiveWorkflow is in draft mode."));
  }
}
