/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author roman.smirnov
 */
@ExtendWith(ProcessEngineExtension.class)
class SuspendJobTest {

  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @Test
  void testSuspensionById_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.suspendJobById(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("jobId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionById_shouldSuspendJob() {
    // given

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isFalse();

    // when
    // the job will be suspended
    managementService.suspendJobById(job.getId());

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getId()).isEqualTo(job.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByJobDefinitionId_shouldSuspendJob() {
    // given

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // the job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isFalse();

    // when
    // the job will be suspended
    managementService.suspendJobByJobDefinitionId(jobDefinition.getId());

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getId()).isEqualTo(job.getId());
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByProcessInstanceId_shouldSuspendJob() {
    // given

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // the job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isFalse();

    // when
    // the job will be suspended
    managementService.suspendJobByProcessInstanceId(processInstance.getId());

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getId()).isEqualTo(job.getId());
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByProcessDefinitionId_shouldSuspendJob() {
    // given
    // a deployed process definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isFalse();

    // when
    // the job will be suspended
    managementService.suspendJobByProcessDefinitionId(processDefinition.getId());

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getId()).isEqualTo(job.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByProcessDefinitionKey_shouldSuspendJob() {
    // given
    // a deployed process definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isFalse();

    // when
    // the job will be suspended
    managementService.suspendJobByProcessDefinitionKey(processDefinition.getKey());

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getId()).isEqualTo(job.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Test
  void testMultipleSuspensionByProcessDefinitionKey_shouldSuspendJob() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i < nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // when
    // the job will be suspended
    managementService.suspendJobByProcessDefinitionKey(key);

    // then
    // the job should be suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isFalse();

    // when
    // the job will be suspended
    managementService
      .updateJobSuspensionState()
      .byJobId(job.getId())
      .suspend();

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByJobDefinitionIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.active().count()).isEqualTo(1);

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // the job will be suspended
    managementService
      .updateJobSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .suspend();

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByProcessInstanceIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.active().count()).isEqualTo(1);

    // when
    // the job will be suspended
    managementService
      .updateJobSuspensionState()
      .byProcessInstanceId(processInstance.getId())
      .suspend();

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByProcessDefinitionIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.active().count()).isEqualTo(1);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    // the job will be suspended
    managementService
      .updateJobSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .suspend();

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testSuspensionByProcessDefinitionKeyUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.active().count()).isEqualTo(1);

    // when
    // the job will be suspended
    managementService
      .updateJobSuspensionState()
      .byProcessDefinitionKey("suspensionProcess")
      .suspend();

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);
  }

}
