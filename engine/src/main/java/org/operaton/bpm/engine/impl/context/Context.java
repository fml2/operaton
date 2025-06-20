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
package org.operaton.bpm.engine.impl.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.ProcessApplicationUnavailableException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandInvocationContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutorContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Thorben Lindhauer
 */
public class Context {
  protected static ThreadLocal<Deque<CommandContext>> commandContextThreadLocal = new ThreadLocal<>();

  protected static ThreadLocal<Deque<CommandInvocationContext>> commandInvocationContextThreadLocal = new ThreadLocal<>();

  protected static ThreadLocal<Deque<ProcessEngineConfigurationImpl>> processEngineConfigurationStackThreadLocal = new ThreadLocal<>();
  protected static ThreadLocal<Deque<CoreExecutionContext<? extends CoreExecution>>> executionContextStackThreadLocal = new ThreadLocal<>();
  protected static ThreadLocal<JobExecutorContext> jobExecutorContextThreadLocal = new ThreadLocal<>();
  protected static ThreadLocal<Deque<ProcessApplicationReference>> processApplicationContext = new ThreadLocal<>();

  private Context() {
  }

  public static CommandContext getCommandContext() {
    Deque<CommandContext> stack = getStack(commandContextThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setCommandContext(CommandContext commandContext) {
    getStack(commandContextThreadLocal).push(commandContext);
  }

  public static void removeCommandContext() {
    getStack(commandContextThreadLocal).pop();
  }

  public static CommandInvocationContext getCommandInvocationContext() {
    Deque<CommandInvocationContext> stack = getStack(commandInvocationContextThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setCommandInvocationContext(CommandInvocationContext commandInvocationContext) {
    getStack(commandInvocationContextThreadLocal).push(commandInvocationContext);
  }

  public static void removeCommandInvocationContext() {
    Deque<CommandInvocationContext> stack = getStack(commandInvocationContextThreadLocal);
    CommandInvocationContext currentContext = stack.pop();
    if (stack.isEmpty()) {
      // do not clear when called from JobExecutor, will be cleared there after logging
      if (getJobExecutorContext() == null) {
        // outer command remove flow
        currentContext.getProcessDataContext().clearMdc();
        currentContext.getProcessDataContext().restoreExternalMDCProperties();
      }
    } else {
      // reset the MDC to the logging context of the outer command invocation
      // inner command remove flow
      stack.peek().getProcessDataContext().updateMdcFromCurrentValues();
    }
  }

  public static ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    Deque<ProcessEngineConfigurationImpl> stack = getStack(processEngineConfigurationStackThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
    getStack(processEngineConfigurationStackThreadLocal).push(processEngineConfiguration);
  }

  public static void removeProcessEngineConfiguration() {
    getStack(processEngineConfigurationStackThreadLocal).pop();
  }

  /**
   * @deprecated Use {@link #getBpmnExecutionContext()} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public static ExecutionContext getExecutionContext() {
    return getBpmnExecutionContext();
  }

  public static BpmnExecutionContext getBpmnExecutionContext() {
    return (BpmnExecutionContext) getCoreExecutionContext();
  }

  public static CaseExecutionContext getCaseExecutionContext() {
    return (CaseExecutionContext) getCoreExecutionContext();
  }

  public static CoreExecutionContext<? extends CoreExecution> getCoreExecutionContext() {
    Deque<CoreExecutionContext<? extends CoreExecution>> stack = getStack(executionContextStackThreadLocal);
    if(stack == null || stack.isEmpty()) {
      return null;
    } else {
      return stack.peek();
    }
  }

  public static void setExecutionContext(ExecutionEntity execution) {
    getStack(executionContextStackThreadLocal).push(new BpmnExecutionContext(execution));
  }

  public static void setExecutionContext(CaseExecutionEntity execution) {
    getStack(executionContextStackThreadLocal).push(new CaseExecutionContext(execution));
  }

  public static void removeExecutionContext() {
    getStack(executionContextStackThreadLocal).pop();
  }

  protected static <T> Deque<T> getStack(ThreadLocal<Deque<T>> threadLocal) {
    Deque<T> stack = threadLocal.get();
    if (stack==null) {
      stack = new ArrayDeque<>();
      threadLocal.set(stack);
    }
    return stack;
  }

  public static JobExecutorContext getJobExecutorContext() {
    return jobExecutorContextThreadLocal.get();
  }

  public static void setJobExecutorContext(JobExecutorContext jobExecutorContext) {
    jobExecutorContextThreadLocal.set(jobExecutorContext);
  }

  public static void removeJobExecutorContext() {
    jobExecutorContextThreadLocal.remove();
  }


  public static ProcessApplicationReference getCurrentProcessApplication() {
    Deque<ProcessApplicationReference> stack = getStack(processApplicationContext);
    if(stack.isEmpty()) {
      return null;
    } else {
      return stack.peek();
    }
  }

  public static void setCurrentProcessApplication(ProcessApplicationReference reference) {
    Deque<ProcessApplicationReference> stack = getStack(processApplicationContext);
    stack.push(reference);
  }

  public static void removeCurrentProcessApplication() {
    Deque<ProcessApplicationReference> stack = getStack(processApplicationContext);
    stack.pop();
  }

  /**
   * Use {@link #executeWithinProcessApplication(Callable, ProcessApplicationReference, InvocationContext)}
   * instead if an {@link InvocationContext} is available.
   */
  public static <T> T executeWithinProcessApplication(Callable<T> callback, ProcessApplicationReference processApplicationReference) {
    return executeWithinProcessApplication(callback, processApplicationReference, null);
  }

  public static <T> T executeWithinProcessApplication(Callable<T> callback, ProcessApplicationReference processApplicationReference, InvocationContext invocationContext) {
    String paName = processApplicationReference.getName();
    try {
      ProcessApplicationInterface processApplication = processApplicationReference.getProcessApplication();
      setCurrentProcessApplication(processApplicationReference);

      try {
        // wrap callback
        ProcessApplicationClassloaderInterceptor<T> wrappedCallback = new ProcessApplicationClassloaderInterceptor<>(callback);
        // execute wrapped callback
        return processApplication.execute(wrappedCallback, invocationContext);

      } catch (Exception e) {

        // unwrap exception
        if(e.getCause() != null && e.getCause() instanceof RuntimeException runtimeException) {
          throw runtimeException;
        }else {
          throw new ProcessEngineException("Unexpected exeption while executing within process application ", e);
        }

      } finally {
        removeCurrentProcessApplication();
      }


    } catch (ProcessApplicationUnavailableException e) {
      throw new ProcessEngineException("Cannot switch to process application '"+paName+"' for execution: "+e.getMessage(), e);
    }
  }
}
