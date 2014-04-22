package com.jexbox.connector.jsf;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jexbox.connector.http.JexboxConnectorHttp;
import com.sun.faces.facelets.util.DevTools;

public class JexboxExceptionHandler extends ExceptionHandlerWrapper {
    private static final Logger _logger = Logger.getLogger(JexboxExceptionHandler.class.getCanonicalName());
    private ExceptionHandler _wrapped;
	private JexboxConnectorHttp _jexbox;

    public JexboxExceptionHandler(JexboxConnectorHttp jexbox, ExceptionHandler exception) {
        _wrapped = exception;
        _jexbox = jexbox;
    }
 
    @Override
    public ExceptionHandler getWrapped() {
        return _wrapped;
    }
 
    @Override
    public void handle() throws FacesException {
        Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator();
        while (i.hasNext()) {
            ExceptionQueuedEvent event = i.next();
            try {
                ExceptionQueuedEventContext eqec = (ExceptionQueuedEventContext) event.getSource();
                Throwable t = eqec.getException();
                if(t == null) continue;
        		Throwable filtered = getRootCause(t);
                
                FacesContext fc = FacesContext.getCurrentInstance();
                ExternalContext ec = fc.getExternalContext();
                
                String pageTrace = getPageStack(fc);
        		Map<String, String> meta = new HashMap<String, String>();
        		String pageTrace64 = Base64.encodeBase64String(pageTrace.getBytes());
        		meta.put("data", pageTrace64);
        		Map<String, Map<String, String>> meta2 = new HashMap<String, Map<String, String>>();
        		meta2.put("Page Trace", meta);

                Object request = ec.getRequest();
                if(request instanceof HttpServletRequest){
            		_jexbox.sendWithMeta(filtered, (HttpServletRequest)request, meta2);
                }else{
                    JsonObject json = _jexbox.json(filtered, meta2);
        			addExternalContextMetaData(ec, json);
        			addSessionMetaData(ec, json);
        			_jexbox.sendJson(json);
                }
            }catch(Throwable e){
            	_logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        getWrapped().handle();
    }
    
	protected void addSessionMetaData(ExternalContext ec, JsonObject json){
		JsonObject meta = json.getAsJsonObject("meta");
		if(meta == null){
			meta = new JsonObject();
			json.add("meta", meta);
		}
		
		JsonObject sessionD = new JsonObject();
		meta.add("Session", sessionD);
		
		Map<String, Object> sessionMap = ec.getRequestMap();
		for (String name : sessionMap.keySet()) {
			Object attr = sessionMap.get(name);
			sessionD.add(name, new JsonPrimitive(String.valueOf(attr)));
		}
	}

    protected void addExternalContextMetaData(ExternalContext ec, JsonObject json){
		JsonObject meta = json.getAsJsonObject("meta");
		if(meta == null){
			meta = new JsonObject();
			json.add("meta", meta);
		}

		Map<String, Object> attrMap = ec.getRequestMap();
		JsonObject reqAttr = new JsonObject();
		meta.add("Request Attr", reqAttr);
		for (String name : attrMap.keySet()) {
			Object attr = attrMap.get(name);
			if(attr != null){
				reqAttr.add(name, new JsonPrimitive(String.valueOf(attr)));
			}else{
				reqAttr.add(name, new JsonPrimitive("null"));
			}
		}
		
		Map<String, String[]> paramsMap = ec.getRequestParameterValuesMap();
		JsonObject reqPara = new JsonObject();
		meta.add("Request Params", reqPara);
		for (String name : paramsMap.keySet()) {
			String[] attr = paramsMap.get(name);
			if(attr != null){
				
				reqPara.add(name, new JsonPrimitive(Arrays.toString(attr)));
			}else{
				reqPara.add(name, new JsonPrimitive("null"));
			}
		}
		
		Map<String, String[]> headersMap = ec.getRequestHeaderValuesMap();
		JsonObject reqHead = new JsonObject();
		meta.add("Request Headers", reqHead);
		for (String name : headersMap.keySet()) {
			String[] attr = headersMap.get(name);
			if(attr != null){
				reqHead.add(name, new JsonPrimitive(Arrays.toString(attr)));
			}else{
				reqHead.add(name, new JsonPrimitive("null"));
			}
		}
		
		
		JsonObject req = new JsonObject();
		meta.add("Request", req);
		
		req.add("Auth Type", new JsonPrimitive(String.valueOf(ec.getAuthType())));
		req.add("Character Encoding", new JsonPrimitive(String.valueOf(ec.getRequestCharacterEncoding())));
		req.add("Content Type", new JsonPrimitive(String.valueOf(ec.getRequestContentType())));
		req.add("Context Path", new JsonPrimitive(String.valueOf(ec.getRequestContextPath())));
		req.add("Locale", new JsonPrimitive(String.valueOf(ec.getRequestLocale())));
		req.add("Path Info", new JsonPrimitive(String.valueOf(ec.getRequestPathInfo())));
		req.add("Remote User", new JsonPrimitive(String.valueOf(ec.getRemoteUser())));
		req.add("Requested Session Id", new JsonPrimitive(String.valueOf(ec.getSessionId(false))));
		req.add("Scheme", new JsonPrimitive(String.valueOf(ec.getRequestScheme())));
		req.add("Server Name", new JsonPrimitive(String.valueOf(ec.getRequestServerName())));
		req.add("Servlet Path", new JsonPrimitive(String.valueOf(ec.getRequestServletPath())));
		req.add("Content Length", new JsonPrimitive(String.valueOf(ec.getRequestContentLength())));
		
		req.add("Context Name", new JsonPrimitive(String.valueOf(ec.getContextName())));
		req.add("Request Server Port", new JsonPrimitive(String.valueOf(ec.getRequestServerPort())));

    }
    
    protected String getPageStack(FacesContext fc){
        try {
            UIViewRoot view = fc.getViewRoot();
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            PrintWriter pr = new PrintWriter(bas);
			DevTools.writeComponent(pr, view);
			pr.flush();
			String pageStack = bas.toString();
			pr.close();
			bas.close();
			return pageStack;
        } catch (Throwable e) {
        	_logger.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
    }
    
}

