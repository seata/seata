/**
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React from 'react';
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { Message } from '@alicloud/console-components';
import { get } from 'lodash';
import { GlobalStateModel } from '@/reducers';
import { AUTHORIZATION_HEADER } from '@/contants';

const API_GENERAL_ERROR_MESSAGE: string = 'Request error, please try again later!';

const codeMessage = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '用户没有权限（令牌、用户名、密码错误）。',
  403: '用户得到授权，但是访问是被禁止的。',
  404: '发出的请求针对的是不存在的记录，服务器没有进行操作。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。',
  '-1000': '项目名称已存在, 请使用其他名称',
};

const request = () => {
  const instance: AxiosInstance = axios.create({
    baseURL: '/api/v1',
    method: 'get',
  });

  instance.interceptors.request.use((config: AxiosRequestConfig) => {
    let authHeader: string | null = localStorage.getItem(AUTHORIZATION_HEADER);
    // add jwt header
    config.headers[AUTHORIZATION_HEADER] = authHeader;

    return config;
  })

  instance.interceptors.response.use(
    (response: AxiosResponse): Promise<any> => {
      const code = get(response, 'data.code');
      if (response.status === 200 && code === 200) {
        return Promise.resolve(get(response, 'data.data'));
      } else {
        const errorText =
          (codeMessage as any)[code] ||
          get(response, 'data.message') ||
          get(response, 'data.errorMsg') ||
          response.statusText;
        Message.error(errorText || `请求错误 ${code}: ${get(response, 'config.url', '')}`);
        return Promise.reject(response);
      }
    },
    error => {
      if (error.response) {
        const { status } = error.response;
        if (status === 403 || status === 401) {
          (window as any).globalHistory.replace('login');
          return;
        }
        Message.error(`HTTP ERROR: ${status}`);
      } else {
        Message.error(API_GENERAL_ERROR_MESSAGE);
      }
      return Promise.reject(error);
    }
  );

  return instance;
};

export default request();
