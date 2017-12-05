package com.volley.demo.volley;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.volley.demo.LogAgentUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * http 请求volley支持库，目前只提供了http的get与post请求
 */
public class VolleyAgent {
    private final String TAG = "VolleyAgent";
    // 上下文对象
    private Context mContext = null;
    // 请求队列
    private RequestQueue mQueue = null;


    /**
     * Callback interface for delivering parsed responses.
     */
    public interface VolleyRequestListener<T> {
        /**
         * Called when a response is received.
         */
        public void onResponse(T response, String text);

        public void onErrorResponse(VolleyError error);
    }


    //#######################单例begin##########################
    //
    private volatile static VolleyAgent instance;

    // 构造方法
    private VolleyAgent(Context context) {
        mContext = context;
        mQueue = Volley.newRequestQueue(mContext);
    }

    //
    public static VolleyAgent getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (instance == null) {
            synchronized (VolleyAgent.class) {
                if (instance == null) {
                    instance = new VolleyAgent(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    //############################单例end#############################


    // 超时时间为30s钟
    private final int TIME_OUT = 30000;
    // 重试次数
    private final int RETRY_COUNT = 1;


    //###############################对外暴露的接口begin#################################


    /**
     * 同步get
     *
     * @param url
     * @param params
     * @param headers
     */
    public <T> T get_Sync(String url,
                          Map<String, String> headers,
                          Map<String, String> params, final Class<T> cls) throws InterruptedException, ExecutionException {
        LogAgentUtils.d(TAG, "---get_Async---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // 组合get请求url
        if (params != null && params.size() > 0) {
            url = url + "?" + map2QueryString(params);
        }
        LogAgentUtils.d(TAG, "get url: " + url);
        // get请求
        return this._get_Sync(url, headers, cls);
    }


    /**
     * 对外暴露的get请求，将gson数据解析放到了异步线程中
     *
     * @param url
     * @param params
     * @param headers
     * @param cls
     * @param callback
     * @param <T>
     */
    public <T> void get(String url,
                        Map<String, String> headers,
                        Map<String, String> params,
                        final Class<T> cls,
                        final VolleyRequestListener<T> callback) {
        LogAgentUtils.d(TAG, "---get---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // 组合get请求url
        if (params != null && params.size() > 0) {
            url = url + "?" + map2QueryString(params);
        }
        LogAgentUtils.d(TAG, "get url: " + url);
        // get请求
        this._get(url, headers, cls, callback);
    }


    /**
     * 同步Post
     *
     * @param url
     * @param params
     * @param cls
     * @param <T>
     */
    public <T> T post_Sync(final String url,
                           final Map<String, String> headers,
                           final Map<String, String> params,
                           final Class<T> cls) throws InterruptedException, ExecutionException {
        LogAgentUtils.d(TAG, "---post_Sync---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // post请求
        return this._anyHttp_Sync(Request.Method.POST, url, headers, params, cls);
    }


    /**
     * 对外暴露的post请求，将gson数据解析放到了异步线程中
     *
     * @param url
     * @param params
     * @param cls
     * @param callback
     * @param <T>
     */
    public <T> void post(final String url,
                         final Map<String, String> headers,
                         final Map<String, String> params,
                         final Class<T> cls,
                         final VolleyRequestListener<T> callback) {
        LogAgentUtils.d(TAG, "---post---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // post请求
        this._anyHttp(Request.Method.POST, url, headers, params, cls, callback);
    }

    /**
     * post json数据
     *
     * @param url
     * @param body
     * @param cls
     * @param callback
     * @param <T>
     */
    public <T> void post(final String url,
                         final Map<String, String> headers,
                         final String body,
                         final Class<T> cls,
                         final VolleyRequestListener<T> callback) {
        LogAgentUtils.d(TAG, "---post---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "body: " + body);
        // post请求
        this._anyHttp(Request.Method.POST, url, headers, body, cls, callback);
    }


    /**
     * 同步put
     *
     * @param url
     * @param params
     * @param cls
     * @param <T>
     */
    public <T> T put_Sync(final String url,
                          final Map<String, String> headers,
                          final Map<String, String> params,
                          final Class<T> cls) throws InterruptedException, ExecutionException {
        LogAgentUtils.d(TAG, "---put_Sync---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // post请求
        return this._anyHttp_Sync(Request.Method.PUT, url, headers, params, cls);
    }

    /**
     * 异步put
     *
     * @param url
     * @param params
     * @param cls
     * @param callback
     * @param <T>
     */
    public <T> void put(final String url,
                        final Map<String, String> headers,
                        final Map<String, String> params,
                        final Class<T> cls,
                        final VolleyRequestListener<T> callback) {
        LogAgentUtils.d(TAG, "---put---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // post请求
        this._anyHttp(Request.Method.PUT, url, headers, params, cls, callback);
    }


    /**
     * 同步put
     *
     * @param url
     * @param params
     * @param cls
     * @param <T>
     */
    public <T> T delete_Sync(final String url,
                          final Map<String, String> headers,
                          final Map<String, String> params,
                          final Class<T> cls) throws InterruptedException, ExecutionException {
        LogAgentUtils.d(TAG, "---put_Sync---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // post请求
        return this._anyHttp_Sync(Request.Method.DELETE, url, headers, params, cls);
    }

    /**
     * delete
     *
     * @param url
     * @param params
     * @param cls
     * @param callback
     * @param <T>
     */
    public <T> void delete(final String url,
                           final Map<String, String> headers,
                           final Map<String, String> params,
                           final Class<T> cls,
                           final VolleyRequestListener<T> callback) {
        LogAgentUtils.d(TAG, "---delete---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        // post请求
        this._anyHttp(Request.Method.DELETE, url, headers, params, cls, callback);
    }


    //#########################################网络配置相关begin#######################################

    /**
     * 同步get请求
     *
     * @param url
     * @return
     */
    private <T> T _get_Sync(final String url, final Map<String, String> headers, final Class<T> cls) throws InterruptedException, ExecutionException {
        LogAgentUtils.d(TAG, "---_get_Sync---");
        //------------
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(url, future, future) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // header
                if (headers == null || headers.size() == 0) {
                    return getVolleyHeaders();
                }
                return headers;
            }
        };
        request.setRetryPolicy(getVolleyRetryPolicy());
        mQueue.add(request);
        //--------------同步耗时请求-------------
        String response = future.get(); // this will block
        LogAgentUtils.d(TAG, "response: " + response);
        //--------------解析数据-------------
        if (TextUtils.isEmpty(response) == false) {
            if (cls.isAssignableFrom(String.class)) {
                Log.e(TAG, "---String---");
                return (T) response;
            } else {
                Log.e(TAG, "---Gson---");
                Gson gson = new Gson();
                Object bean = gson.fromJson(response, cls);
                if (bean != null) {
                    return (T) bean;
                } else {
                    throw new ExecutionException(new ParseError());
                }
            }
        } else {
            throw new ExecutionException("response is null", new ParseError());
        }
    }


    /**
     * 在异步线程中完成json数据解析
     *
     * @param url
     * @param headers
     * @param cls
     * @param callback
     * @param <T>
     */

    private <T> void _get(final String url, final Map<String, String> headers, final Class<T> cls,
                          final VolleyRequestListener<T> callback) {

        LogAgentUtils.d(TAG, "---Gson---");
        //
        VolleyGsonRequest.Listener<T> responseListener = new VolleyGsonRequest.Listener<T>() {

            @Override
            public void onResponse(T response, String text) {
                LogAgentUtils.d(TAG, "---onResponse---");
                LogAgentUtils.d(TAG, "url: " + url);
                LogAgentUtils.d(TAG, "data: " + text);
                if (callback != null) {
                    callback.onResponse(response, text);
                }
            }
        };
        //
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LogAgentUtils.d(TAG, "---onErrorResponse---");
                LogAgentUtils.d(TAG, error.getMessage());
                if (callback != null) {
                    callback.onErrorResponse(error);
                }
            }
        };
        //
        VolleyGsonRequest request = new VolleyGsonRequest(Request.Method.GET, url,
                responseListener, errorListener, cls) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // header
                if (headers == null || headers.size() == 0) {
                    return getVolleyHeaders();
                }
                return headers;
            }
        };
        //
        request.setRetryPolicy(getVolleyRetryPolicy());
        mQueue.add(request);

    }


    /**
     * POST = 1;PUT = 2;DELETE = 3;HEAD = 4;OPTIONS = 5;TRACE = 6;PATCH = 7;
     *
     * @param requestMethod requestMethod 接口请求方式 例如Request.Method.POST 默认为post(POST = 1;PUT = 2;DELETE = 3;HEAD = 4;OPTIONS = 5;TRACE = 6;PATCH = 7;)
     * @param url
     * @param headers
     * @param params
     * @param cls
     * @param <T>
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private <T> T _anyHttp_Sync(int requestMethod, final String url, final Map<String, String> headers, final Map<String, String> params, final Class<T> cls) throws InterruptedException, ExecutionException {

        //-----------------
        if (requestMethod < Request.Method.POST || requestMethod > Request.Method.PATCH) {
            requestMethod = Request.Method.POST;
        }
        //----------------
        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(requestMethod, url, future, future) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // header
                if (headers == null || headers.size() == 0) {
                    return getVolleyHeaders();
                }
                return headers;
            }
        };
        request.setRetryPolicy(getVolleyRetryPolicy());
        mQueue.add(request);
        //--------------同步耗时请求-------------
        String response = future.get(); // this will block
        LogAgentUtils.d(TAG, "response: " + response);
        //--------------解析数据-------------
        if (TextUtils.isEmpty(response) == false) {
            if (cls.isAssignableFrom(String.class)) {
                Log.e(TAG, "---String---");
                return (T) response;
            } else {
                Log.e(TAG, "---Gson---");
                Gson gson = new Gson();
                Object bean = gson.fromJson(response, cls);
                if (bean != null) {
                    return (T) bean;
                } else {
                    throw new ExecutionException(new ParseError());
                }
            }
        } else {
            throw new ExecutionException("response is null", new ParseError());
        }
    }


    /**
     * POST = 1;PUT = 2;DELETE = 3;HEAD = 4;OPTIONS = 5;TRACE = 6;PATCH = 7;
     *
     * @param requestMethod 接口请求方式 例如Request.Method.POST 默认为post(POST = 1;PUT = 2;DELETE = 3;HEAD = 4;OPTIONS = 5;TRACE = 6;PATCH = 7;)
     * @param url
     * @param headers
     * @param params
     * @param cls
     * @param callback
     * @param <T>
     */
    private <T> void _anyHttp(int requestMethod, final String url, final Map<String, String> headers, final Map<String, String> params, final Class<T> cls, final VolleyRequestListener<T> callback) {
        //
        if (requestMethod < Request.Method.POST || requestMethod > Request.Method.PATCH) {
            requestMethod = Request.Method.POST;
        }


        VolleyGsonRequest.Listener<T> responseListener = new VolleyGsonRequest.Listener<T>() {

            @Override
            public void onResponse(T response, String text) {
                LogAgentUtils.d(TAG, "---onResponse---");
                LogAgentUtils.d(TAG, "url: " + url);
                LogAgentUtils.d(TAG, "data: " + text);

                if (callback != null) {
                    callback.onResponse(response, text);
                }
            }
        };
        //
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LogAgentUtils.d(TAG, "---onErrorResponse---");
                LogAgentUtils.d(TAG, error.getMessage());
                if (callback != null) {
                    callback.onErrorResponse(error);
                }
            }
        };

        //
        VolleyGsonRequest request = new VolleyGsonRequest(requestMethod, url, responseListener, errorListener, cls) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // header
                if (headers == null || headers.size() == 0) {
                    return getVolleyHeaders();
                }
                return headers;
            }
        };
        request.setRetryPolicy(getVolleyRetryPolicy());
        mQueue.add(request);
    }


    /**
     * POST = 1;PUT = 2;DELETE = 3;HEAD = 4;OPTIONS = 5;TRACE = 6;PATCH = 7;
     *
     * @param requestMethod 接口请求方式 例如Request.Method.POST 默认为post(POST = 1;PUT = 2;DELETE = 3;HEAD = 4;OPTIONS = 5;TRACE = 6;PATCH = 7;)
     * @param url
     * @param headers
     * @param body
     * @param cls
     * @param callback
     * @param <T>
     */
    private <T> void _anyHttp(int requestMethod, final String url, final Map<String, String> headers, final String body, final Class<T> cls, final VolleyRequestListener<T> callback) {
        //
        if (requestMethod < Request.Method.POST || requestMethod > Request.Method.PATCH) {
            requestMethod = Request.Method.POST;
        }
        LogAgentUtils.d(TAG, "---Gson---");
        VolleyGsonRequest.Listener<T> responseListener = new VolleyGsonRequest.Listener<T>() {

            @Override
            public void onResponse(T response, String text) {
                LogAgentUtils.d(TAG, "---onResponse---");
                LogAgentUtils.d(TAG, "url: " + url);
                LogAgentUtils.d(TAG, "data: " + text);

                if (callback != null) {
                    callback.onResponse(response, text);
                }
            }
        };
        //
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LogAgentUtils.d(TAG, "---onErrorResponse---");
                LogAgentUtils.d(TAG, error.getMessage());
                if (callback != null) {
                    callback.onErrorResponse(error);
                }
            }
        };
        //
        VolleyGsonRequest request = new VolleyGsonRequest(requestMethod, url, responseListener, errorListener, cls) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return body.getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // header
                if (headers == null || headers.size() == 0) {
                    return getVolleyHeaders();
                }
                return headers;
            }
        };
        request.setRetryPolicy(getVolleyRetryPolicy());
        mQueue.add(request);
    }


    //------------------------超时相关代码---------------------------
    private RetryPolicy getVolleyRetryPolicy() {
        return new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return TIME_OUT;
            }

            @Override
            public int getCurrentRetryCount() {
                return RETRY_COUNT;
            }

            @Override
            public void retry(VolleyError volleyError) throws VolleyError {
                volleyError.printStackTrace();
            }
        };
    }

    //-----------------------------------

    /**
     * 生成QueryString,以 a=1&b=2形式返回
     */
    private static String map2QueryString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        String value;
        try {
            if (map != null && map.size() > 0) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    value = "";
                    value = entry.getValue();
                    if (TextUtils.isEmpty(value)) {
                        value = "";
                    } else {
                        value = URLEncoder.encode(value, "utf-8");
                    }
                    sb.append(entry.getKey()).append("=").append(value)
                            .append("&");
                }
                sb.deleteCharAt(sb.length() - 1);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    //---------------------------默认header配置-----------------------------
    // 默认header配置(如果用户不传header的话)
    private static HashMap<String, String> getVolleyHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        try {
            String userAgent = System.getProperty("http.agent");
            headers.put("User-Agent", TextUtils.isEmpty(userAgent) ? "Android HTTP Client 1.0" : userAgent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return headers;
    }

    /**
     * post json数据时，必须使用的header配置
     *
     * @return
     */
    private static HashMap<String, String> getPostJsonHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();

        try {
            // json
            headers.put("Charset", "UTF-8");
            headers.put("Content-Type", "application/x-javascript");
            headers.put("Accept-Encoding", "gzip,deflate");
            //
            String userAgent = System.getProperty("http.agent");
            headers.put("User-Agent", TextUtils.isEmpty(userAgent) ? "Android HTTP Client 1.0" : userAgent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return headers;
    }

}
