/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.example.common.test.booking.graphene;

import com.google.common.base.Predicate;
import org.jboss.arquillian.container.test.api.Deployment;
import static org.jboss.arquillian.graphene.Graphene.*;
import org.jboss.seam.example.common.test.DeploymentResolver;
import org.jboss.seam.example.common.test.SeamGrapheneTest;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author jbalunas
 * @author jharting
 *
 */
public class BookingFunctionalTestBase extends SeamGrapheneTest {

    private final String DEFAULT_USERNAME = "demo";

    private final String DEFAULT_PASSWORD = "demo";

    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        return DeploymentResolver.createDeployment();
    }

    @Before
    public void setUp() {
        assertTrue("Login failed.", login());
    }

    @After
    public void tearDown() {
        logout();
    }

    public boolean login() {
        return login(DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    public boolean login(String username, String password) {
        if (isLoggedIn()) {
            fail("User already logged in.");
        }
        open(contextPath + getProperty("HOME_PAGE"));
        if (!browser.getTitle().equals(getProperty("PAGE_TITLE"))) {
            return false;
        }
        type(getBy("LOGIN_USERNAME_FIELD"), username);
        type(getBy("LOGIN_PASSWORD_FIELD"), password);
        clickAndWaitHttp(getBy("LOGIN_SUBMIT"));
        return isLoggedIn();
    }

    public void logout() {
        if (isLoggedIn()) {
            clickAndWaitHttp(getBy("LOGOUT"));
        }
    }

    public boolean isLoggedIn() {
        return isElementPresent(getBy("LOGOUT"));
    }

    public void enterSearchQuery(String query) {
        if ("FALSE".equalsIgnoreCase(getProperty("USE_AJAX_SEARCH"))) {
            enterSearchQueryWithoutAJAX(query);
        } else {
            if ("TRUE".equalsIgnoreCase(getProperty("USE_SEARCH_BUTTON"))) {
                enterSearchQueryUsingAJAX(query, true);
            } else {
                enterSearchQueryUsingAJAX(query, false);
            }
        }
    }

    public void enterSearchQueryUsingAJAX(String query, boolean click) {
        setTextInputValue(getBy("SEARCH_STRING_FIELD"), query.substring(0, query.length() - 1));
        type(getBy("SEARCH_STRING_FIELD"), query.substring(query.length() - 1), false);
        if (click) {
            clickAndWaitAjax(getBy("SEARCH_SUBMIT"));
        }
        waitModel(browser).until(element(getBy("SPINNER")).not().isVisible());// ugly
        waitModel(browser).until(new Predicate() {
            public boolean apply(Object input) {
                return isElementPresent(getBy("SEARCH_RESULT_TABLE")) || isElementPresent(getBy("NO_HOTELS_FOUND"));
            }
        });
    }

    public void enterSearchQueryWithoutAJAX(String query) {
        type(getBy("SEARCH_STRING_FIELD"), query);
        clickAndWaitHttp(getBy("SEARCH_SUBMIT"));
    }
}
