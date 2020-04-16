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
import { ConfigProvider, Table, Input, Select, Button } from '@alicloud/console-components';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { withRouter } from 'react-router-dom';
import { GlobalStateModel } from '@/reducers';
import { OverviewStateModel, getData } from '@/reducers/overview'
import './index.scss';
import Page from '@/components/Page';
import { GlobalProps } from '@/module';

type StateToPropsType = OverviewStateModel;

type DispathToPropsType = {
  getData: () => void
};

type OverviewPropsType = {
} & StateToPropsType & DispathToPropsType & GlobalProps;

class Overview extends React.Component<OverviewPropsType> {
  static displayName = 'Overview';

  constructor(props: OverviewPropsType) {
    super(props);
  }
  componentDidMount() {
    const { getData } = this.props;
    getData();
  }
  render() {
    const { locale = {}, getData, data } = this.props;
    const { title, subTitle, search } = locale;
    return (
      <Page title={title} breadcrumbs={[
        {
          link: '/',
          text: title,
        },
        {
          text: subTitle,
        }
      ]}>
        <div>
          <Input />
          <Button type="primary" className="ml-8" onClick={getData}>{search}</Button>
        </div>
        <Table className="mt-16" dataSource={data}>
          <Table.Column title="id" dataIndex="id"/>
          <Table.Column title="name" dataIndex="name"/>
        </Table>
      </Page>
    );
  }
}

const mapStateToProps = (state: GlobalStateModel): StateToPropsType => ({
  ...state.overview
});

const mapDispatchToProps = (dispatch: Dispatch): DispathToPropsType => ({
  getData: () => (getData()(dispatch))
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(ConfigProvider.config(Overview, {})));