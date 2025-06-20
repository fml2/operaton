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

import org.operaton.bpm.engine.history.HistoricActivityStatistics;

/**
 *
 * @author Roman Smirnov
 *
 */
public class HistoricActivityStatisticsDto {

  protected String id;
  protected long instances;
  protected long canceled;
  protected long finished;
  protected long completeScope;
  protected long openIncidents;
  protected long resolvedIncidents;
  protected long deletedIncidents;

  public String getId() {
    return id;
  }

  public long getInstances() {
    return instances;
  }

  public long getCanceled() {
    return canceled;
  }

  public long getFinished() {
    return finished;
  }

  public long getCompleteScope() {
    return completeScope;
  }

  public long getOpenIncidents() {
    return openIncidents;
  }

  public long getResolvedIncidents() {
    return resolvedIncidents;
  }

  public long getDeletedIncidents() {
    return deletedIncidents;
  }

  public static HistoricActivityStatisticsDto fromHistoricActivityStatistics(HistoricActivityStatistics statistics) {
    HistoricActivityStatisticsDto result = new HistoricActivityStatisticsDto();

    result.id = statistics.getId();

    result.instances = statistics.getInstances();
    result.canceled = statistics.getCanceled();
    result.finished = statistics.getFinished();
    result.completeScope = statistics.getCompleteScope();
    result.openIncidents = statistics.getOpenIncidents();
    result.resolvedIncidents = statistics.getResolvedIncidents();
    result.deletedIncidents = statistics.getDeletedIncidents();

    return result;
  }

}
