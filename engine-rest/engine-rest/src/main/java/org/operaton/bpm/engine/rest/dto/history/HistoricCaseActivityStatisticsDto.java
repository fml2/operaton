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
package org.operaton.bpm.engine.rest.dto.history;

import org.operaton.bpm.engine.history.HistoricCaseActivityStatistics;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricCaseActivityStatisticsDto {

  protected String id;
  protected long available;
  protected long enabled;
  protected long disabled;
  protected long active;
  protected long completed;
  protected long terminated;

  public String getId() {
    return id;
  }

  public long getAvailable() {
    return available;
  }

  public long getEnabled() {
    return enabled;
  }

  public long getDisabled() {
    return disabled;
  }

  public long getActive() {
    return active;
  }

  public long getCompleted() {
    return completed;
  }

  public long getTerminated() {
    return terminated;
  }

  public static HistoricCaseActivityStatisticsDto fromHistoricCaseActivityStatistics(HistoricCaseActivityStatistics statistics) {
    HistoricCaseActivityStatisticsDto result = new HistoricCaseActivityStatisticsDto();

    result.id = statistics.getId();
    result.available = statistics.getAvailable();
    result.enabled = statistics.getEnabled();
    result.disabled = statistics.getDisabled();
    result.active = statistics.getActive();
    result.completed = statistics.getCompleted();
    result.terminated = statistics.getTerminated();

    return result;
  }

}
