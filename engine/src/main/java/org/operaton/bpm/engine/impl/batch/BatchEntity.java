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
package org.operaton.bpm.engine.impl.batch;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogManager;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionManager;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.Nameable;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceManager;
import org.operaton.bpm.engine.impl.persistence.entity.util.ByteArrayField;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ResourceTypes;

public class BatchEntity implements Batch, DbEntity, HasDbReferences, Nameable, HasDbRevision {

  public static final BatchSeedJobDeclaration BATCH_SEED_JOB_DECLARATION = new BatchSeedJobDeclaration();
  public static final BatchMonitorJobDeclaration BATCH_MONITOR_JOB_DECLARATION = new BatchMonitorJobDeclaration();

  // persistent
  protected String id;
  protected String type;

  protected int totalJobs;
  protected int jobsCreated;
  protected int batchJobsPerSeed;
  protected int invocationsPerBatchJob;

  protected String seedJobDefinitionId;
  protected String monitorJobDefinitionId;
  protected String batchJobDefinitionId;

  protected ByteArrayField configuration = new ByteArrayField(this, ResourceTypes.RUNTIME);

  protected String tenantId;
  protected String createUserId;

  protected int suspensionState = SuspensionState.ACTIVE.getStateCode();

  protected Date startTime;
  protected Date executionStartTime;

  protected int revision;

  // transient
  protected JobDefinitionEntity seedJobDefinition;
  protected JobDefinitionEntity monitorJobDefinition;
  protected JobDefinitionEntity batchJobDefinition;

