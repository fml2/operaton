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
package org.operaton.bpm.engine.impl.dmn.deployer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.operaton.bpm.dmn.engine.impl.DmnDecisionImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.operaton.bpm.engine.impl.AbstractDefinitionDeployer;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.core.model.Properties;
import org.operaton.bpm.engine.impl.dmn.DecisionLogger;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionEntity;
import org.operaton.bpm.engine.impl.dmn.entity.repository.DecisionRequirementsDefinitionManager;
import org.operaton.bpm.engine.impl.dmn.transformer.DecisionDefinitionHandler;
import org.operaton.bpm.engine.impl.persistence.deploy.Deployer;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.operaton.bpm.model.dmn.instance.Decision;

/**
 * {@link Deployer} responsible to parse DMN 1.1 XML files and create the proper
 * {@link DecisionRequirementsDefinitionEntity}s.
 */
public class DecisionRequirementsDefinitionDeployer extends AbstractDefinitionDeployer<DecisionRequirementsDefinitionEntity> {

  protected static final DecisionLogger LOG = ProcessEngineLogger.DECISION_LOGGER;

  protected DmnTransformer transformer;

  @Override
  protected String[] getResourcesSuffixes() {
    // since the DecisionDefinitionDeployer uses the result of this cacheDeployer, make sure that
    // it process the same DMN resources
    return DecisionDefinitionDeployer.DMN_RESOURCE_SUFFIXES;
  }

  @Override
  protected List<DecisionRequirementsDefinitionEntity> transformDefinitions(DeploymentEntity deployment, ResourceEntity resource, Properties properties) {
    byte[] bytes = resource.getBytes();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

    if (!deployment.isNew()) {
      skipEnforceTtl(true);
    }

    try {
      DecisionRequirementsDefinitionEntity drd = transformer
          .createTransform()
          .modelInstance(inputStream)
          .transformDecisionRequirementsGraph();

      return Collections.singletonList(drd);

    } catch (Exception e) {
      throw LOG.exceptionParseDmnResource(resource.getName(), e);

    } finally {
      skipEnforceTtl(false);

    }
  }

  public void skipEnforceTtl(boolean skipEnforceTtl) {
    DmnElementTransformHandler<Decision, DmnDecisionImpl> handler = transformer.getElementTransformHandlerRegistry().getHandler(Decision.class);
    ((DecisionDefinitionHandler) handler).setSkipEnforceTtl(skipEnforceTtl);
  }

  @Override
  protected DecisionRequirementsDefinitionEntity findDefinitionByDeploymentAndKey(String deploymentId, String definitionKey) {
    return getDecisionRequirementsDefinitionManager().findDecisionRequirementsDefinitionByDeploymentAndKey(deploymentId, definitionKey);
  }

  @Override
  protected DecisionRequirementsDefinitionEntity findLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
    return getDecisionRequirementsDefinitionManager().findLatestDecisionRequirementsDefinitionByKeyAndTenantId(definitionKey, tenantId);
  }

  @Override
  protected void persistDefinition(DecisionRequirementsDefinitionEntity definition) {
    if (isDecisionRequirementsDefinitionPersistable(definition)) {
      getDecisionRequirementsDefinitionManager().insertDecisionRequirementsDefinition(definition);
    }
  }

  @Override
  protected void addDefinitionToDeploymentCache(DeploymentCache deploymentCache, DecisionRequirementsDefinitionEntity definition) {
    if (isDecisionRequirementsDefinitionPersistable(definition)) {
      deploymentCache.addDecisionRequirementsDefinition(definition);
    }
  }

  @Override
  protected void ensureNoDuplicateDefinitionKeys(List<DecisionRequirementsDefinitionEntity> definitions) {
    // ignore decision requirements definitions which will not be persistent
    ArrayList<DecisionRequirementsDefinitionEntity> persistableDefinitions = new ArrayList<>();
    for (DecisionRequirementsDefinitionEntity definition : definitions) {
      if (isDecisionRequirementsDefinitionPersistable(definition)) {
        persistableDefinitions.add(definition);
      }
    }

    super.ensureNoDuplicateDefinitionKeys(persistableDefinitions);
  }

  public static boolean isDecisionRequirementsDefinitionPersistable(DecisionRequirementsDefinitionEntity definition) {
    // persist no decision requirements definition for a single decision
    return definition.getDecisions().size() > 1;
  }

  @Override
  protected void updateDefinitionByPersistedDefinition(DeploymentEntity deployment, DecisionRequirementsDefinitionEntity definition,
      DecisionRequirementsDefinitionEntity persistedDefinition) {
    // cannot update the definition if it is not persistent
    if (persistedDefinition != null) {
      super.updateDefinitionByPersistedDefinition(deployment, definition, persistedDefinition);
    }
  }

  //context ///////////////////////////////////////////////////////////////////////////////////////////

  protected DecisionRequirementsDefinitionManager getDecisionRequirementsDefinitionManager() {
    return getCommandContext().getDecisionRequirementsDefinitionManager();
  }

  // getters/setters ///////////////////////////////////////////////////////////////////////////////////

  public DmnTransformer getTransformer() {
    return transformer;
  }

  public void setTransformer(DmnTransformer transformer) {
    this.transformer = transformer;
  }

}
