package com.alibaba.csb.sdk.internel;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.alibaba.csb.sdk.ContentBody;
import com.alibaba.csb.sdk.HttpCaller;
import com.alibaba.csb.sdk.ContentBody.Type;
import com.alibaba.csb.sdk.CsbSDKConstants;
import com.alibaba.csb.sdk.security.SignUtil;
import com.alibaba.csb.sdk.HttpCallerException;
//import com.alibaba.fastjson.JSONObject;

/**
 * HttpClient Helper Class
 *
 */
public class HttpClientHelper {
	public static void printDebugInfo(String msg) {
		if (HttpCaller.DEBUG)
			System.out.println(msg);
	}

	public static void mergeParams(Map<String, List<String>> urlParamsMap, Map<String, String> paramsMap, boolean decodeFlag) throws HttpCallerException {
		if (paramsMap != null) {
			//decode all params first, due to it will be encode to construct the request URL later
			String value;
			for (Entry<String,String> kv:paramsMap.entrySet()) {
				value = decodeValue(kv.getKey(), kv.getValue(), decodeFlag);
				urlParamsMap.put(kv.getKey(), Arrays.asList(value));
			}
		}
	}

	/**
	 * 根据输入的参数，关键值和扩展签名头列表 生成签名并返回最终的签名头列表
	 * @param paramsMap
	 * @param apiName
	 * @param version
	 * @param accessKey
	 * @param securityKey
	 * @param extSignHeaders 放在extSignHeaders里的kv都参与签名
	 * @return
	 */
	public static Map<String, String> newParamsMap(Map<String, List<String>> paramsMap, String apiName, String version,
			String accessKey, String securityKey, boolean timestampFlag, boolean nonceFlag,  Map<String, String> extSignHeaders) {
		return SignUtil.newParamsMap(paramsMap, apiName, version, accessKey, securityKey, timestampFlag, nonceFlag, extSignHeaders);
	}

	public static String trimUrl(String requestURL) {
		int pos = requestURL.indexOf("?");
		String ret = requestURL;

		if (pos >= 0) {
			ret = requestURL.substring(0, pos);
		}

		return ret;
	}

	public static void validateParams(String apiName, String accessKey, String securityKey, Map<String, String> paramsMap) throws HttpCallerException {
		if (apiName == null)
			throw new HttpCallerException(new InvalidParameterException("param apiName can not be null!"));

		if (accessKey != null && securityKey == null)
			throw new HttpCallerException(
					new InvalidParameterException("param securityKey can not be null for a given accessKey!"));

		if (paramsMap != null) {
			for (Entry<String,String> kv:paramsMap.entrySet()) {
				if (kv.getValue() == null) {
					throw new HttpCallerException(new InvalidParameterException(
							String.format("bad parasMap, the value for key [ %s ] is null, please remove the key or set its value, e.g. \"\"!", kv.getKey())));
				}
			}
		}

	}
	
	private static String decodeValue(String key, String value, boolean decodeFlag) throws HttpCallerException {
		if (decodeFlag) {
			if (value == null ) {
			  throw new HttpCallerException("bad params, the value for key {"+key+"} is null!");
			}
			return URLDecoder.decode(value);
		}
		
		return value;
	}

	/**
	 * Parse URL parameters to Map, url-decode all values
	 * @param requestURL
	 * @return
	 * @throws HttpCallerException
	 */
	public static Map<String, List<String>> parseUrlParamsMap(String requestURL, boolean decodeFlag) throws HttpCallerException {
		boolean questionMarkFlag = requestURL.contains("?");
		Map<String, List<String>> urlParamsMap = new HashMap<String, List<String>>();
		String key;
		String value;
		if (questionMarkFlag) {
			// parse params
			int pos = requestURL.indexOf("?");
			String paramStr = requestURL.substring(pos + 1);
			// requestURL = requestURL.substring(0, pos);
			// The caller needs to ensure the url-encode for a parameter value!!
			String[] params = paramStr.split("&");
			for (String param : params) {
				pos = param.indexOf("=");
				if (pos <= 0) {
					throw new HttpCallerException("bad request URL, url params error:" + requestURL);
				}
				key = decodeValue("", param.substring(0, pos), decodeFlag);
				value = param.substring(pos + 1);
				List<String> values = urlParamsMap.get(key);
				if (values == null) {
					values = new ArrayList<String>();
				}
				values.add(decodeValue(key, value, decodeFlag));
				urlParamsMap.put(key, values);
			}
		}

		return urlParamsMap;
	}
//
//	public static StringEntity jsonProcess(Map<String, String> params) {
//		JSONObject jsonParam = new JSONObject();
//		for (Entry<String, String> entry : params.entrySet())
//			jsonParam.put(entry.getKey(), entry.getValue());
//
//		StringEntity entity = new StringEntity(jsonParam.toString(), HTTP.UTF_8);// 解决中文乱码问题
//		entity.setContentEncoding(HTTP.UTF_8);
//		entity.setContentType("application/json");
//		return entity;
//	}