  protected BatchJobHandler<?> batchJobHandler;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return id;
  }

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public int getTotalJobs() {
    return totalJobs;
  }

  public void setTotalJobs(int totalJobs) {
    this.totalJobs = totalJobs;
  }

  @Override
  public int getJobsCreated() {
    return jobsCreated;
  }

  public void setJobsCreated(int jobsCreated) {
    this.jobsCreated = jobsCreated;
  }

  @Override
  public int getBatchJobsPerSeed() {
    return batchJobsPerSeed;
  }

  public void setBatchJobsPerSeed(int batchJobsPerSeed) {
    this.batchJobsPerSeed = batchJobsPerSeed;
  }

  @Override
  public int getInvocationsPerBatchJob() {
    return invocationsPerBatchJob;
  }

  public void setInvocationsPerBatchJob(int invocationsPerBatchJob) {
    this.invocationsPerBatchJob = invocationsPerBatchJob;
  }

  @Override
  public String getSeedJobDefinitionId() {
    return seedJobDefinitionId;
  }

  public void setSeedJobDefinitionId(String seedJobDefinitionId) {
    this.seedJobDefinitionId = seedJobDefinitionId;
  }

  @Override
  public String getMonitorJobDefinitionId() {
    return monitorJobDefinitionId;
  }

  public void setMonitorJobDefinitionId(String monitorJobDefinitionId) {
    this.monitorJobDefinitionId = monitorJobDefinitionId;
  }

  @Override
  public String getBatchJobDefinitionId() {
    return batchJobDefinitionId;
  }

  public void setBatchJobDefinitionId(String batchJobDefinitionId) {
    this.batchJobDefinitionId = batchJobDefinitionId;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String getCreateUserId() {
    return createUserId;
  }

  public void setCreateUserId(String createUserId) {
    this.createUserId = createUserId;
  }

  public String getConfiguration() {
    return configuration.getByteArrayId();
  }

  public void setConfiguration(String configuration) {
    this.configuration.setByteArrayId(configuration);
  }

  public void setSuspensionState(int state) {
    this.suspensionState = state;
  }

  public int getSuspensionState() {
    return suspensionState;
  }

  @Override
  public boolean isSuspended() {
    return suspensionState == SuspensionState.SUSPENDED.getStateCode();
  }

  @Override
  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(final Date startTime) {
    this.startTime = startTime;
  }

  @Override
  public Date getExecutionStartTime() {
    return executionStartTime;
  }

  public void setExecutionStartTime(final Date executionStartTime) {
    this.executionStartTime = executionStartTime;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;

  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  // transient

  public JobDefinitionEntity getSeedJobDefinition() {
    if (seedJobDefinition == null && seedJobDefinitionId != null) {
      seedJobDefinition = Context.getCommandContext().getJobDefinitionManager().findById(seedJobDefinitionId);
    }

    return seedJobDefinition;
  }

  public JobDefinitionEntity getMonitorJobDefinition() {
    if (monitorJobDefinition == null && monitorJobDefinitionId != null) {
      monitorJobDefinition = Context.getCommandContext().getJobDefinitionManager().findById(monitorJobDefinitionId);
    }

    return monitorJobDefinition;
  }

  public JobDefinitionEntity getBatchJobDefinition() {
    if (batchJobDefinition == null && batchJobDefinitionId != null) {
      batchJobDefinition = Context.getCommandContext().getJobDefinitionManager().findById(batchJobDefinitionId);
    }

    return batchJobDefinition;
  }

  public byte[] getConfigurationBytes() {
    return this.configuration.getByteArrayValue();
  }

  public void setConfigurationBytes(byte[] configuration) {
    this.configuration.setByteArrayValue(configuration);
  }

  public BatchJobHandler<?> getBatchJobHandler() {
    if (batchJobHandler == null) {
      batchJobHandler = Context.getCommandContext().getProcessEngineConfiguration().getBatchHandlers().get(type);
    }

    return batchJobHandler;
  }

  @Override
  public Object getPersistentState() {
    HashMap<String, Object> persistentState = new HashMap<>();
    persistentState.put("jobsCreated", jobsCreated);
    persistentState.put("executionStartTime", executionStartTime);
    return persistentState;
  }

  public JobDefinitionEntity createSeedJobDefinition(String deploymentId) {
    seedJobDefinition = new JobDefinitionEntity(BATCH_SEED_JOB_DECLARATION);
    seedJobDefinition.setJobConfiguration(id);
    seedJobDefinition.setTenantId(tenantId);
    seedJobDefinition.setDeploymentId(deploymentId);

    Context.getCommandContext().getJobDefinitionManager().insert(seedJobDefinition);

    seedJobDefinitionId = seedJobDefinition.getId();

    return seedJobDefinition;
  }

  public JobDefinitionEntity createMonitorJobDefinition() {
    monitorJobDefinition = new JobDefinitionEntity(BATCH_MONITOR_JOB_DECLARATION);
    monitorJobDefinition.setJobConfiguration(id);
    monitorJobDefinition.setTenantId(tenantId);

    Context.getCommandContext().getJobDefinitionManager().insert(monitorJobDefinition);

    monitorJobDefinitionId = monitorJobDefinition.getId();

    return monitorJobDefinition;
  }

  public JobDefinitionEntity createBatchJobDefinition() {
    batchJobDefinition = new JobDefinitionEntity(getBatchJobHandler().getJobDeclaration());
    batchJobDefinition.setJobConfiguration(id);
    batchJobDefinition.setTenantId(tenantId);

    Context.getCommandContext().getJobDefinitionManager().insert(batchJobDefinition);

    batchJobDefinitionId = batchJobDefinition.getId();

    return batchJobDefinition;
  }

  public JobEntity createSeedJob() {
    JobEntity seedJob = BATCH_SEED_JOB_DECLARATION.createJobInstance(this);

    Context.getCommandContext().getJobManager().insertAndHintJobExecutor(seedJob);

    return seedJob;
  }

  public void deleteSeedJob() {
    List<JobEntity> seedJobs = Context.getCommandContext()
      .getJobManager()
      .findJobsByJobDefinitionId(seedJobDefinitionId);

    for (JobEntity job : seedJobs) {
      job.delete();
    }
  }

  public JobEntity createMonitorJob(boolean setDueDate) {
    // Maybe use an other job declaration
    JobEntity monitorJob = BATCH_MONITOR_JOB_DECLARATION.createJobInstance(this);
    if (setDueDate) {
      monitorJob.setDuedate(calculateMonitorJobDueDate());
    }

    Context.getCommandContext()
      .getJobManager().insertAndHintJobExecutor(monitorJob);

    return monitorJob;
  }

  protected Date calculateMonitorJobDueDate() {
    int pollTime = Context.getCommandContext()
      .getProcessEngineConfiguration()
      .getBatchPollTime();
    long dueTime = ClockUtil.getCurrentTime().getTime() + (pollTime * 1000);
    return new Date(dueTime);
  }

  public void deleteMonitorJob() {
    List<JobEntity> monitorJobs = Context.getCommandContext()
      .getJobManager()
      .findJobsByJobDefinitionId(monitorJobDefinitionId);

    for (JobEntity monitorJob : monitorJobs) {
      monitorJob.delete();
    }
  }

  public void delete(boolean cascadeToHistory, boolean deleteJobs) {
    CommandContext commandContext = Context.getCommandContext();

    if (Batch.TYPE_SET_VARIABLES.equals(type) ||
        Batch.TYPE_PROCESS_INSTANCE_MIGRATION.equals(type) ||
        Batch.TYPE_CORRELATE_MESSAGE.equals(type)) {
      deleteVariables(commandContext);
    }

    deleteSeedJob();
    deleteMonitorJob();
    if (deleteJobs) {
      getBatchJobHandler().deleteJobs(this);
    }

    JobDefinitionManager jobDefinitionManager = commandContext.getJobDefinitionManager();
    jobDefinitionManager.delete(getSeedJobDefinition());
    jobDefinitionManager.delete(getMonitorJobDefinition());
    jobDefinitionManager.delete(getBatchJobDefinition());

    commandContext.getBatchManager().delete(this);
    configuration.deleteByteArrayValue();

    fireHistoricEndEvent();

    if (cascadeToHistory) {
      HistoricIncidentManager historicIncidentManager = commandContext.getHistoricIncidentManager();
      historicIncidentManager.deleteHistoricIncidentsByJobDefinitionId(seedJobDefinitionId);
      historicIncidentManager.deleteHistoricIncidentsByJobDefinitionId(monitorJobDefinitionId);
      historicIncidentManager.deleteHistoricIncidentsByJobDefinitionId(batchJobDefinitionId);

      HistoricJobLogManager historicJobLogManager = commandContext.getHistoricJobLogManager();
      historicJobLogManager.deleteHistoricJobLogsByJobDefinitionId(seedJobDefinitionId);
      historicJobLogManager.deleteHistoricJobLogsByJobDefinitionId(monitorJobDefinitionId);
      historicJobLogManager.deleteHistoricJobLogsByJobDefinitionId(batchJobDefinitionId);

      commandContext.getHistoricBatchManager().deleteHistoricBatchById(id);
    }
  }

  protected void deleteVariables(CommandContext commandContext) {
    VariableInstanceManager variableInstanceManager = commandContext.getVariableInstanceManager();

    List<VariableInstanceEntity> variableInstances =
        variableInstanceManager.findVariableInstancesByBatchId(id);

    variableInstances.forEach(VariableInstanceEntity::delete);
  }

  public void fireHistoricStartEvent() {
    Context.getCommandContext()
      .getHistoricBatchManager()
      .createHistoricBatch(this);
  }

  public void fireHistoricEndEvent() {
    Context.getCommandContext()
      .getHistoricBatchManager()
      .completeHistoricBatch(this);
  }

  public void fireHistoricUpdateEvent() {
    Context.getCommandContext()
      .getHistoricBatchManager()
      .updateHistoricBatch(this);
  }

  public boolean isCompleted() {
    return Context.getCommandContext().getProcessEngineConfiguration()
      .getManagementService()
      .createJobQuery()
      .jobDefinitionId(batchJobDefinitionId)
      .count() == 0;
  }

  @Override
  public String toString() {
    return "BatchEntity{" +
      "batchHandler=" + batchJobHandler +
      ", id='" + id + '\'' +
      ", type='" + type + '\'' +
      ", size=" + totalJobs +
      ", jobCreated=" + jobsCreated +
      ", batchJobsPerSeed=" + batchJobsPerSeed +
      ", invocationsPerBatchJob=" + invocationsPerBatchJob +
      ", seedJobDefinitionId='" + seedJobDefinitionId + '\'' +
      ", monitorJobDefinitionId='" + seedJobDefinitionId + '\'' +
      ", batchJobDefinitionId='" + batchJobDefinitionId + '\'' +
      ", configurationId='" + configuration.getByteArrayId() + '\'' +
      '}';
  }

  @Override
  public Set<String> getReferencedEntityIds() {
    return new HashSet<>();
  }

  @Override
  public Map<String, Class> getReferencedEntitiesIdAndClass() {
    Map<String, Class> referenceIdAndClass = new HashMap<>();

    if (seedJobDefinitionId != null) {
      referenceIdAndClass.put(seedJobDefinitionId, JobDefinitionEntity.class);
    }
    if (batchJobDefinitionId != null) {
      referenceIdAndClass.put(batchJobDefinitionId, JobDefinitionEntity.class);
    }
    if (monitorJobDefinitionId != null) {
      referenceIdAndClass.put(monitorJobDefinitionId, JobDefinitionEntity.class);
    }

    return referenceIdAndClass;
  }
}
