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
package org.operaton.bpm.engine.impl.cmmn.transformer;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.handler.*;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration;
import org.operaton.bpm.engine.impl.core.transformer.Transform;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.CmmnModelException;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Roman Smirnov
 *
 */
public class CmmnTransform implements Transform<CaseDefinitionEntity> {

  protected static final CmmnTransformerLogger LOG = ProcessEngineLogger.CMMN_TRANSFORMER_LOGGER;

  protected CmmnTransformer transformer;

  protected ExpressionManager expressionManager;
  protected DefaultCmmnElementHandlerRegistry handlerRegistry;
  protected List<CmmnTransformListener> transformListeners;

  protected ResourceEntity resource;
  protected DeploymentEntity deployment;

  protected CmmnModelInstance model;
  protected CmmnHandlerContext context = new CmmnHandlerContext();
  protected List<CaseDefinitionEntity> caseDefinitions = new ArrayList<>();

  public CmmnTransform(CmmnTransformer transformer) {
    this.transformer = transformer;
    this.expressionManager = transformer.getExpressionManager();
    this.handlerRegistry = transformer.getCmmnElementHandlerRegistry();
    this.transformListeners = transformer.getTransformListeners();
  }

  public CmmnTransform deployment(DeploymentEntity deployment) {
    this.deployment = deployment;
    return this;
  }

  public CmmnTransform resource(ResourceEntity resource) {
    this.resource = resource;
    return this;
  }

  @Override
  public List<CaseDefinitionEntity> transform() {
    // get name of resource
    String resourceName = resource.getName();

    // create an input stream
    byte[] bytes = resource.getBytes();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

    try {
      // read input stream
      model = Cmmn.readModelFromStream(inputStream);

    } catch (CmmnModelException e) {
      throw LOG.transformResourceException(resourceName, e);
    }

    // TODO: use model API to validate (ie.
    // semantic and execution validation) model

    context.setModel(model);
    context.setDeployment(deployment);
    context.setExpressionManager(expressionManager);

    try {

       transformRootElement();

    } catch (Exception e) {
      // ALL unexpected exceptions should bubble up since they are not handled
      // accordingly by underlying parse-methods and the process can't be deployed
      throw LOG.parseProcessException(resourceName, e);
    }

    return caseDefinitions;
  }

  protected void transformRootElement() {

    transformImports();
    transformCaseDefinitions();

    Definitions definitions = model.getDefinitions();
    for (CmmnTransformListener transformListener : transformListeners) {
      transformListener.transformRootElement(definitions, caseDefinitions);
    }

  }

  protected void transformImports() {
    // not implemented yet
  }

  protected void transformCaseDefinitions() {
    Definitions definitions = model.getDefinitions();

    Collection<Case> cases = definitions.getCases();

    for (Case currentCase : cases) {
      context.setCaseDefinition(null);
      context.setParent(null);
      CmmnCaseDefinition caseDefinition = transformCase(currentCase);
      caseDefinitions.add((CaseDefinitionEntity) caseDefinition);
    }
  }

  protected CaseDefinitionEntity transformCase(Case element) {
    // get CaseTransformer
    CmmnElementHandler<Case, CmmnActivity> caseTransformer = getDefinitionHandler(Case.class);
    CmmnActivity definition = caseTransformer.handleElement(element, context);

    context.setCaseDefinition((CmmnCaseDefinition) definition);
    context.setParent(definition);

    CasePlanModel casePlanModel = element.getCasePlanModel();
    transformCasePlanModel(casePlanModel);

    for (CmmnTransformListener transformListener : transformListeners) {
      transformListener.transformCase(element, (CmmnCaseDefinition) definition);
    }

    return (CaseDefinitionEntity) definition;
  }

  protected void transformCasePlanModel(CasePlanModel casePlanModel) {
    CasePlanModelHandler planItemHandler = (CasePlanModelHandler) getPlanItemHandler(CasePlanModel.class);
    CmmnActivity newActivity = planItemHandler.handleElement(casePlanModel, context);
    context.setParent(newActivity);

    transformStage(casePlanModel, newActivity);

    context.setParent(newActivity);
    planItemHandler.initializeExitCriterias(casePlanModel, newActivity, context);

    for (CmmnTransformListener transformListener : transformListeners) {
      transformListener.transformCasePlanModel((org.operaton.bpm.model.cmmn.impl.instance.CasePlanModel) casePlanModel, newActivity);
    }
  }

  protected void transformStage(Stage stage, CmmnActivity parent) {

    context.setParent(parent);

    // transform a sentry with it ifPart (onParts will
    // not be transformed in this step)
    transformSentries(stage);

    // transform planItems
    transformPlanItems(stage, parent);

    // transform the onParts of the existing sentries
    transformSentryOnParts(stage);

    // parse planningTable (not yet implemented)
    transformPlanningTable(stage.getPlanningTable(), parent);

  }

  protected void transformPlanningTable(PlanningTable planningTable, CmmnActivity parent) {
    // not yet implemented.

    // TODO: think about how to organize the planning tables! A tableItem or planningTable
    // can have "applicabilityRules": If the rule evaluates to "true" the the tableItem or
    // planningTable is applicable for planning otherwise it is not.
  }

