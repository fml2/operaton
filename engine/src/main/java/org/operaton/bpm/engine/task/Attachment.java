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
package org.operaton.bpm.engine.task;

import java.util.Date;

import org.operaton.bpm.engine.TaskService;



/** Any type of content that is be associated with
 * a task or with a process instance.
 *
 * @author Tom Baeyens
 */
public interface Attachment {

  /** unique id for this attachment */
  String getId();

  /** free user defined short (max 255 chars) name for this attachment */
  String getName();

  /** free user defined short (max 255 chars) name for this attachment */
  void setName(String name);

  /** long (max 255 chars) explanation what this attachment is about in context of the task and/or process instance it's linked to. */
  String getDescription();

  /** long (max 255 chars) explanation what this attachment is about in context of the task and/or process instance it's linked to. */
  void setDescription(String description);

  /** indication of the type of content that this attachment refers to. Can be mime type or any other indication. */
  String getType();

  /** reference to the task to which this attachment is associated. */
  String getTaskId();

  /** reference to the process instance to which this attachment is associated. */
  String getProcessInstanceId();

  /** the remote URL in case this is remote content.  If the attachment content was
   * {@link TaskService#createAttachment(String, String, String, String, String, java.io.InputStream) uploaded with an input stream},
   * then this method returns null and the content can be fetched with {@link TaskService#getAttachmentContent(String)}. */
  String getUrl();

  /** The time when the attachment was created. */
  Date getCreateTime();

  /** reference to the root process instance id of the process instance on which this attachment was made */
  String getRootProcessInstanceId();

  /** The time the historic attachment will be removed. */
  Date getRemovalTime();

}
