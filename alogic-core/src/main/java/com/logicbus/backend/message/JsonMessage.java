package com.logicbus.backend.message;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.anysoft.util.IOTools;
import com.anysoft.util.JsonTools;
import com.anysoft.util.Settings;
import com.jayway.jsonpath.spi.JsonProvider;
import com.jayway.jsonpath.spi.JsonProviderFactory;
import com.logicbus.backend.Context;
import com.logicbus.backend.message.Message;

/**
 * 基于JSON的消息协议
 * @author duanyy
 * 
 * @since 1.2.1
 * 
 * @version 1.4.0 [20141117 duanyy] <br>
 * - Message被改造为接口 <br>
 * 
 * @version 1.6.1.1 [20141118 duanyy] <br>
 * - 修正没有读入的情况下,root为空的bug <br>
 * - MessageDoc暴露InputStream和OutputStream <br>
 * 
 * @version 1.6.1.2 [20141118 duanyy] <br>
 * - 支持MessageDoc的Raw数据功能 <br>
 * 
 * @version 1.6.2.1 [20141223 duanyy] <br>
 * - 增加对Comet的支持 <br>
 * 
 * @version 1.6.3.14 [20150409 duanyy] <br>
 * - 修正formContentType所取的参数名问题，笔误 <br>
 */
public class JsonMessage implements Message {
	protected static final Logger logger = LogManager.getLogger(JsonMessage.class);
	
	@SuppressWarnings("unchecked")
	public void init(MessageDoc ctx) {
		String data = null;
		
		{
			byte [] inputData = ctx.getRequestRaw();
			if (inputData != null){
				try {
					data = new String(inputData,ctx.getEncoding());
				}catch (Exception ex){
					
				}
			}
		}
		
		if (data == null){
			//当客户端通过form来post的时候，Message不去读取输入流。
			String _contentType = ctx.getReqestContentType();
			if (_contentType == null || !_contentType.startsWith(formContentType)){
				InputStream in = null;
				try {
					in = ctx.getInputStream();
					data = Context.readFromInputStream(in, ctx.getEncoding());
				}catch (Exception ex){
					logger.error("Error when reading data from inputstream",ex);
				}finally{
					IOTools.close(in);
				}
			}
		}
		
		if (data != null && data.length() > 0){
			JsonProvider provider = JsonProviderFactory.createProvider();
			Object rootObj = provider.parse(data);
			if (rootObj instanceof Map){
				root = (Map<String,Object>)rootObj;
			}
		}
		if (root == null){
			root = new HashMap<String,Object>();
		}
	}

	public void finish(MessageDoc ctx,boolean closeStream) {
		Map<String,Object> _root = getRoot();
		JsonTools.setString(_root, "code", ctx.getReturnCode());
		JsonTools.setString(_root, "reason", ctx.getReason());
		JsonTools.setString(_root, "duration", String.valueOf(ctx.getDuration()));
		JsonTools.setString(_root, "host", ctx.getHost());
		JsonTools.setString(_root, "serial", ctx.getGlobalSerial());
				
		OutputStream out = null;
		try {

			String data = provider.toJson(_root);
			
			String jsonp = ctx.GetValue("jsonp", "");
			if (jsonp != null && jsonp.length() > 0){
				data = jsonp + "(" + data + ")";
			}
			
			out = ctx.getOutputStream();
			ctx.setResponseContentType("application/json;charset=" + ctx.getEncoding());
			Context.writeToOutpuStream(out, data, ctx.getEncoding());
			out.flush();
		}catch (Exception ex){
			logger.error("Error when writing data to outputstream",ex);
		}finally{
			if (closeStream)
				IOTools.close(out);
		}
	}	

	
	/**
	 * Json结构的根节点
	 */
	protected Map<String,Object> root = null;
	
	/**
	 * 获取JSON结构的根节点
	 * 
	 * @return JSON结构的根节点
	 */
	public Map<String,Object> getRoot(){
		return root;
	}
	
	public String toString(){
		return provider.toJson(root);
	}
	
	protected static JsonProvider provider = null;
	
	static {
		provider = JsonProviderFactory.createProvider();
	}
	
	protected static String formContentType = "application/x-www-form-urlencoded";
	static {
		formContentType = Settings.get().GetValue("http.formContentType",
				"application/x-www-form-urlencoded");
	}

}
