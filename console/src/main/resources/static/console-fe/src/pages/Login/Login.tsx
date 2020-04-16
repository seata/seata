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
import { Card, Form, Input, ConfigProvider, Field } from '@alicloud/console-components';
import { withRouter } from 'react-router-dom';
import { Dispatch } from 'redux';
import { connect } from 'react-redux';
import { GlobalStateModel } from '@/reducers';
import { LoginStateModel, UserType, login } from '@/reducers/login';
import { GlobalProps } from '@/module';

import './index.scss';
import PropTypes from 'prop-types';

type DispathToPropsType = {
  login: (userInfo: UserType) => Promise<string>
}

type LoginPropsType = {
} & DispathToPropsType & LoginStateModel & GlobalProps;
class Login extends React.Component<LoginPropsType> {
  static displayName = 'Login';

  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
  };

  field = new Field(this);

  constructor(props: LoginPropsType) {
    super(props);
  }

  handleSubmit = () => {
    const { history, login } = this.props;
    this.field.validate(async (errors, values) => {
      if (errors) {
        return;
      }
      const { username, password }: any = values;
      const userInfo: UserType = {
        username,
        password
      }
      let authHeader = await login(userInfo);
      if (!!authHeader) {
        history.push('/');
      }
    });
  };

  onKeyDown = (event: any) => {
    // 'keypress' event misbehaves on mobile so we track 'Enter' key via 'keydown' event
    if (event.key === 'Enter') {
      event.preventDefault();
      event.stopPropagation();
      this.handleSubmit();
    }
  };

  render() {
    const { locale = {} } = this.props;

    return (
      <div className="home-page">
        <section
          className="top-section"
          style={{
            background: 'url(img/black_dot.png) repeat',
            backgroundSize: '14px 14px',
          }}
        >
          <div className="vertical-middle product-area">
            <img className="product-logo" src="img/seata_logo_white.png" />
            <p className="product-desc">
              {locale.desc}
            </p>
          </div>
          <div className="animation animation1" />
          <div className="animation animation2" />
          <div className="animation animation3" />
          <div className="animation animation4" />
          <div className="animation animation5" />
          <Card className="login-panel" contentHeight="auto">
            <div className="login-header">{locale.login}</div>
            <Form className="login-form" field={this.field}>
              <Form.Item>
                <Input
                  {...this.field.init('username', {
                    rules: [
                      {
                        required: true,
                        message: locale.usernameRequired,
                      },
                    ],
                  })}
                  placeholder={locale.pleaseInputUsername}
                  onKeyDown={this.onKeyDown}
                />
              </Form.Item>
              <Form.Item>
                <Input
                  htmlType="password"
                  placeholder={locale.pleaseInputPassword}
                  {...this.field.init('password', {
                    rules: [
                      {
                        required: true,
                        message: locale.passwordRequired,
                      },
                    ],
                  })}
                  onKeyDown={this.onKeyDown}
                />
              </Form.Item>
              <Form.Item label=" ">
                <Form.Submit onClick={this.handleSubmit}>{locale.submit}</Form.Submit>
              </Form.Item>
            </Form>
          </Card>
        </section>
      </div>
    );
  }
}

const mapStateToProps = (state: GlobalStateModel): LoginStateModel => ({
  ...state.login
});

const mapDispatchToProps = (dispatch: Dispatch): DispathToPropsType => ({
  login: (userInfo: UserType):Promise<string> => (login(userInfo)(dispatch))
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(ConfigProvider.config(Login, {})));