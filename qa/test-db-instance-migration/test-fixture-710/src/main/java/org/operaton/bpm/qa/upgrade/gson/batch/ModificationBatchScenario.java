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
package org.operaton.bpm.qa.upgrade.gson.batch;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tassilo Weidner
 */
public class ModificationBatchScenario {

  private ModificationBatchScenario() {
  }

  @Deployment
  public static String deploy() {
    return "org/operaton/bpm/qa/upgrade/gson/oneTaskProcessModification.bpmn20.xml";
  }

  @DescribesScenario("ModificationBatchScenario")
  public static ScenarioSetup initModificationBatch() {
    return new ScenarioSetup() {
      public void execute(ProcessEngine engine, String scenarioName) {

        String processDefinitionId = engine.getRepositoryService().createProcessDefinitionQuery()
          .processDefinitionKey("oneTaskProcessModification_710")
          .singleResult()
          .getId();

        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
          String processInstanceId = engine.getRuntimeService()
            .startProcessInstanceById(processDefinitionId, "ModificationBatchScenario").getId();

          processInstanceIds.add(processInstanceId);
        }

        engine.getRuntimeService().createModification(processDefinitionId)
          .startAfterActivity("theStart")
          .startBeforeActivity("theTask")
          .startBeforeActivity("userTask4")
          .startTransition("flow2")
          .cancelAllForActivity("userTask4", false)
          .processInstanceIds(processInstanceIds)
          .skipCustomListeners()
          .skipIoMappings()
          .executeAsync();
      }
    };
  }
}
