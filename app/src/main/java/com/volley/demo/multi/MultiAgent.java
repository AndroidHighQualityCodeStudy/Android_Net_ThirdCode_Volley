package com.volley.demo.multi;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.ParseError;
import com.google.gson.Gson;
import com.volley.demo.LogAgentUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 文件上传(提供的为同步上传的方法)
 * <p>
 * Created by xueliangxia on 2017/2/8.
 */
public class MultiAgent {
    private static final String TAG = "MultiAgent";

    /**
     * 发送POST请求
     *
     * @param url
     * @param headers
     * @param params
     * @param files
     * @return
     * @throws Exception
     */
    public static <T> T postFile_Sync(String url, Map<String, String> headers, Map<String, String> params, Map<String, File> files, final Class<T> cls) throws InterruptedException, ExecutionException {
        LogAgentUtils.d(TAG, "---postFile_Sync---");
        LogAgentUtils.d(TAG, "url: " + url);
        LogAgentUtils.d(TAG, "headers: " + headers);
        LogAgentUtils.d(TAG, "params: " + params);
        LogAgentUtils.d(TAG, "files: " + files);
        //
        return _postFile_Sync(url, headers, params, files, cls);
    }

    /**
     * 解析数据
     *
     * @param url
     * @param headers
     * @param params
     * @param files
     * @param cls
     * @param <T>
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static <T> T _postFile_Sync(String url, Map<String, String> headers, Map<String, String> params, Map<String, File> files, final Class<T> cls) throws InterruptedException, ExecutionException {

        try {
            //--------------同步耗时请求-------------
            String response = _postFile_Sync(url, headers, params, files);// this will block
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
                        throw new ExecutionException("ParseError",new ParseError());
                    }
                }
            } else {
                throw new ExecutionException("response is null", new ParseError());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new InterruptedException(e.getMessage());
        }
    }


    /**
     * 发送POST请求
     */
    private static String _postFile_Sync(String postUrl, Map<String, String> headers, Map<String, String> params, Map<String, File> files) throws Exception {
        // 生成HTTP协议中的边界字符串,用于分隔多个文件、表单项。
        final String multipartBoundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        final String LINE_FEED = "\r\n";
        final String fileFieldName = "file";
        //
        HttpURLConnection connection = null;
        DataOutputStream dataOutStream = null;
        try {
            //------URL------
            URL url = new URL(postUrl);
            //-----HttpURLConnection----
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + multipartBoundary);

            // -----向HTTP请求添加头信息----
            if (headers != null && headers.isEmpty() == false) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            //
            dataOutStream = new DataOutputStream(connection.getOutputStream());


            // ------向HTTP请求添加上传文件部分------
            if (files != null && files.isEmpty() == false) {
                for (Map.Entry<String, File> fileEntry : files.entrySet()) {
                    String fileName = fileEntry.getValue().getName();

                    dataOutStream.writeBytes("--" + multipartBoundary);
                    dataOutStream.writeBytes(LINE_FEED);
                    dataOutStream.writeBytes("Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + fileName + "\"");
                    dataOutStream.writeBytes(LINE_FEED);
                    dataOutStream.writeBytes("Content-Type: multipart/form-data");
                    dataOutStream.writeBytes(LINE_FEED);
                    dataOutStream.writeBytes("Content-Transfer-Encoding: binary");
                    dataOutStream.writeBytes(LINE_FEED);

                    dataOutStream.writeBytes(LINE_FEED);

                    InputStream iStream = null;
                    try {
                        iStream = new FileInputStream(fileEntry.getValue());
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = iStream.read(buffer)) != -1) {
                            dataOutStream.write(buffer, 0, bytesRead);
                        }

                        iStream.close();
                        dataOutStream.writeBytes(LINE_FEED);
                        dataOutStream.flush();
                    } catch (IOException ignored) {
                    } finally {
                        try {
                            if (iStream != null) {
                                iStream.close();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            dataOutStream.writeBytes(LINE_FEED);

            // ------添加Post请求参数-----
            if (params != null && params.isEmpty() == false) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    dataOutStream.writeBytes("--" + multipartBoundary);
                    dataOutStream.writeBytes(LINE_FEED);
                    //
                    dataOutStream.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"");
                    dataOutStream.writeBytes(LINE_FEED);
                    dataOutStream.writeBytes("Content-Type: text/plain");
                    dataOutStream.writeBytes(LINE_FEED);
                    //
                    dataOutStream.writeBytes(LINE_FEED);
                    dataOutStream.writeBytes(entry.getValue());
                    dataOutStream.writeBytes(LINE_FEED);
                }
            }
            //
            dataOutStream.writeBytes("--" + multipartBoundary + "--");
            dataOutStream.writeBytes(LINE_FEED);
            dataOutStream.close();
            // ---------获取HTTP响应----------
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new Exception("status is " + status);
            }
            // ------从流中获取数据------
            return convertStreamToString(connection.getInputStream());
        } finally {
            try {
                //
                if (connection != null) {
                    connection.disconnect();
                }
                //
                if (dataOutStream != null) {
                    dataOutStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从流中读取数据
     *
     * @param is
     * @return
     * @throws Exception
     */
    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
