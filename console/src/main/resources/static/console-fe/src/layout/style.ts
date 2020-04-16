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
import styled from 'styled-components';

export const Bodyer = styled.div`
    display: flex;
    height: calc(100vh - 66px);
    margin-top: 66px;
`;

export const NavBar = styled.div`
    height: 100%;
    width: 208px;
    min-width: 208px;
    position: relative;
    z-index: 100;
    flex: 0 1 auto;
    transition: width 0.3s ease-in-out 0s, min-width 0.3s ease-in-out 0s;
`;

export const Content = styled.div`
    height: 100%;
    overflow-y: auto;
    flex: 1 1 auto;
    padding: 0px 24px;
`;