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
import fusionEnUS from '@alifd/next/lib/locale/en-us';
import fusionZhCN from '@alifd/next/lib/locale/zh-cn';
import I18N, { ILocale } from '@/locales';
import { LANGUAGE_KEY, LANGUAGE_SWITCH } from '@/contants';

const enUS = Object.assign({}, fusionEnUS, I18N.enUS);
const zhCN = Object.assign({}, fusionZhCN, I18N.zhCN);

interface LocaleStateModel {
  language: string;
  locale: ILocale;
}

export const enUsKey = 'en-US';
export const zhCnKey = 'zh-CN';

const initialState: LocaleStateModel = {
  language: enUsKey,
  locale: enUS,
};

type IChangeLanguage = (lang: string| null) => (dispacth: Dispatch) => void;

const changeLanguage: IChangeLanguage = lang => dispatch => {
  const language = lang === zhCnKey ? zhCnKey : enUsKey;
  localStorage.setItem(LANGUAGE_KEY, language);
  dispatch({ type: LANGUAGE_SWITCH, language, locale: language === enUsKey ? enUS : zhCN });
};

const getCurrentLanguage = (): string => {
  let lang: string| null = localStorage.getItem(LANGUAGE_KEY);
  if (!lang) {
    lang = enUsKey;
  }
  return lang;
}

const getCurrentLocaleObj = (): any => {
  let lang = getCurrentLanguage();

  return lang === zhCnKey ? zhCN : enUS;
}

export default (state = initialState, action: any) => {
  switch (action.type) {
    case LANGUAGE_SWITCH:
      return { ...state, ...action };
    default:
      return state;
  }
};

export { changeLanguage, IChangeLanguage, LocaleStateModel, getCurrentLanguage, getCurrentLocaleObj };
