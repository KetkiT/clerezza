/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License"). You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above. However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 * 
 * trialox.org (trialox AG, Switzerland) elects to include this software in this
 * distribution under the CDDL license.
 */ 

package org.apache.clerezza.triaxrs.headerDelegate;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 *  parsed and fixed from jersey
 * 
 */
public class CookieProvider implements HeaderDelegate<Cookie> {

    @Override
    public String toString(Cookie cookie) {
        StringBuilder b = new StringBuilder();

        b.append(cookie.getName()).append('=');
        b.append(cookie.getValue());
		b.append(";$Version=").append(cookie.getVersion());

        if (cookie.getDomain() != null) {
            b.append(";$Domain=");
            b.append(cookie.getDomain());
        }
        if (cookie.getPath() != null) {
            b.append(";$Path=");
            b.append(cookie.getPath());
        }
        return b.toString();
    }

    @Override
    public Cookie fromString(String header) {
        Map<String, Cookie> cookies = parseCookies(header);
        return cookies.entrySet().iterator().next().getValue();
    }

    private static Map<String, Cookie> parseCookies(String header) {
        String bites[] = header.split("[;,]");
        Map<String, Cookie> cookies = new LinkedHashMap<String, Cookie>();
        int version = 0;
        MutableCookie cookie = null;
        for (String bite : bites) {
            String crumbs[] = bite.split("=", 2);
            String name = crumbs.length > 0 ? crumbs[0].trim() : "";
            String value = crumbs.length > 1 ? crumbs[1].trim() : "";
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }
            if (!name.startsWith("$")) {
                if (cookie != null) {
                    cookies.put(cookie.name, cookie.getImmutableCookie());
                }
                cookie = new MutableCookie(name, value);
                cookie.version = version;
            } else if (name.startsWith("$Version")) {
                version = Integer.parseInt(value);
            } else if (name.startsWith("$Path") && cookie != null) {
                cookie.path = value;
            } else if (name.startsWith("$Domain") && cookie != null) {
                cookie.domain = value;
            }
        }
        if (cookie != null) {
            cookies.put(cookie.name, cookie.getImmutableCookie());
        }
        return cookies;
    }

    private static class MutableCookie {

        String name;
        String value;
        int version = Cookie.DEFAULT_VERSION;
        String path = null;
        String domain = null;

        public MutableCookie(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Cookie getImmutableCookie() {
            return new Cookie(name, value, path, domain, version);
        }
    }
}