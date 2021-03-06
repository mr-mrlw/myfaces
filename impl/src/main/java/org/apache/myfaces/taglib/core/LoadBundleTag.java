/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.taglib.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.el.ValueExpression;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspTag;
import org.apache.myfaces.config.MyfacesConfig;
import org.apache.myfaces.util.lang.ClassUtils;

/**
 * Loads a resource bundle and saves it as a variable in the request scope.
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * <p>
 * TODO: We should find a way to save loaded bundles in the state, because otherwise on the next request the bundle map
 * will not be present before the render phase and value bindings that reference to the bundle will always log annoying
 * "Variable 'xxx' could not be resolved" error messages.
 * </p>
 * 
 * @author Manfred Geiler (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
@JSFJspTag(name = "f:loadBundle", bodyContent = "empty")
public class LoadBundleTag extends TagSupport
{
    private static final long serialVersionUID = -8892145684062838928L;

    private ValueExpression _basename;
    private String _var;

    /**
     * The base name of the resource bundle.
     */
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    public void setBasename(ValueExpression basename)
    {
        _basename = basename;
    }

    /**
     * The name of the variable in request scope that the resources are saved to. This must be a static value.
     */
    @JSFJspAttribute(required = true)
    public void setVar(String var)
    {
        _var = var;
    }

    @Override
    public int doStartTag() throws JspException
    {
        if (null == _var)
        {
            throw new NullPointerException("LoadBundle: 'var' must not be null");
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null)
        {
            throw new JspException("No faces context?!");
        }

        UIViewRoot viewRoot = facesContext.getViewRoot();
        if (viewRoot == null)
        {
            throw new JspException("No view root! LoadBundle must be nested inside <f:view> action.");
        }

        Locale locale = viewRoot.getLocale();
        if (locale == null)
        {
            locale = facesContext.getApplication().getDefaultLocale();
        }

        String basename = null;
        if (_basename != null)
        {
            if (_basename.isLiteralText())
            {
                basename = _basename.getExpressionString();
            }
            else
            {
                basename = (String)_basename.getValue(facesContext.getELContext());
            }
        }

        if (null == basename)
        {
            throw new NullPointerException("LoadBundle: 'basename' must not be null");
        }

        ResourceBundle.Control bundleControl =
                MyfacesConfig.getCurrentInstance(facesContext).getResourceBundleControl();
        ResourceBundle bundle;
        try
        {
            if (bundleControl == null)
            {
                bundle = ResourceBundle.getBundle(basename, locale, ClassUtils.getContextClassLoader());
            }
            else
            {
                bundle = ResourceBundle.getBundle(basename, locale, ClassUtils.getContextClassLoader(),
                        bundleControl);
            }
        }
        catch (MissingResourceException e)
        {
            try
            {
                if (bundleControl == null)
                {
                    bundle = ResourceBundle.getBundle(basename, locale, this.getClass().getClassLoader());
                }
                else
                {
                    bundle = ResourceBundle.getBundle(basename, locale, this.getClass().getClassLoader(),
                            bundleControl);
                }
            }
            catch (MissingResourceException e1)
            {
                throw new JspException("Resource bundle '" + basename + "' could not be found.", e1);
            }
        }

        facesContext.getExternalContext().getRequestMap().put(_var, new BundleMap(bundle));
        return Tag.SKIP_BODY;
    }

    private static class BundleMap implements Map<String, String>
    {
        private ResourceBundle _bundle;
        private List<String> _values;

        public BundleMap(ResourceBundle bundle)
        {
            _bundle = bundle;
        }

        // Optimized methods

        @Override
        public String get(Object key)
        {
            try
            {
                return (String)_bundle.getObject(key.toString());
            }
            catch (Exception e)
            {
                return "???" + key + "???";
            }
        }

        @Override
        public boolean isEmpty()
        {
            return !_bundle.getKeys().hasMoreElements();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return _bundle.containsKey(key.toString());
        }

        // Unoptimized methods

        @Override
        public Collection<String> values()
        {
            if (_values == null)
            {
                _values = new ArrayList<>();
                for (Enumeration<String> enumer = _bundle.getKeys(); enumer.hasMoreElements();)
                {
                    String v = _bundle.getString(enumer.nextElement());
                    _values.add(v);
                }
            }
            return _values;
        }

        @Override
        public int size()
        {
            return values().size();
        }

        @Override
        public boolean containsValue(Object value)
        {
            return values().contains(value);
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet()
        {
            Set<Entry<String, String>> set = new HashSet<>();
            for (Enumeration<String> enumer = _bundle.getKeys(); enumer.hasMoreElements();)
            {
                final String k = enumer.nextElement();
                set.add(new Map.Entry<String, String>()
                {
                    @Override
                    public String getKey()
                    {
                        return k;
                    }

                    @Override
                    public String getValue()
                    {
                        return (String)_bundle.getObject(k);
                    }

                    @Override
                    public String setValue(String value)
                    {
                        throw new UnsupportedOperationException(this.getClass().getName()
                                + " UnsupportedOperationException");
                    }
                });
            }
            return set;
        }

        @Override
        public Set<String> keySet()
        {
            Set<String> set = new HashSet<>();
            for (Enumeration<String> enumer = _bundle.getKeys(); enumer.hasMoreElements();)
            {
                set.add(enumer.nextElement());
            }
            return set;
        }

        // Unsupported methods

        @Override
        public String remove(Object key)
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> t)
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }

        @Override
        public String put(String key, String value)
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }
    }

}
