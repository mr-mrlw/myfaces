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

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspTag;
import org.apache.myfaces.renderkit.html.util.JSFAttr;

import javax.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIOutput;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;

/**
 * @author Manfred Geiler (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
@JSFJspTag(name = "f:verbatim", bodyContent = "JSP")
public class VerbatimTag extends UIComponentELTagBase
{
    // private static final Log log = LogFactory.getLog(VerbatimTag.class);

    @Override
    public String getComponentType()
    {
        return "javax.faces.Output";
    }

    @Override
    public String getRendererType()
    {
        return "javax.faces.Text";
    }

    // HtmlOutputText attributes
    private ValueExpression _escape;
    private ValueExpression _rendered;

    @Override
    protected void setProperties(UIComponent component)
    {
        super.setProperties(component);

        setBooleanProperty(component, JSFAttr.ESCAPE_ATTR, _escape, Boolean.FALSE);
        setBooleanProperty(component, JSFAttr.RENDERED, _rendered, Boolean.TRUE);

        // No need to save component state
        component.setTransient(true);
    }

    /**
     * If true, generated markup is escaped. Default: false.
     */
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.Boolean")
    public void setEscape(ValueExpression escape)
    {
        _escape = escape;
    }

    /**
     * Flag indicating whether or not this component should be rendered (during Render Response Phase), or processed on
     * any subsequent form submit. The default value for this property is true.
     */
    @Override
    @JSFJspAttribute(className="javax.el.ValueExpression",
            deferredValueType="java.lang.Boolean")
    public void setRendered(ValueExpression rendered)
    {
        _rendered = rendered;
    }

    @Override
    public int doAfterBody() throws JspException
    {
        BodyContent bodyContent = getBodyContent();
        if (bodyContent != null)
        {
            UIOutput component = (UIOutput)getComponentInstance();
            component.setValue(bodyContent.getString());
            bodyContent.clearBody();
        }
        return super.getDoAfterBodyValue();
    }
}
