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
package org.operaton.bpm.model.cmmn.impl.instance;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_AUTHORIZED_ROLE_REFS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_USER_EVENT_LISTENER;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.EventListener;
import org.operaton.bpm.model.cmmn.instance.Role;
import org.operaton.bpm.model.cmmn.instance.UserEventListener;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReferenceCollection;

/**
 * @author Roman Smirnov
 *
 */
public class UserEventListenerImpl extends EventListenerImpl implements UserEventListener {

  protected static AttributeReferenceCollection<Role> authorizedRoleRefCollection;

  public UserEventListenerImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<Role> getAuthorizedRoles() {
    return authorizedRoleRefCollection.getReferenceTargetElements(this);
  }


  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(UserEventListener.class, CMMN_ELEMENT_USER_EVENT_LISTENER)
        .namespaceUri(CMMN11_NS)
        .extendsType(EventListener.class)
        .instanceProvider(UserEventListenerImpl::new);

    authorizedRoleRefCollection = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_AUTHORIZED_ROLE_REFS)
        .idAttributeReferenceCollection(Role.class, CmmnAttributeElementReferenceCollection.class)
        .build();

    typeBuilder.build();
  }

}
