package jp.redmine.redmineclient.form.helper;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.redmine.redmineclient.activity.handler.WebviewActionInterface;

public class WebViewHelper {
	private WebviewActionInterface action;
	private Pattern patternIntent = Pattern.compile(TextileHelper.URL_PREFIX);
	public void setup(WebView view){
		setupWebView(view);
		setupHandler(view);
	}

	protected void setupWebView(WebView view){
		view.getSettings().setPluginState(WebSettings.PluginState.OFF);
		view.getSettings().setBlockNetworkLoads(true);
	}
	public void setAction(WebviewActionInterface act){
		action = act;
	}

	protected void setupHandler(WebView view){
		view.setWebViewClient(new WebViewClient(){

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Matcher m = patternIntent.matcher(url);
				if(m.find()){
					return  TextileHelper.kickAction(action, m.replaceAll(""));
				} else if (action != null) {
					return action.url(url);
				} else {
					return super.shouldOverrideUrlLoading(view, url);
				}
			}
		});
	}

	public void setContent(WebView view, final int connectionid, final long project, final String text){
		view.loadDataWithBaseURL("", TextileHelper.getHtml(view.getContext(),TextileHelper.getHtml(connectionid,project,text),""), "text/html", "UTF-8", "");
	}

	public void setContent(WebView view, String text){
		String inner = TextileHelper.convertTextileToHtml(text, null);
		view.loadDataWithBaseURL("", TextileHelper.getHtml(view.getContext(),inner,""), "text/html", "UTF-8", "");
	}
}
