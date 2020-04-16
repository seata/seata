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
import Header from '@/components//Header';
import { Bodyer, NavBar, Content } from './style';
import './index.scss';

export type PropsType = {
    children: any
    nav: any;
    location?: any;
}

export default class Layout extends React.PureComponent<PropsType> {
    render() {
        console.log('this.props: Layout: ', this.props);
        const { children, nav, location } = this.props;
        return (
            <div>
                <Header />
                <Bodyer>
                    <NavBar className="navbar">{nav({location})}</NavBar>
                    <Content>{children}</Content>
                </Bodyer>
            </div>
        )
    }
}