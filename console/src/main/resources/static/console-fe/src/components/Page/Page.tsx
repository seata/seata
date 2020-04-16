import React, { PureComponent } from 'react';
import styled, { css } from 'styled-components';
import PageHeader from './PageHeader';
import PageContent from './PageContent';

const PageWrapper = styled.div`
  padding: 0;
`;

export default class Page extends PureComponent<any> {
  render() {
    const { title, breadcrumbs, separator, children } = this.props;
    return (
      <PageWrapper>
        <PageHeader title={title} breadcrumbs={breadcrumbs} separator={separator} />
        <PageContent>{children}</PageContent>
      </PageWrapper>
    );
  }
}