  protected void transformSentries(Stage stage) {
    Collection<Sentry> sentries = stage.getSentrys();

    if (sentries != null && !sentries.isEmpty()) {
      SentryHandler handler = getSentryHandler();
      for (Sentry sentry : sentries) {
        handler.handleElement(sentry, context);
      }
    }
  }

  protected void transformSentryOnParts(Stage stage) {
    Collection<Sentry> sentries = stage.getSentrys();

    if (sentries != null && !sentries.isEmpty()) {
      SentryHandler handler = getSentryHandler();
      for (Sentry sentry : sentries) {
        handler.initializeOnParts(sentry, context);
        // sentry fully transformed -> call transform listener
        CmmnSentryDeclaration sentryDeclaration = context.getParent().getSentry(sentry.getId());
        for (CmmnTransformListener transformListener : transformListeners) {
          transformListener.transformSentry(sentry, sentryDeclaration);
        }
      }
    }
  }

  protected void transformPlanItems(PlanFragment planFragment, CmmnActivity parent) {
    Collection<PlanItem> planItems = planFragment.getPlanItems();

    for (PlanItem planItem : planItems) {
      transformPlanItem(planItem, parent);
    }

  }

  protected void transformPlanItem(PlanItem planItem, CmmnActivity parent) {
    PlanItemDefinition definition = planItem.getDefinition();

    ItemHandler planItemTransformer = null;

    if (definition instanceof HumanTask) {
      planItemTransformer = getPlanItemHandler(HumanTask.class);
    } else if (definition instanceof ProcessTask) {
      planItemTransformer = getPlanItemHandler(ProcessTask.class);
    } else if (definition instanceof CaseTask) {
      planItemTransformer = getPlanItemHandler(CaseTask.class);
    } else if (definition instanceof DecisionTask) {
      planItemTransformer = getPlanItemHandler(DecisionTask.class);
    } else if (definition instanceof Task) {
      planItemTransformer = getPlanItemHandler(Task.class);
    } else if (definition instanceof Stage) {
      planItemTransformer = getPlanItemHandler(Stage.class);
    } else if (definition instanceof Milestone) {
      planItemTransformer = getPlanItemHandler(Milestone.class);
    } else if (definition instanceof EventListener) {
      planItemTransformer = getPlanItemHandler(EventListener.class);
    }

    if (planItemTransformer != null) {
      CmmnActivity newActivity = planItemTransformer.handleElement(planItem, context);

      if (definition instanceof Stage stage) {
        transformStage(stage, newActivity);
        context.setParent(parent);

      } else if (definition instanceof HumanTask humanTask) {

        // According to the specification: A HumanTask can only contain
        // one planningTable, the XSD allows multiple planningTables!
        Collection<PlanningTable> planningTables = humanTask.getPlanningTables();
        for (PlanningTable planningTable : planningTables) {
          transformPlanningTable(planningTable, parent);
        }

      }

      for (CmmnTransformListener transformListener : transformListeners) {
        if (definition instanceof HumanTask task) {
          transformListener.transformHumanTask(planItem, task, newActivity);
        } else if (definition instanceof ProcessTask task) {
          transformListener.transformProcessTask(planItem, task, newActivity);
        } else if (definition instanceof CaseTask task) {
          transformListener.transformCaseTask(planItem, task, newActivity);
        } else if (definition instanceof DecisionTask task) {
          transformListener.transformDecisionTask(planItem, task, newActivity);
        } else if (definition instanceof Task task) {
          transformListener.transformTask(planItem, task, newActivity);
        } else if (definition instanceof Stage stage) {
          transformListener.transformStage(planItem, stage, newActivity);
        } else if (definition instanceof Milestone milestone) {
          transformListener.transformMilestone(planItem, milestone, newActivity);
        } else if (definition instanceof EventListener listener) {
          transformListener.transformEventListener(planItem, listener, newActivity);
        }
      }
    }
  }

  // getter/setter ////////////////////////////////////////////////////////////////////

  public DeploymentEntity getDeployment() {
    return deployment;
  }

  public void setDeployment(DeploymentEntity deployment) {
    this.deployment = deployment;
  }

  public ResourceEntity getResource() {
    return resource;
  }

  public void setResource(ResourceEntity resource) {
    this.resource = resource;
  }

  public DefaultCmmnElementHandlerRegistry getHandlerRegistry() {
    return handlerRegistry;
  }

  public void setHandlerRegistry(DefaultCmmnElementHandlerRegistry handlerRegistry) {
    this.handlerRegistry = handlerRegistry;
  }

  @SuppressWarnings("unchecked")
  protected <V extends CmmnElement> CmmnElementHandler<V, CmmnActivity> getDefinitionHandler(Class<V> cls) {
    return (CmmnElementHandler<V, CmmnActivity>) getHandlerRegistry().getDefinitionElementHandlers().get(cls);
  }

  protected ItemHandler getPlanItemHandler(Class<? extends PlanItemDefinition> cls) {
    return getHandlerRegistry().getPlanItemElementHandlers().get(cls);
  }

  protected ItemHandler getDiscretionaryItemHandler(Class<? extends PlanItemDefinition> cls) {
    return getHandlerRegistry().getDiscretionaryElementHandlers().get(cls);
  }

  protected SentryHandler getSentryHandler() {
    return getHandlerRegistry().getSentryHandler();
  }

  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }

  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

}
