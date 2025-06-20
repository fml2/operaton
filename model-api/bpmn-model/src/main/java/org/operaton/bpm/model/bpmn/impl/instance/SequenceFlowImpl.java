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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.SequenceFlowBuilder;
import org.operaton.bpm.model.bpmn.instance.ConditionExpression;
import org.operaton.bpm.model.bpmn.instance.FlowElement;
import org.operaton.bpm.model.bpmn.instance.FlowNode;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN sequenceFlow element
 *
 * @author Sebastian Menski
 */
public class SequenceFlowImpl extends FlowElementImpl implements SequenceFlow {

  protected static AttributeReference<FlowNode> sourceRefAttribute;
  protected static AttributeReference<FlowNode> targetRefAttribute;
  protected static Attribute<Boolean> isImmediateAttribute;
  protected static ChildElement<ConditionExpression> conditionExpressionCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(SequenceFlow.class, BPMN_ELEMENT_SEQUENCE_FLOW)
      .namespaceUri(BPMN20_NS)
      .extendsType(FlowElement.class)
      .instanceProvider(SequenceFlowImpl::new);

    sourceRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_SOURCE_REF)
      .required()
      .idAttributeReference(FlowNode.class)
      .build();

    targetRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TARGET_REF)
      .required()
      .idAttributeReference(FlowNode.class)
      .build();

    isImmediateAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_IMMEDIATE)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    conditionExpressionCollection = sequenceBuilder.element(ConditionExpression.class)
      .build();

    typeBuilder.build();
  }

  public SequenceFlowImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public SequenceFlowBuilder builder() {
    return new SequenceFlowBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public FlowNode getSource() {
    return sourceRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSource(FlowNode source) {
    sourceRefAttribute.setReferenceTargetElement(this, source);
  }

  @Override
  public FlowNode getTarget() {
    return targetRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setTarget(FlowNode target) {
    targetRefAttribute.setReferenceTargetElement(this, target);
  }

  @Override
  public boolean isImmediate() {
    return isImmediateAttribute.getValue(this);
  }

  @Override
  public void setImmediate(boolean isImmediate) {
    isImmediateAttribute.setValue(this, isImmediate);
  }

  @Override
  public ConditionExpression getConditionExpression() {
    return conditionExpressionCollection.getChild(this);
  }

  @Override
  public void setConditionExpression(ConditionExpression conditionExpression) {
    conditionExpressionCollection.setChild(this, conditionExpression);
  }

  @Override
  public void removeConditionExpression() {
    conditionExpressionCollection.removeChild(this);
  }

  @Override
  public BpmnEdge getDiagramElement() {
    return (BpmnEdge) super.getDiagramElement();
  }

}
