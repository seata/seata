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

import { Dispatch } from 'redux';
import { loginService } from '@/service/login';
import { SET_LOGIN, AUTHORIZATION_HEADER } from '@/contants';

export type UserType = {
  username: string;
  password: string;
};

export interface LoginStateModel {
  authHeader: string;
}

const initialState: LoginStateModel = {
  authHeader: ''
};

const login = (userInfo: UserType) => async (dispatch: Dispatch): Promise<string> => {
  let authHeader: string = await loginService(userInfo);
  localStorage.setItem(AUTHORIZATION_HEADER, authHeader);
  dispatch({
    type: SET_LOGIN,
    data: {
      authHeader
    }
  })
  return authHeader;
};

export default (state = initialState, action: any) => {
  switch (action.type) {
    case SET_LOGIN:
      return { ...state, ...action.data };
    default:
      return state;
  }
};

export { login };
