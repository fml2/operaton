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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.batch.BatchConfiguration;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.batch.BatchJobHandler;
import org.operaton.bpm.engine.impl.batch.DeploymentMapping;
import org.operaton.bpm.engine.impl.batch.DeploymentMappings;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

class JobExecutorBatchTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;

  public CountingJobExecutor jobExecutor;
  protected JobExecutor defaultJobExecutor;
  protected int defaultBatchJobsPerSeed;

  @BeforeEach
  void replaceJobExecutor() {
    defaultJobExecutor = processEngineConfiguration.getJobExecutor();
    jobExecutor = new CountingJobExecutor();
    processEngineConfiguration.setJobExecutor(jobExecutor);
  }

  @BeforeEach
  void saveBatchJobsPerSeed() {
    defaultBatchJobsPerSeed = engineRule.getProcessEngineConfiguration().getBatchJobsPerSeed();
  }

  @AfterEach
  void resetJobExecutor() {
    engineRule.getProcessEngineConfiguration()
      .setJobExecutor(defaultJobExecutor);
  }

  @AfterEach
  void resetBatchJobsPerSeed() {
    engineRule.getProcessEngineConfiguration()
      .setBatchJobsPerSeed(defaultBatchJobsPerSeed);
  }

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @Test
  void testJobExecutorHintedOnBatchCreation() {
    // given
    jobExecutor.startRecord();

    // when a batch is created
    helper.migrateProcessInstancesAsync(2);

    // then the job executor is hinted for the seed job
    assertThat(jobExecutor.getJobsAdded()).isEqualTo(1);
  }

  @Test
  void testJobExecutorHintedSeedJobExecution() {
    // reduce number of batch jobs per seed to not have to create a lot of instances
    engineRule.getProcessEngineConfiguration().setBatchJobsPerSeed(10);

    // given
    Batch batch = helper.migrateProcessInstancesAsync(13);
    jobExecutor.startRecord();

    // when the seed job is executed
    helper.executeSeedJob(batch);

    // then the job executor is hinted for the monitor job and 10 execution jobs
    assertThat(jobExecutor.getJobsAdded()).isEqualTo(11);
  }

  @Test
  void testJobExecutorHintedSeedJobCompletion() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(3);
    jobExecutor.startRecord();

    // when the seed job is executed
    helper.executeSeedJob(batch);

    // then the job executor is hinted for the monitor job and 3 execution jobs
    assertThat(jobExecutor.getJobsAdded()).isEqualTo(4);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void testMixedJobExecutorVersionSeedJobExecution() {
    // given
    Batch batch = helper.migrateProcessInstancesAsync(4);
    // ... and there are more mappings than ids (simulating an intermediate execution of a SeedJob
    // by an older version that only processes ids but does not update the mappings)
    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(context -> {
      BatchEntity batchEntity = context.getBatchManager().findBatchById(batch.getId());
      BatchJobHandler batchJobHandler = context.getProcessEngineConfiguration().getBatchHandlers()
          .get(batchEntity.getType());
      BatchConfiguration config = (BatchConfiguration) batchJobHandler
          .readConfiguration(batchEntity.getConfigurationBytes());
      DeploymentMappings idMappings = config.getIdMappings();
      DeploymentMapping firstMapping = idMappings.get(0);
      idMappings.set(0, new DeploymentMapping(firstMapping.getDeploymentId(), firstMapping.getCount() + 2));
      idMappings.add(0, new DeploymentMapping("foo", 2));
      batchEntity.setConfigurationBytes(batchJobHandler.writeConfiguration(config));
      return null;
    });
    jobExecutor.startRecord();

    // when the seed job is executed
    helper.executeSeedJob(batch);

    // then the job executor is hinted for the monitor job and 4 execution jobs
    assertThat(jobExecutor.getJobsAdded()).isEqualTo(5);
  }

  public class CountingJobExecutor extends JobExecutor {

    public boolean recordStarted = false;
    public long jobsAdded = 0;

    @Override
    public boolean isActive() {
      return true;
    }

    protected void startExecutingJobs() {
      // do nothing
    }

    protected void stopExecutingJobs() {
      // do nothing
    }

    @Override
    public void executeJobs(List<String> jobIds, ProcessEngineImpl processEngine) {
      // do nothing
    }

    public void startRecord() {
      resetJobsAdded();
      recordStarted = true;
    }

    @Override
    public void jobWasAdded() {
      if (recordStarted) {
        jobsAdded++;
      }
    }

    public long getJobsAdded() {
      return jobsAdded;
    }

    public void resetJobsAdded() {
      jobsAdded = 0;
    }

  }

}
