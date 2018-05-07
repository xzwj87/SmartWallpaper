package com.samsung.app.smartwallpaper.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samsung.app.smartwallpaper.ASRDialog;
import com.samsung.app.smartwallpaper.AppContext;
import com.samsung.app.smartwallpaper.command.Command;
import com.samsung.app.smartwallpaper.command.CommandExecutor;
import com.samsung.app.smartwallpaper.command.RuleId;
import com.samsung.app.smartwallpaper.config.UrlConstant;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static com.samsung.app.smartwallpaper.config.UrlConstant.GET_WALLPAPER_FILE_PATH_URL;
import static com.samsung.app.smartwallpaper.config.UrlConstant.VOTEUP_WALLPAPER_URL;

public class ApiClient {
	private static final String TAG = "ApiClient";

	public static final String UTF_8 = "UTF-8";
	
	private final static int TIMEOUT_CONNECTION = 20000;
	private final static int TIMEOUT_SOCKET = 20000;

	private static String appUserAgent;
	
	private static String getUserAgent(AppContext appContext) {
		if(appUserAgent == null || appUserAgent == "") {
			StringBuilder ua = new StringBuilder("SmartWallpaper");
			ua.append('/'+appContext.getPackageInfo().versionName+'_'+appContext.getPackageInfo().versionCode);//App版本
			ua.append("/Android");//手机系统平台
			ua.append("/"+android.os.Build.VERSION.RELEASE);//手机系统版本
			ua.append("/"+android.os.Build.MODEL); //手机型号
			ua.append("/"+appContext.getAppId());//客户端唯一标识
			appUserAgent = ua.toString();
		}
		return appUserAgent;
	}
	
	private static HttpClient getHttpClient() {        
        HttpClient httpClient = new HttpClient();
		// 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        // 设置 默认的超时重试处理策略
		httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		// 设置 连接超时时间
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(TIMEOUT_CONNECTION);
		// 设置 读数据超时时间 
		httpClient.getHttpConnectionManager().getParams().setSoTimeout(TIMEOUT_SOCKET);
		// 设置 字符集
		httpClient.getParams().setContentCharset(UTF_8);
		return httpClient;
	}	
	
	private static GetMethod getHttpGet(String url, String cookie, String userAgent) {
		GetMethod httpGet = new GetMethod(url);
		// 设置 请求超时时间
		httpGet.getParams().setSoTimeout(TIMEOUT_SOCKET);
		httpGet.setRequestHeader("Host", UrlConstant.HOST);
		httpGet.setRequestHeader("Connection","Keep-Alive");
		httpGet.setRequestHeader("Cookie", cookie);
		httpGet.setRequestHeader("User-Agent", userAgent);
		return httpGet;
	}
	
	private static PostMethod getHttpPost(String url, String cookie, String userAgent) {
		PostMethod httpPost = new PostMethod(url);
		// 设置 请求超时时间
		httpPost.getParams().setSoTimeout(TIMEOUT_SOCKET);
		httpPost.setRequestHeader("Host", UrlConstant.HOST);
		httpPost.setRequestHeader("Connection","Keep-Alive");
		httpPost.setRequestHeader("Cookie", cookie);
		httpPost.setRequestHeader("User-Agent", userAgent);
		return httpPost;
	}

