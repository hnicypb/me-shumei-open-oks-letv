package me.shumei.open.oks.letv;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
	String resultFlag = "false";
	String resultStr = "未知错误！";
	
	/**
	 * <p><b>程序的签到入口</b></p>
	 * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
	 * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
	 * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
	 * @param cfg “配置”栏内输入的数据
	 * @param user 用户名
	 * @param pwd 解密后的明文密码
	 * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
	 */
	public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
		//把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
		CaptchaUtil.context = ctx;
		//标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
		CaptchaUtil.isAutoSign = isAutoSign;
		
		try{
			//存放Cookies的HashMap
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Jsoup的Response
			Response res;
			
			String loginUrl = "http://sso.letv.com/";
			String checkLoginUrl = "http://sso.letv.com/user/loginmini";
			String signinUrl = "http://api.my.letv.com/user/usersign";
			String captchaUrl = null;
			
			//设置登录需要提交的数据
			HashMap<String, String> postDatas = new HashMap<String, String>();
			postDatas.put("loginname", user);
			postDatas.put("password", pwd);
			postDatas.put("memberme", "true");
			postDatas.put("referrer", loginUrl);
			
			//检测账号信息
			//{"flag":"200","username":"letv_512480d97efa000"}
			res = Jsoup.connect(checkLoginUrl).data(postDatas).userAgent(UA_CHROME).timeout(TIME_OUT).referrer(checkLoginUrl).ignoreContentType(true).method(Method.POST).execute();
			cookies.putAll(res.cookies());
			System.out.println(res.body());
			JSONObject jsonObj = new JSONObject(res.body());
			String flag = jsonObj.getString("flag");
			if(flag.equals("401"))
			{
				this.resultFlag = "false";
				this.resultStr = "帐号或密码不正确";
			}
			else if(flag.equals("402"))
			{
				this.resultFlag = "false";
				this.resultStr = "用户已被管理员屏蔽了!";
			}
			else if(flag.equals("403"))
			{
				this.resultFlag = "false";
				this.resultStr = "邮箱未激活";
			}
			else if(flag.equals("200"))
			{
				//开始登录
				//跨域登录
				String tmpurl = "http://sso.letv.com/user/sys?memberme=true";
				res = Jsoup.connect(tmpurl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
				cookies.putAll(res.cookies());
				/*2013-4-7 22:10:06
				//兼容老passport
				String url = "http://passport.letv.com/cas/loginCheck.do?username=" + user + "&password=" + URLEncoder.encode(pwd, "UTF-8") + "&service=my&rememberMe=true&param=&jsonCallback=?";
				res = Jsoup.connect(url).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
				cookies.putAll(res.cookies());
				*/

				//提交签到信息
				res = Jsoup.connect(signinUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
				System.out.println(res.body());
				int result = new JSONObject(res.body()).getInt("result");
				if(result == -1)
				{
					this.resultFlag = "false";
					this.resultStr = "签到失败";
				}
				else
				{
					this.resultFlag = "true";
					this.resultStr = "签到成功，连续签到" + result + "天";
				}
			}
			else
			{
				this.resultFlag = "false";
				this.resultStr = "服务器返回未知登录错误，请检查网络是否畅通";
			}
			
			
		} catch (IOException e) {
			this.resultFlag = "false";
			this.resultStr = "连接超时";
			e.printStackTrace();
		} catch (Exception e) {
			this.resultFlag = "false";
			this.resultStr = "未知错误！";
			e.printStackTrace();
		}
		
		return new String[]{resultFlag, resultStr};
	}
	
	
}
