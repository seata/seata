import React, { PureComponent } from 'react';
import styled, { css } from 'styled-components';
import { Breadcrumb } from '@alicloud/console-components';
import _ from 'lodash';

const PageHeaderWrapper = styled.div`
  margin: 16px 0;
`;

const Title = styled.h3`
  font-size: 28px;
  font-weight: 400;
  margin: 0px;
  margin-top: 16px;
  vertical-align: middle;
`;

export default class PageHeader extends PureComponent<any> {
  render() {
    const { title, breadcrumbs = [], separator = '/' } = this.props;
    return (
      <PageHeaderWrapper>
        <Breadcrumb separator={separator}>
          {_.map(breadcrumbs, ({ link, text }: {link: string, text: string}) => (
            <Breadcrumb.Item key={text} link={link}>
              {text}
            </Breadcrumb.Item>
          ))}
        </Breadcrumb>
        <Title>{title}</Title>
      </PageHeaderWrapper>
    );
  }
}
