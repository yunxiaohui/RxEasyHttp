/*
 * Copyright (C) 2017 zhouyou(478319399@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhouyou.http.func;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zhouyou.http.model.ApiResult;
import com.zhouyou.http.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import rx.functions.Func1;


/**
 * <p>描述：定义了ApiResult结果转换Func</p>
 * 作者： zhouyou<br>
 * 日期： 2017/3/15 16:52 <br>
 * 版本： v1.0<br>
 */
public class ApiResultFunc<T> implements Func1<ResponseBody, ApiResult<T>> {
    protected Type type;
    protected Gson gson;

    public ApiResultFunc(Type type) {
        gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();
        this.type = type;
    }

    @Override
    public ApiResult<T> call(ResponseBody responseBody) {
        ApiResult<T> apiResult = new ApiResult<T>();
        apiResult.setCode(-1);
        if (type instanceof ParameterizedType) {//自定义ApiResult
            Class<T> cls = (Class) ((ParameterizedType) type).getRawType();
            if (ApiResult.class.isAssignableFrom(cls)) {
                Type[] params = ((ParameterizedType) type).getActualTypeArguments();
                Class clazz = Utils.getClass(params[0], 0);
                try {
                    String json = responseBody.string();
                    if (clazz.equals(String.class)) {
                        apiResult.setData((T) json);
                        apiResult.setCode(0);
                    } else {
                        ApiResult result = gson.fromJson(json, type);
                        if (result != null) {
                            apiResult = result;
                        } else {
                            apiResult.setMsg("json is null");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    apiResult.setMsg(e.getMessage());
                } finally {
                    responseBody.close();
                }
            } else {
                apiResult.setMsg("ApiResult.class.isAssignableFrom(cls) err!!");
            }
        } else {//默认Apiresult
            try {
                String json = responseBody.string();
                Class<T> clazz = Utils.getClass(type, 0);
                if (clazz.equals(String.class)) {
                    apiResult.setData((T) json);
                    apiResult.setCode(0);
                } else {
                    ApiResult result = parseApiResult(json, apiResult);
                    if (result != null) {
                        apiResult = result;
                        if (apiResult.getData() != null) {
                            T data = gson.fromJson(apiResult.getData().toString(), clazz);
                            apiResult.setData(data);
                        } else {
                            apiResult.setMsg("ApiResult's data is null");
                        }
                    } else {
                        apiResult.setMsg("json is null");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                apiResult.setMsg(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                apiResult.setMsg(e.getMessage());
            } finally {
                responseBody.close();
            }
        }
        return apiResult;
    }

    private ApiResult parseApiResult(String json, ApiResult apiResult) throws JSONException {
        if (TextUtils.isEmpty(json))
            return null;
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has("code")) {
            apiResult.setCode(jsonObject.getInt("code"));
        }
        if (jsonObject.has("data")) {
            apiResult.setData(jsonObject.getString("data"));
        }
        if (jsonObject.has("msg")) {
            apiResult.setMsg(jsonObject.getString("msg"));
        }
        return apiResult;
    }
}
