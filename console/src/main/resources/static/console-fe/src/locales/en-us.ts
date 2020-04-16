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

import { ILocale } from './index.d';

const enUs: ILocale = {
  MenuRouter: {
    overview: 'Overview',
  },
  Header: {
    home: 'HOME',
    docs: 'DOCS',
    blog: 'BLOG',
    community: 'COMMUNITY',
    languageSwitchButton: '中',
    logout: 'logout',
    passwordRequired: 'password should not be empty',
    usernameRequired: 'username should not be empty',
  },
  Login: {
    login: 'Login',
    submit: 'Submit',
    pleaseInputUsername: 'Please input username',
    pleaseInputPassword: 'Please input password',
    invalidUsernameOrPassword: 'invalid username or password',
    desc: 'Seata is an open source distributed transaction solution that delivers high performance and easy to use distributed transaction services under a microservices architecture.'
  },
  Overview: {
    title: 'Overview',
    subTitle: 'list',
    search: 'search',
  }
};

export default enUs;
