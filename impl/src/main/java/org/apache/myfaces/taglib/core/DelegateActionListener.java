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

import javax.el.ELException;
import javax.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.component.StateHolder;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.ActionListener;

import org.apache.myfaces.util.lang.ClassUtils;


/**
 * This class is used in conjunction with ActionListenerTag. 
 * 
 * When a tag like this is in a jsp page:
 * <code>
 * &lt;f:actionListener binding="#{mybean}"/&gt;
 *  
 *  or
 *  
 * &lt;f:actionListener type="#{'anyid'}" binding="#{mybean}"/&gt;
 * </code>
 * 
 * The value of mybean could be already on the context, so this
 * converter avoid creating a new variable and use the previous one.
 * 
 * @author Leonardo Uribe (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class DelegateActionListener implements ActionListener, StateHolder
{

    private ValueExpression _type;
    private ValueExpression _binding;

    public DelegateActionListener()
    {
    }

    public DelegateActionListener(ValueExpression type, ValueExpression binding)
    {
        super();
        _type = type;
        _binding = binding;
    }

    @Override
    public boolean isTransient()
    {
        return false;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object state)
    {
        Object[] values = (Object[]) state;
        _type = (ValueExpression) values[0];
        _binding = (ValueExpression) values[1];
    }

    @Override
    public Object saveState(FacesContext facesContext)
    {
        Object[] values = new Object[2];
        values[0] = _type;
        values[1] = _binding;
        return values;
    }

    @Override
    public void setTransient(boolean arg0)
    {
        // Do nothing        
    }

    private ActionListener _getDelegate()
    {
        return _createActionListener();
    }

    private ActionListener _createActionListener()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ActionListener listener = null;
        // type and/or binding must be specified
        try
        {
            if (null != _binding)
            {
                try
                {
                    listener = (ActionListener) _binding.getValue(facesContext
                            .getELContext());
                    if (null != listener)
                    {
                        return listener;
                    }
                }
                catch (ELException e)
                {
                    throw new ELException("Exception while evaluating the binding attribute", e);
                }
            }
            if (null != _type)
            {
                String className;
                if (_type.isLiteralText())
                {
                    className = _type.getExpressionString();
                }
                else
                {
                    className = (String) _type.getValue(facesContext.getELContext());
                }

                listener = (ActionListener) ClassUtils.newInstance(className);
                if (null != _binding)
                {
                    try
                    {
                        _binding.setValue(facesContext.getELContext(), listener);
                    }
                    catch (ELException e)
                    {
                        throw new ELException("Exception while evaluating the binding attribute ");
                    }
                }
                return listener;
            }
        }
        catch (ClassCastException e)
        {
            throw new FacesException(e);
        }
        return listener;
    }

    @Override
    public void processAction(ActionEvent event) throws AbortProcessingException
    {
        _getDelegate().processAction(event);
    }

}
