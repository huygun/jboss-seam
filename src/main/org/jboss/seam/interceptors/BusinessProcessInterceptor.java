/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.interceptors;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import java.util.Map;

import javax.ejb.AroundInvoke;
import javax.ejb.InvocationContext;
import javax.faces.context.FacesContext;

import org.jboss.logging.Logger;
import org.jboss.seam.Component;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.Around;
import org.jboss.seam.annotations.CompleteTask;
import org.jboss.seam.annotations.CreateProcess;
import org.jboss.seam.annotations.ResumeProcess;
import org.jboss.seam.annotations.ResumeTask;
import org.jboss.seam.annotations.StartTask;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Process;
import org.jboss.seam.core.ManagedJbpmSession;
import org.jbpm.db.JbpmSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.security.Authentication;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * Interceptor which handles interpretation of jBPM-related annotations.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 * @version $Revision$
 */
@Around({ValidationInterceptor.class, BijectionInterceptor.class, OutcomeInterceptor.class})
public class BusinessProcessInterceptor extends AbstractInterceptor
{
   private static final Logger log = Logger.getLogger( BusinessProcessInterceptor.class );

   @AroundInvoke
   public Object manageBusinessProcessContext(InvocationContext invocation) throws Exception
   {
      String actorId = (String) Contexts.lookupInStatefulContexts( "actorId" );
      if (actorId!=null) Authentication.pushAuthenticatedActorId( actorId );
      try
      {
         String componentName = Seam.getComponentName( invocation.getBean().getClass() );
         Method method = invocation.getMethod();
         log.trace( "Starting bpm interception [component=" + componentName + ", method=" + method.getName() + "]" );
   
         beforeInvocation( invocation );
         return afterInvocation( invocation, invocation.proceed() );
      }
      finally
      {
         if (actorId!=null) Authentication.popAuthenticatedActorId();
      }
   }

   private void beforeInvocation(InvocationContext invocationContext) {
      Method method = invocationContext.getMethod();
      if ( method.isAnnotationPresent( StartTask.class ) ) {
         log.trace( "encountered @StartTask" );
         StartTask tag = method.getAnnotation( StartTask.class );
         initTask( tag.taskIdParameter() );
      }
      else if ( method.isAnnotationPresent( ResumeTask.class ) ) {
         log.trace( "encountered @ResumeTask" );
         ResumeTask tag = method.getAnnotation( ResumeTask.class );
         initTask( tag.taskIdParameter() );
      }
      else if ( method.isAnnotationPresent( ResumeProcess.class ) ) {
         log.trace( "encountered @ResumeProcess" );
         ResumeProcess tag = method.getAnnotation( ResumeProcess.class );
         initProcess( tag.processIdParameter() );
      }
   }

   private void initProcess(String processIdParameter) {
      Process.instance().setProcessId( getRequestParamValueAsLong(processIdParameter) );
   }

   private void initTask(String taskIdParameter) {
      Process context = Process.instance();
      context.setTaskId( getRequestParamValueAsLong(taskIdParameter) );
      TaskInstance taskInstance = org.jboss.seam.core.TaskInstance.instance();
      context.setProcessId( taskInstance.getTaskMgmtInstance().getProcessInstance().getId() );
   }


   private Object afterInvocation(InvocationContext invocation, Object result)
   {
      if (result!=null) //interpreted as "redisplay"
      {
         Method method = invocation.getMethod();
         if ( method.isAnnotationPresent( CreateProcess.class ) )
         {
            log.trace( "encountered @CreateProcess" );
            CreateProcess tag = method.getAnnotation( CreateProcess.class );
            createProcess( tag.definition() );
         }
         else if ( method.isAnnotationPresent( StartTask.class ) )
         {
            log.trace( "encountered @StartTask" );
            //StartTask tag = method.getAnnotation( StartTask.class );
            startTask();
         }
         else if ( method.isAnnotationPresent( CompleteTask.class ) )
         {
            log.trace( "encountered @CompleteTask" );
            completeTask( component.getTransition( invocation.getBean() ) );
         }
      }
      return result;
   }

   private void createProcess(String processDefinitionName)
   {
      JbpmSession session = (JbpmSession) Component.getInstance(ManagedJbpmSession.class, true);
      
      ProcessDefinition pd = session.getGraphSession().findLatestProcessDefinition(processDefinitionName);
      if ( pd == null )
      {
         throw new IllegalArgumentException( "Unknown process definition: " + processDefinitionName );
      }
      ProcessInstance process = new ProcessInstance( pd );
      session.getGraphSession().saveProcessInstance( process );
      Process.instance().setProcessId( process.getId() );

      // need to set process variables before the signal
      Contexts.getBusinessProcessContext().flush();

      process.signal();
      session.getSession().flush();
   }


   private void startTask()
   {
      String actorId = (String) Contexts.lookupInStatefulContexts("actorId");
      TaskInstance task = org.jboss.seam.core.TaskInstance.instance();
      if ( actorId != null )
      {
         task.start(actorId);
      }
      else
      {
         task.start();
      }
   }

   private void completeTask(String transitionName)
   {
      TaskInstance task = org.jboss.seam.core.TaskInstance.instance();
      if ( task == null )
      {
         throw new IllegalStateException( "jBPM task instance not associated with context" );
      }

      if ( transitionName == null )
      {
         task.end();
      }
      else
      {
         task.end(transitionName);
      }
      Process context = Process.instance();
      context.setTaskId(null);
   }

    private Long getRequestParamValueAsLong(String paramName)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map paramMap = facesContext.getExternalContext()
              .getRequestParameterMap();
        String paramValue = (String) paramMap.get(paramName);
        PropertyEditor editor = PropertyEditorManager.findEditor(Long.class);
        if ( editor != null )
        {
            editor.setAsText(paramValue);
            return (Long) editor.getValue();
        }
        else
        {
            return Long.parseLong(paramValue);
        }
    }
}
