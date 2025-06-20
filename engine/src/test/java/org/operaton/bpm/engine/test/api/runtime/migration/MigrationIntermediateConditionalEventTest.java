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
package org.operaton.bpm.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.impl.migration.validation.instruction.ConditionalEventUpdateEventTriggerValidator.MIGRATION_CONDITIONAL_VALIDATION_ERROR_MSG;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.CONDITION_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.USER_TASK_ID;
import static org.operaton.bpm.engine.test.api.runtime.migration.models.ConditionalModels.VAR_CONDITION;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;


/**
 * @author Thorben Lindhauer
 * @author Christopher Zell
 */
public class MigrationIntermediateConditionalEventTest {


  public static final BpmnModelInstance ONE_CONDITION_PROCESS = ProcessModels.newModel()
    .startEvent()
    .intermediateCatchEvent(CONDITION_ID)
      .conditionalEventDefinition("test")
        .condition(VAR_CONDITION)
      .conditionalEventDefinitionDone()
    .userTask(USER_TASK_ID)
    .endEvent()
    .done();

  protected static final String VAR_NAME = "variable";
  protected static final String NEW_CONDITION_ID = "newCondition";
  protected static final String NEW_VAR_CONDITION = "${variable == 2}";

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testMigrateEventSubscription() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_CONDITION_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ONE_CONDITION_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(CONDITION_ID, CONDITION_ID).updateEventTrigger()
      .build();

    //when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertEventSubscriptionMigrated(CONDITION_ID, CONDITION_ID, null);

    //then it is possible to trigger the conditional event
    testHelper.setVariable(processInstance.getId(), VAR_NAME, "1");

    testHelper.completeTask(USER_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateConditionalEventWithoutUpdateTrigger() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_CONDITION_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ONE_CONDITION_PROCESS);

    var migrationInstructionBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(CONDITION_ID, CONDITION_ID);

    //when conditional event is migrated without update event trigger
    assertThatThrownBy(migrationInstructionBuilder::build)
      // then
      .isInstanceOf(MigrationPlanValidationException.class)
      .hasMessageContaining(MIGRATION_CONDITIONAL_VALIDATION_ERROR_MSG);
  }

  @Test
  void testMigrateEventSubscriptionChangeCondition() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_CONDITION_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(Bpmn.createExecutableProcess()
      .startEvent()
      .intermediateCatchEvent(NEW_CONDITION_ID)
        .conditionalEventDefinition()
          .condition(NEW_VAR_CONDITION)
        .conditionalEventDefinitionDone()
      .userTask(USER_TASK_ID)
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(CONDITION_ID, NEW_CONDITION_ID).updateEventTrigger()
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertEventSubscriptionMigrated(CONDITION_ID, NEW_CONDITION_ID, null);

    //and var is set with value of old condition
    testHelper.setVariable(processInstance.getId(), VAR_NAME, "1");

    //then nothing happens
    assertThat(rule.getTaskService().createTaskQuery().singleResult()).isNull();

    //when correct value is set
    testHelper.setVariable(processInstance.getId(), VAR_NAME, "2");

    //then condition is satisfied
    testHelper.completeTask(USER_TASK_ID);
    testHelper.assertProcessEnded(processInstance.getId());
  }
}