	private static void setHeaders(HttpPost httpPost, Map<String, String> newParamsMap) {
		if (newParamsMap != null) {
			for(Entry<String,String> kv: newParamsMap.entrySet())
			  httpPost.addHeader(kv.getKey(), kv.getValue());
		}
	}
	

	public static void setHeaders(HttpGet httpGet, Map<String, String> newParamsMap) {
		if (newParamsMap != null) {
			for(Entry<String,String> kv: newParamsMap.entrySet())
				httpGet.addHeader(kv.getKey(), kv.getValue());
		}
	}
	
	public static String genCurlHeaders(Map<String, String> newParamsMap) {
		if (newParamsMap != null) {
			StringBuffer sb = new StringBuffer();
			for(Entry<String,String> kv: newParamsMap.entrySet())
				sb.append("-H \"").append(kv.getKey()).append(":").append(kv.getValue()).append("\"  ");
			
			return sb.toString();
		}else
			return "";
	}
	
	public static String createPostCurlString(String url, Map<String, String> params, Map<String, String> headerParams, ContentBody cb, Map<String, String> directHheaderParamsMap) {
		StringBuffer sb = new StringBuffer("curl ");
		
		//透传的http headers
		sb.append(genCurlHeaders(directHheaderParamsMap));
		
		sb.append(genCurlHeaders(headerParams));
		
		if (params != null) {
			StringBuffer postSB = new StringBuffer();
			for(Entry<String,String> e:params.entrySet()){
				if(postSB.length()>0)
					postSB.append("&");
				postSB.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue()));
			}
			if (postSB.length() > 0) {
				sb.append(" -d \"");
				postSB.append("\"");
				sb.append(postSB.toString());
			} else {
				sb.append("--data ''");
			}
		} else  {
			// set params as
			//FIXME need this ??
			sb.append("--data '");
			sb.append(urlEncodedString(toNVP(params), HTTP.UTF_8));
			sb.append("'");
		}
		
		sb.append(" --insecure ");
		sb.append("\"");
		sb.append(url);
		sb.append("\"");
		return sb.toString();
	}
	
	private static String urlEncodedString(List<NameValuePair> parameters, String charset) {
		return URLEncodedUtils.format(parameters,
                charset != null ? charset : HTTP.DEF_CONTENT_CHARSET.name());
	}

	public static HttpPost createPost(final String url, Map<String, String> urlParams, Map<String, String> headerParams, ContentBody cb) {
		//set both cb and urlParams
		String newUrl = url;
		if (cb != null && urlParams != null)
		if (urlParams != null) {
			List<NameValuePair> nvps = toNVP(urlParams);
			String newParamStr = urlEncodedString(nvps, HTTP.UTF_8);
			if (!url.contains("?")) {
				newUrl =  String.format("%s?%s", url, newParamStr);
			} else {
				newUrl =  String.format("%s&%s", url, newParamStr);
			} 
		}
		printDebugInfo("new requestURL=" + newUrl);
		HttpPost httpost = new HttpPost(newUrl);
		setHeaders(httpost, headerParams);
		if (cb == null) {
			List<NameValuePair> nvps = toNVP(urlParams);
			try {
				httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else {
			if (cb.getContentType() == Type.JSON) {
				StringEntity entity = new StringEntity((String)cb.getContentBody(), HTTP.UTF_8);// 解决中文乱码问题
				entity.setContentType(Type.JSON.getContentType());
				httpost.setEntity(entity);
			}else {
				//binary
				httpost.setHeader(HTTP.CONTENT_TYPE, Type.BINARY.getContentType());  
		        ByteArrayEntity be = new ByteArrayEntity((byte[])cb.getContentBody());  
		        httpost.setEntity(be); 
			}
		}

		return httpost;
	}

	private static List<NameValuePair> toNVP(Map<String, String> urlParams) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();

		//fix NPE
		if (urlParams != null) {
			Set<String> keySet = urlParams.keySet();
			for (String key : keySet) {
				nvps.add(new BasicNameValuePair(key, urlParams.get(key)));
			}
		}
		return nvps;
	}

	public static void setDirectHeaders(HttpPost httpPost, Map<String, String> directHheaderParamsMap) {
		if (directHheaderParamsMap == null) {
			//do nothing
			return;
		} else {
			for(Entry<String,String> kv:directHheaderParamsMap.entrySet()) {
				if (kv.getKey() == null) {
					//log.info("ignore empty key");
				} else {
					if (HTTP.CONTENT_TYPE.equals(kv.getKey()) || !httpPost.containsHeader(kv.getKey())) {
					  // direct header has no chance to overwrite the normal headers, except it is the content-type
					  httpPost.addHeader(kv.getKey(), kv.getValue());
					}
				}
			}
		}
	}

	public static String getUrlPathInfo(String url) throws HttpCallerException{
		URL urlStr = null;
		try {
			urlStr = new URL(url);
		}catch (Exception e){
			throw new HttpCallerException("url is unformat, url is " + url);
		}
		String path = urlStr.getPath();
		return path;
	}

}
