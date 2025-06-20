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
package org.operaton.bpm.model.bpmn.builder;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.ProcessType;
import org.operaton.bpm.model.bpmn.instance.Process;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractProcessBuilder<B extends AbstractProcessBuilder<B>> extends AbstractCallableElementBuilder<B, Process> {

  protected AbstractProcessBuilder(BpmnModelInstance modelInstance, Process element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the process type for this.
   *
   * @param processType  the process type to set
   * @return the builder object
   */
  public B processType(ProcessType processType) {
    element.setProcessType(processType);
    return myself;
  }

  /**
   * Sets this closed.
   *
   * @return the builder object
   */
  public B closed() {
    element.setClosed(true);
    return myself;
  }

  /**
   * Sets this executable.
   *
   * @return the builder object
   */
  public B executable() {
    element.setExecutable(true);
    return myself;
  }

  public B operatonJobPriority(String jobPriority) {
    element.setOperatonJobPriority(jobPriority);
    return myself;
  }

  /**
   * Set the operaton task priority attribute.
   * The priority is only used for service tasks which have as type value
   * <code>external</code>
   *
   * @param taskPriority the task priority which should used for the external tasks
   * @return the builder object
   */
  public B operatonTaskPriority(String taskPriority) {
    element.setOperatonTaskPriority(taskPriority);
    return myself;
  }

  /**
   * Sets the operaton history time to live.
   *
   * @param historyTimeToLive value for history time to live, must be either null or non-negative integer.
   * @return the builder object
   */
  public B operatonHistoryTimeToLive(Integer historyTimeToLive) {
    element.setOperatonHistoryTimeToLive(historyTimeToLive);
    return myself;
  }

  /**
   * Sets the operaton history time to live string.
   *
   * @param historyTimeToLive string value of history time to live, can be null or a valid ISO-8601 value.
   * @return the builder object
   */
  public B operatonHistoryTimeToLiveString(String historyTimeToLive) {
    element.setOperatonHistoryTimeToLiveString(historyTimeToLive);
    return myself;
  }

  /**
   * Set whenever the process is startable in Tasklist
   *
   * @param isStartableInTasklist default value is true
   * @return the builder object
   */
  public B operatonStartableInTasklist(Boolean isStartableInTasklist) {
    element.setOperatonIsStartableInTasklist(isStartableInTasklist);
    return myself;
  }

  /**
   * Set to specify a version tag for the process definition.
   *
   * @param versionTag the version of the process definition
   * @return the builder object
   */
  public B operatonVersionTag(String versionTag) {
    element.setOperatonVersionTag(versionTag);
    return myself;
  }
}
