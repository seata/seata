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

/* eslint-disable */
export const throttle = (fn: any, delay: number) => {
  let timer: any = null;
  return function(...args: any) {
    const context = this;
    clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(context, args);
    }, delay);
  };
};

export const getScrollTop = () => {
  let scrollTop = 0;
  if (document.documentElement && document.documentElement.scrollTop) {
    ({ scrollTop } = document.documentElement);
  } else if (document.body) {
    ({ scrollTop } = document.body);
  }
  return scrollTop;
};

export const getLink = (link: string) => {
  if (`${link}`.length > 1 && /^\/[^/]/.test(`${link}`)) {
    return `${(window as any).rootPath}${link}`;
  }
  return link;
};

export const getParameter = (search: string, name: string) => {
  const [, query = ''] = search.split('?');
  const [hit = ''] = query.split('&').filter(item => name === item.split('=')[0]);
  const [, value = ''] = hit.split('=');
  return value;
};
