/*
 * JBoss, Home of Professional Open Source  
 * 
 * Distributable under LGPL license. 
 * See terms of license at gnu.org.  
 */
package org.jboss.seam.contexts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.jboss.seam.ScopeType;
import org.jboss.seam.Seam;
import org.jboss.seam.core.Events;

/**
 * The page context allows you to store state during a request that
 * renders a page, and access that state from any postback request
 * that originates from that page. The state is destroyed at the 
 * end of the second request. During the RENDER_RESPONSE phase,
 * the page context instance refers to the page that is about to
 * be rendered. Prior to the INVOKE_APPLICATION phase, it refers
 * to the page that was the source of the request. During the
 * INVOKE_APPLICATION phase, set() and remove() manipulate the
 * context of the page that is about to be rendered, while get()
 * returns values from the page that was the source of the request.
 * 
 * @author Gavin King
 * @version $Revision$
 */
public class PageContext implements Context {

   private Map<String, Object> previousPageMap;
   private Map<String, Object> nextPageMap;
   
   public PageContext()
   {
      previousPageMap = (Map<String, Object>) getAttributeMap();
      nextPageMap = new HashMap<String, Object>();
   }

   public ScopeType getType()
   {
      return ScopeType.PAGE;
   }
   
   private String getKey(String name)
   {
      return /*getPrefix() +*/ name;
   }

   /*private String getPrefix()
   {
      return ScopeType.PAGE.getPrefix() + '$';
   }*/

	public Object get(String name) 
   {
      return getCurrentReadableMap().get( getKey(name) );
	}
   
   public boolean isSet(String name) 
   {
      return getCurrentReadableMap().containsKey( getKey(name) );
   }
   
   private Map<String, Object> getCurrentReadableMap()
   {
      return isRenderResponsePhase() ?
            nextPageMap : previousPageMap;
   }

   private Map<String, Object> getCurrentWritableMap()
   {
      return isBeforeInvokeApplicationPhase() ?
            previousPageMap : nextPageMap;
   }

	public void set(String name, Object value) 
   {
      if ( Events.exists() ) Events.instance().raiseEvent("org.jboss.seam.preSetVariable." + name);
      getCurrentWritableMap().put( getKey(name), value );
      if ( Events.exists() ) Events.instance().raiseEvent("org.jboss.seam.postSetVariable." + name);
	}

	public void remove(String name) 
   {
      if ( Events.exists() ) Events.instance().raiseEvent("org.jboss.seam.preRemoveVariable." + name);
      getCurrentWritableMap().remove( getKey(name) );
      if ( Events.exists() ) Events.instance().raiseEvent("org.jboss.seam.postRemoveVariable." + name);
	}

   public String[] getNames() {
      return getCurrentReadableMap().keySet().toArray( new String[]{} );
   }
   
   public String toString()
   {
      return "PageContext";
   }

   public Object get(Class clazz)
   {
      return get( Seam.getComponentName(clazz) );
   }

   /**
    * Put the buffered context variables in the faces view root, 
    * at the beginning of the render phase.
    */
   public void flush()
   {
      Map attributeMap = getAttributeMap();
      attributeMap.putAll(nextPageMap);
      nextPageMap = attributeMap;
   }

   private static Map getAttributeMap()
   {
      FacesContext facesContext = FacesContext.getCurrentInstance();
      if (facesContext==null)
      {
         throw new IllegalStateException("no FacesContext bound to current thread");
      }
      UIViewRoot viewRoot = facesContext.getViewRoot();
      return viewRoot==null ? Collections.EMPTY_MAP : viewRoot.getAttributes();
   }

   private static PhaseId getPhaseId()
   {
      PhaseId phaseId = Lifecycle.getPhaseId();
      if (phaseId==null)
      {
         throw new IllegalStateException("No phase id bound to current thread (make sure you do not have two SeamPhaseListener instances installed)");
      }
      return phaseId;
   }

   private static boolean isBeforeInvokeApplicationPhase()
   {
      return getPhaseId().compareTo(PhaseId.INVOKE_APPLICATION) < 0;
   }

   private static boolean isRenderResponsePhase()
   {
      return getPhaseId().compareTo(PhaseId.INVOKE_APPLICATION) > 0;
   }

}
