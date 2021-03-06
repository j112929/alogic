package com.logicbus.service;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.anysoft.metrics.core.MetricsHandler;
import com.anysoft.util.Settings;
import com.logicbus.backend.AbstractServant;
import com.logicbus.backend.Context;
import com.logicbus.backend.ServantException;
import com.logicbus.backend.message.JsonMessage;
import com.logicbus.backend.message.XMLMessage;
import com.logicbus.models.servant.ServiceDescription;

/**
 * 指标处理报告
 * @author duanyy
 * 
 * @since 1.2.8
 * @version 1.4.0 [20141117 duanyy] <br>
 * - 将MessageDoc和Context进行合并整合 <br>
 */
public class MetricsReport extends AbstractServant{

	
	protected void onDestroy() {
	}

	
	protected void onCreate(ServiceDescription sd) throws ServantException{
	}

	
	protected int onXml(Context ctx) throws Exception{
		XMLMessage msg = (XMLMessage) ctx.asMessage(XMLMessage.class);
		
		Settings settings = Settings.get();
		
		MetricsHandler handler = (MetricsHandler) settings.get("metricsHandler");
		
		if (handler != null){
			Element root = msg.getRoot();
			Document doc = root.getOwnerDocument();
			
			Element logger = doc.createElement(handler.getHandlerType());
			
			handler.report(logger);
			
			root.appendChild(logger);
		}
		return 0;
	}

	
	protected int onJson(Context ctx) throws Exception{
		JsonMessage msg = (JsonMessage)ctx.asMessage(JsonMessage.class);
		
		Settings settings = Settings.get();
		
		MetricsHandler handler = (MetricsHandler) settings.get("metricsHandler");
		if (handler != null){
			Map<String,Object> root = msg.getRoot();
		
			Map<String,Object> logger = new HashMap<String,Object>();
			
			handler.report(logger);
			
			root.put(handler.getHandlerType(), logger);
		}
		return 0;
	}


}