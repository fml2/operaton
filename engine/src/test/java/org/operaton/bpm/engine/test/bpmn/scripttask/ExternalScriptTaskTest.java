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
package org.operaton.bpm.engine.test.bpmn.scripttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.ScriptCompilationException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Sebastian Menski
 */
class ExternalScriptTaskTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;

  @Deployment
  @Test
  void testDefaultExternalScript() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testDefaultExternalScriptAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testDefaultExternalScriptAsVariable.bpmn20.xml"})
  @Test
  void testDefaultExternalScriptAsNonExistingVariable() {
    try {
      runtimeService.startProcessInstanceByKey("process");
      fail("Process variable 'scriptPath' not defined");
    }
    catch(ProcessEngineException e) {
      testRule.assertTextPresentIgnoreCase("Cannot resolve identifier 'scriptPath'", e.getMessage());
    }
  }

  @Deployment
  @Test
  void testDefaultExternalScriptAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptResourceBean", new ScriptResourceBean());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptInClasspath() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptInClasspathAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "classpath://org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptInClasspathAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptResourceBean", new ScriptResourceBean());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptNotFoundInClasspath() {
    try {
      runtimeService.startProcessInstanceByKey("process");
      fail("Resource does not exist in classpath");
    }
    catch (NotFoundException e) {
      testRule.assertTextPresentIgnoreCase("unable to find resource at path classpath://org/operaton/bpm/engine/test/bpmn/scripttask/notexisting.py", e.getMessage());
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeployment.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py"
  })
  @Test
  void testScriptInDeployment() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeployment.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py"
  })
  @Test
  void testScriptInDeploymentAfterCacheWasCleaned() {
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeploymentAsVariable.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py"
  })
  @Test
  void testScriptInDeploymentAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "deployment://org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeploymentAsBean.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py"
  })
  @Test
  void testScriptInDeploymentAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptResourceBean", new ScriptResourceBean());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptNotFoundInDeployment() {
    try {
      runtimeService.startProcessInstanceByKey("process");
      fail("Resource does not exist in classpath");
    }
    catch (NotFoundException e) {
      testRule.assertTextPresentIgnoreCase("unable to find resource at path deployment://org/operaton/bpm/engine/test/bpmn/scripttask/notexisting.py", e.getMessage());
    }
  }

  @Deployment
  @Test
  void testNotExistingImport() {
    try {
      runtimeService.startProcessInstanceByKey("process");
      fail("Should fail during script compilation");
    }
    catch (ScriptCompilationException e) {
      testRule.assertTextPresentIgnoreCase("import unknown", e.getMessage());
    }
  }

}
