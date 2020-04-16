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
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { Router, Route, Switch, Redirect, RouteComponentProps } from 'react-router-dom';
import { ConfigProvider, Loading } from '@alicloud/console-components';
import { createHashHistory, History } from "history";
import CCConsoleMenu from '@alicloud/console-components-console-menu';
import { GlobalStateModel } from '@/reducers';
import { changeLanguage, LocaleStateModel, getCurrentLanguage } from '@/reducers/locale';
import Layout from '@/layout';
import Login from '@/pages/Login';
import router from '@/router';

export const history: History = createHashHistory();
(window as any).globalHistory = history;

export type OwnProps = any;

export type StateToPropsType = LocaleStateModel;

export type DispathToPropsType = {
    changeLanguage: (lang: string) => void
};

export type AppPropsType = StateToPropsType & DispathToPropsType & RouteComponentProps & OwnProps;

export type AppStateType = {
    loading: object;
}

class App extends React.Component<AppPropsType, AppStateType> {
    static propTypes = {
        locale: PropTypes.object,
        changeLanguage: PropTypes.func,
    };

    state: AppStateType = {
        loading: {},
    };

    constructor(props: AppPropsType) {
        super(props);
    }

    componentDidMount() {
        console.log('this.props: ', this.props, history);
        const language: string = getCurrentLanguage();
        this.props.changeLanguage(language);
    }

    get menu() {
        const { locale }: AppPropsType = this.props;
        const { MenuRouter = {} } = locale;
        const { overview } = MenuRouter;
        return {
            items: [
                {
                    key: '/Overview',
                    label: overview,
                },
            ],
            header: 'Seata',
            onItemClick: (key: string) => history.push(key)
        }
    }

    get router() {
        return (
            <Router history={history}>
                <Switch>
                    <Route path="/login" component={Login} />
                    <Layout nav={({ location }: any) => <CCConsoleMenu  {...this.menu} activeKey={location.pathname} />}>
                        <Route path={'/'} exact render={() => <Redirect to="/Overview" />} />
                        {router.map(item => (
                            <Route key={item.path} {...item} />
                        ))}
                    </Layout>
                </Switch>
            </Router>
        );
    }

    render() {
        const { locale } = this.props;
        const { loading } = this.state;
        return (
            <Loading
                tip="loading..."
                visible={false}
                fullScreen
                {...loading}
            >
                <ConfigProvider locale={locale}>
                    {this.router}
                </ConfigProvider>
            </Loading>
        );
    }
}


const mapStateToProps = (state: GlobalStateModel, ownProps: OwnProps): StateToPropsType => ({
    ...state.locale
});

const mapDispatchToProps = (dispatch: Dispatch): DispathToPropsType => ({
    changeLanguage: (lang) => (changeLanguage(lang)(dispatch))
});

export default connect(mapStateToProps, mapDispatchToProps)(App as any);