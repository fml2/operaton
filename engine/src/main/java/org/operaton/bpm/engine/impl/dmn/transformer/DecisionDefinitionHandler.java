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
package org.operaton.bpm.engine.impl.dmn.transformer;

import org.operaton.bpm.dmn.engine.impl.DmnDecisionImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.transform.DmnDecisionTransformHandler;
import org.operaton.bpm.engine.impl.HistoryTimeToLiveParser;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.operaton.bpm.model.dmn.instance.Decision;

public class DecisionDefinitionHandler extends DmnDecisionTransformHandler {

  protected boolean skipEnforceTtl = false;

  @Override
  protected DmnDecisionImpl createDmnElement() {
    return new DecisionDefinitionEntity();
  }

  @Override
  protected DmnDecisionImpl createFromDecision(DmnElementTransformContext context, Decision decision) {
    DecisionDefinitionEntity decisionDefinition = (DecisionDefinitionEntity) super.createFromDecision(context, decision);
    String category = context.getModelInstance().getDefinitions().getNamespace();

    decisionDefinition.setCategory(category);
    decisionDefinition.setVersionTag(decision.getVersionTag());

    validateAndSetHTTL(decision, decisionDefinition, isSkipEnforceTtl());

    return decisionDefinition;
  }

  protected void validateAndSetHTTL(Decision decision, DecisionDefinitionEntity decisionDefinition, boolean skipEnforceTtl) {
    Integer historyTimeToLive = HistoryTimeToLiveParser.create().parse(decision, decisionDefinition.getKey(), skipEnforceTtl);
    decisionDefinition.setHistoryTimeToLive(historyTimeToLive);
  }

  public boolean isSkipEnforceTtl() {
    return skipEnforceTtl;
  }

  public void setSkipEnforceTtl(boolean skipEnforceTtl) {
    this.skipEnforceTtl = skipEnforceTtl;
  }
}
