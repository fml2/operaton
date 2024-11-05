/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.identity.impl.ldap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.ldap.util.LdapTestEnvironmentExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Deployment
@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(LdapTestEnvironmentExtension.class)
public class ProcessDefinitionQueryWithCustomIdentityProviderTest {

  RepositoryService repositoryService;

  @Test
  void should_find_all_processes_for_candidate_user() {
    // given

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("oscar").list();

    // then
    assertThat(processDefinitions).hasSize(2);
    assertThat(processDefinitions.get(0).getKey()).isEqualTo("process1");
    assertThat(processDefinitions.get(1).getKey()).isEqualTo("process2");
  }

  @Test
  void should_find_no_processes_for_candidate_user() {
    // given

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("pepe").list();
    // then
    assertThat(processDefinitions).isEmpty();
  }

  @Test
  void should_find_all_processes_for_user_in_candidate_group() {
    // given

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("david(IT)").list();

    // then
    assertThat(processDefinitions).hasSize(1);
    assertThat(processDefinitions.get(0).getKey()).isEqualTo("process3");
  }

  @Test
  void should_find_all_processes_for_candidate_user_in_candidate_group() {
    // given

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("monster").list();

    // then
    assertThat(processDefinitions).hasSize(2);
    assertThat(processDefinitions.get(0).getKey()).isEqualTo("process1");
    assertThat(processDefinitions.get(1).getKey()).isEqualTo("process3");
  }

  @Test
  void should_find_no_duplicate_processes_for_candidate_user_in_candidate_group() {
    // given

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("fozzie").list();

    // then
    assertThat(processDefinitions).hasSize(1);
    assertThat(processDefinitions.get(0).getKey()).isEqualTo("process3");
  }
}
