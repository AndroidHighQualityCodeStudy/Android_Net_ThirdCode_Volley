package com.volley.demo.volley;


import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;

/**
 * 我们的数据解析一直是放在主线程的，这样是非常不好的。
 * 添加这个GsonRequest把数据解析搞到异步线程
 * <p/>
 * Created by xiaxveliang on 2016/10/24.
 */
public class VolleyGsonRequest<T> extends Request<T> {
    private static final String TAG = "GsonRequest";

    private Listener<T> listener;
    //
    private Class<T> cls;
    //
    private String mJsonStr;

    /**
     * Creates a new request with the given method.
     *
     * @param method        the request {@link Method} to use
     * @param url           URL to fetch the string at
     * @param listener      Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public VolleyGsonRequest(int method, String url, Listener<T> listener,
                             Response.ErrorListener errorListener, final Class<T> cls) {
        super(method, url, errorListener);
        this.listener = listener;
        this.cls = cls;
    }

    /**
     * Creates a new GET request.
     *
     * @param url           URL to fetch the string at
     * @param listener      Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public VolleyGsonRequest(String url, Listener<T> listener, Response.ErrorListener errorListener, final Class<T> cls) {
        this(Method.GET, url, listener, errorListener, cls);
    }

    @Override
    protected void onFinish() {
        super.onFinish();
        this.listener = null;
    }

    @Override
    protected void deliverResponse(T response) {
        if (listener != null) {
            listener.onResponse(response, mJsonStr);
        }
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        //
        try {
            //
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Log.d(TAG, "UI thread parse");
            } else {
                Log.d(TAG, "thread parse");
            }
            try {
                mJsonStr = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            } catch (UnsupportedEncodingException e) {
                mJsonStr = new String(response.data);
            }
            // 数据不为null
            if (TextUtils.isEmpty(mJsonStr) == false) {
                if (cls.isAssignableFrom(String.class)) {
                    Log.d(TAG, "---String---");
                    return Response.success((T) mJsonStr, HttpHeaderParser.parseCacheHeaders(response));
                } else {
                    Log.d(TAG, "---Gson---");
                    Object bean = getFromStr(cls, mJsonStr);
                    if (bean != null) {
                        return Response.success((T) bean, HttpHeaderParser.parseCacheHeaders(response));
                    } else {
                        return Response.error(new ParseError());
                    }
                }
            } else {
                // 数据为null
                return Response.error(new ParseError());
            }
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }


    /**
     * Callback interface for delivering parsed responses.
     */
    public interface Listener<T> {
        /**
         * Called when a response is received.
         */
        public void onResponse(T response, String text);
    }


    //------------------------解析数据-----------------------------
    public static <T> T getFromStr(Class<T> cls, String str) {

        if (TextUtils.isEmpty(str) || cls == null) {
            return null;
        }
        try {
            Gson gson = new Gson();
            Object bean = gson.fromJson(str, cls);
            if (bean != null) {
                return (T) bean;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


}