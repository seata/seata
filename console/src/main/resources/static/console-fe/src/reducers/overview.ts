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
import { fetchData } from '@/service/overview';
import { SET_OVERVIEW } from '@/contants';

export type OverviewData = {
  id: number;
  name: string;
};

export interface OverviewStateModel {
  data: Array<OverviewData>;
}

const initialState: OverviewStateModel = {
  data: [],
};

const getData = () => async (dispatch: Dispatch) => {
  let data: Array<OverviewData> = await fetchData();
  dispatch({
    type: SET_OVERVIEW,
    data: {
      data,
    },
  });
};

export default (state = initialState, action: any) => {
  switch (action.type) {
    case SET_OVERVIEW:
      return { ...state, ...action.data };
    default:
      return state;
  }
};

export { getData };
