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
import ReactDOM from 'react-dom';
import { createStore, combineReducers, compose, applyMiddleware, Reducer, Store } from 'redux';
import { routerReducer } from 'react-router-redux';
import thunk from 'redux-thunk';
import { Provider } from 'react-redux';
import { REDUX_DEVTOOLS } from './contants';
import reducers from './reducers';
import App from './app';
import '@alicloud/console-components/dist/wind.css';
import './index.scss';

const reducer: Reducer = combineReducers({
    ...reducers,
    routing: routerReducer,
});

const store: Store = createStore(
    reducer,
    compose(
        applyMiddleware(thunk),
        (window as any)[REDUX_DEVTOOLS] ? (window as any)[REDUX_DEVTOOLS]() : (f: any) => f
    )
);

(window as any).g_store = store;

ReactDOM.render(
    <Provider store={store}>
      <App />
    </Provider>,
    document.getElementById('root')
);