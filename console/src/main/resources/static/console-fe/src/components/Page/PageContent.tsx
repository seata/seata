import React, { PureComponent } from 'react';

export default class PageContent extends PureComponent<any> {
  render() {
    const { children } = this.props;
    return <div>{children}</div>;
  }
}
