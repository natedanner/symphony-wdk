package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.exception.DuplicateException;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RequiredArgsConstructor
@Slf4j
public abstract class WorkflowAbstractAction {

  private final WorkflowDeployer deployer;

  protected Workflow convertToWorkflow(String content) {
    try {
      return SwadlParser.fromYaml(content);
    } catch (Exception e) {
      throw new IllegalArgumentException("SWADL content is not valid");
    }
  }

  protected void validateFilePath(String path) {
    if (deployer.workflowSwadlPaths().contains(Path.of(path))) {
      throw new DuplicateException("SWADL file already exists");
    }
  }

  protected Path getWorkflowFilePath(String id) {
    return deployer.workflowSwadlPath(id);
  }

  protected boolean workflowExist(String id) {
    return deployer.workflowExist(id);
  }

  protected void writeFile(String content, Workflow workflow, String path) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8));
      writer.write(content);
      writer.close();
    } catch (IOException e) {
      log.error("Write swadl file failure", e);
      throw new RuntimeException("Failed to write SWADL file " + workflow.getId() + " due to " + e.getMessage());
    }
  }

  protected void deleteFile(String workflowId) {
    File file = deployer.workflowSwadlPath(workflowId).toFile();
    if (!file.delete()) {
      throw new NotFoundException(String.format("Workflow %s does not exist", workflowId));
    }
  }
}