	public static JSONObject request_get(String url){
		Log.i(TAG, "request_get-url="+url);
		JSONObject jsonObj = null;
		try {
			URL api_url = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) api_url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoOutput(false);
			conn.setDoInput(true);
			conn.setUseCaches(true);
			conn.setInstanceFollowRedirects(true);
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(30000);
			conn.connect();

			int code = conn.getResponseCode();
			StringBuffer sb = new StringBuffer();
			if (code == 200) { // 正常响应
				// 从流中读取响应信息
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) { // 循环从流中读取
					sb.append(line);
					sb.append("\r\n");
				}
				reader.close(); // 关闭流

				String result = sb.toString();//返回低字符串
				jsonObj = new JSONObject(result);
			}
			// 6. 断开连接，释放资源
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObj;
	}

	public static JSONObject request_post(String url, HashMap<String, Object> params) {
		Log.i(TAG, "request_post-request url="+url);
		JSONObject jsonResult = null;
		URL realUrl = null;
		InputStream in = null;
		HttpURLConnection conn = null;
		StringBuffer sb = new StringBuffer();
		JSONObject jsonObj = new JSONObject(params);
		String stringParams = jsonObj.toString();

		Log.i(TAG,"request_post-request params="+ stringParams);
		//发送请求
		try{
			realUrl = new URL(url);
			conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(30000);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
			conn.setRequestProperty("accept","application/json");


			if (!TextUtils.isEmpty(stringParams)) {
				byte[] writebytes = stringParams.getBytes();
				// 设置文件长度
				conn.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
				OutputStream outStream = conn.getOutputStream();
				outStream.write(writebytes);
				outStream.flush();
				outStream.close();
			}

//			PrintWriter pw = new PrintWriter(conn.getOutputStream());
//			pw.print(stringParams);
//			pw.flush();
//			pw.close();

			int code = conn.getResponseCode();
			if (code == 200) {
				in = conn.getInputStream();//获得返回的输入流
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				StringBuffer buffer = new StringBuffer();
				String line = "";
				while ((line = br.readLine()) != null) {
					buffer.append(line);
				}
				br.close();

				String result = buffer.toString();//返回低字符串
				Log.i(TAG,"request_post-response result="+ result);

				jsonResult = new JSONObject(result);
			}
			conn.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}
		return jsonResult;
	}

	private static AsyncTask<String, Void, Command> mRequestTask = null;
	public synchronized static void requestTS(String utt){
		Log.i(TAG, "requestTS-utt="+utt);
		if(mRequestTask !=null){
			mRequestTask.cancel(true);
			mRequestTask = null;
		}

		mRequestTask = new AsyncTask<String, Void, Command>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}

			@Override
			protected Command doInBackground(String... params) {
				String utteranceRaw = params[0];
				if(TextUtils.isEmpty(utteranceRaw)){
					return null;
				}
				HashMap<String, Object> paramMap = new HashMap<>();
				paramMap.put("model", "tagger");
				paramMap.put("context", "pseudo_root");
				paramMap.put("hashcode","01cd8cce69b5315d787baeda98cb6911");
				paramMap.put("utteranceRaw",utteranceRaw);

				JSONObject jsonObject = ApiClient.request_post(UrlConstant.HOST_URL_TS, paramMap);
				HashMap<String, String> parameters = new HashMap<>();
				ArrayList<String> hashcodeList = new ArrayList<>();
				ArrayList<Integer> voteUpCntList = new ArrayList<>();
				String ruleId = null;
				String action = null;
				String uttSeg = null;
				int resultcode;
				try {
					JSONObject paramsJsonObj = jsonObject.getJSONObject("parameters");
					Iterator<String> iterator = paramsJsonObj.keys();
					while (iterator.hasNext()){
						String key = iterator.next();
						String value = paramsJsonObj.getString(key);
						parameters.put(key, value);
					}

					JSONArray hashcodeArray = jsonObject.getJSONArray("hashcodelist");
					for(int i=0;i<hashcodeArray.length();i++){
						hashcodeList.add(hashcodeArray.getString(i));
					}
					JSONArray voteUpCntArray = jsonObject.getJSONArray("voteupcnt_list");
					for(int i=0;i<voteUpCntArray.length();i++){
						voteUpCntList.add(voteUpCntArray.getInt(i));
					}

					ruleId = jsonObject.getString("ruleid");
					action = jsonObject.getString("action");
					uttSeg = jsonObject.getString("utt_seg");
					resultcode = jsonObject.getInt("resultcode");

//					Log.i(TAG, "parameters="+parameters.toString());
//					Log.i(TAG, "ruleId="+ruleId);
//					Log.i(TAG, "action="+action);
//					Log.i(TAG, "hashcodeList="+hashcodeList.toString());
//					Log.i(TAG, "resultcode="+resultcode);

					Command cmd = new Command();
					cmd.setRuleId(ruleId);
					cmd.setAction(action);
					cmd.setUttSeg(uttSeg);
					cmd.setHashCodeList(hashcodeList);
					cmd.setVoteUpCntList(voteUpCntList);
					cmd.setParams(parameters);
					cmd.setResultCode(resultcode);

					if(RuleId.RULE_ID_1.equals(cmd.getRuleId())){
						HashMap<String, Boolean> extra = new HashMap<>();
						extra.put("isTagMatched", jsonObject.getBoolean("isTagMatched"));
						extra.put("isTagPartialMatched", jsonObject.getBoolean("isTagPartialMatched"));
						cmd.setExtra(extra);
					}
					return cmd;
				}catch (Exception e){
					Log.e(TAG, "error="+e.toString());
				}
				return null;
			}

			@Override
			protected void onPostExecute(Command cmd) {
				super.onPostExecute(cmd);
				boolean handled = false;
				if(cmd != null) {
					Toast.makeText(AppContext.appContext, "Command:"+ cmd.toString(), Toast.LENGTH_SHORT).show();
					handled = CommandExecutor.getInstance(AppContext.appContext).execute(cmd);
				}else{
					Toast.makeText(AppContext.appContext, "Command is null", Toast.LENGTH_SHORT).show();
					handled =false;
				}
				if(ASRDialog.getASRDialogInstance() != null){
					ASRDialog.getASRDialogInstance().onCommandFinish(handled);
				}
			}
		};
		mRequestTask.executeOnExecutor(THREAD_POOL_EXECUTOR, utt);
	}

	//手动点赞
	public synchronized static boolean voteUpWallpaper(String hashcode){
		Log.i(TAG, "voteUpWallpaper-hashcode="+hashcode);
		if(TextUtils.isEmpty(hashcode)){
			return false;
		}
		String api_url = VOTEUP_WALLPAPER_URL + hashcode;
		try {
			JSONObject jsonObject = ApiClient.request_get(api_url);
			String errno = jsonObject.getString("errno");
			if("0".equals(errno)){
				return true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	//根据URL获取图片
	public static Bitmap getBitmapByUrl(String imageUrl){
		Log.d(TAG, "getBitmapByUrl-imageUrl="+imageUrl);
		try{
			URL url = new URL(imageUrl);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setRequestMethod("GET");
			if(conn.getResponseCode() == 200){
				InputStream is = conn.getInputStream();
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				return bitmap;
			}
		}catch (Exception e) {
			Log.d(TAG, "getBitmapByUrl-error="+e.toString());
			e.printStackTrace();
		}
		return null;
	}
	//根据hashcode获取图片
	public static Bitmap getBitmapByHashCode(String hashcode){
		Log.d(TAG, "getBitmapByHashCode-hashcode="+hashcode);
		String api_url = UrlConstant.DOWNLOAD_WALLPAPER_URL + hashcode;
		try{
			URL url = new URL(api_url);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setRequestMethod("GET");
			if(conn.getResponseCode() == 200){
				InputStream is = conn.getInputStream();
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				return bitmap;
			}
		}catch (Exception e) {
			Log.e(TAG, "getBitmapByUrl-error="+e.toString());
			e.printStackTrace();
		}
		return null;
	}
	//根据hashcode获取图片的路径
	public static String getWallpaperFilePathByHashCode(String hashcode){
		Log.d(TAG, "getWallpaperFIlePathByHashCode-hashcode="+hashcode);
		String api_url = GET_WALLPAPER_FILE_PATH_URL + hashcode;
		int errno;
		String errmsg = null;
		try{
			JSONObject jsonObj = ApiClient.request_get(api_url);
			errno = jsonObj.getInt("errno");
			errmsg = jsonObj.getString("errmsg");
			if(errno == 0) {
				String file_path = jsonObj.getString("file_path");
				String file_name = jsonObj.getString("file_name");
				return file_path + File.separator + file_name;
			}
		}catch (Exception e) {
			Log.e(TAG, "getWallpaperFIlePathByHashCode-error="+e.toString());
			e.printStackTrace();
		}
		Log.e(TAG, "getWallpaperFIlePathByHashCode-errmsg="+errmsg);
		return null;
	}

}

