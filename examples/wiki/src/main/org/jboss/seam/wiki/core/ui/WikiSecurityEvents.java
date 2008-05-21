/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.wiki.core.ui;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.security.FacesSecurityEvents;

/**
 * Overrides the "login failed" message and turns it into a WARN (we don't want INFO here).
 *
 * @author Christian Bauer
 */
@Name("org.jboss.seam.security.facesSecurityEvents")
@Install(precedence = Install.APPLICATION, classDependencies = "javax.faces.context.FacesContext")
@BypassInterceptors
@Startup
public class WikiSecurityEvents extends FacesSecurityEvents {

    @Override
    public StatusMessage.Severity getLoginFailedMessageSeverity() {
        return StatusMessage.Severity.WARN;
    }
}
