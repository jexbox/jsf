package com.jexbox.connector.jsf;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

import com.jexbox.connector.http.JexboxConnectorHttp;

public abstract class JexboxExceptionHandlerFactory extends ExceptionHandlerFactory {
	private ExceptionHandlerFactory _parent;
	private JexboxConnectorHttp _jexbox = null;
	
	public JexboxExceptionHandlerFactory(ExceptionHandlerFactory parent) {
		_parent = parent;
		_jexbox = createJexboxConnectorHttp();
	}
	
	protected abstract JexboxConnectorHttp createJexboxConnectorHttp();
	 
	@Override
	public ExceptionHandler  getExceptionHandler() {
		ExceptionHandler handler = new JexboxExceptionHandler(_jexbox, _parent.getExceptionHandler());
		return handler;
	}
	 
}
