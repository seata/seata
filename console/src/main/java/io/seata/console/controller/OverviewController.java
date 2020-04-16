/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.console.controller;


import io.seata.console.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Overview
 *
 * @author jameslcj
 */
@RestController
@RequestMapping("/api/v1/overview")
public class OverviewController {

    @GetMapping(value = "/getData")
    public Result<List> getData() {
        List<Map<String, Object>> result = new ArrayList<>();
        int count = 10;
        while (count-- > 0) {
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", "seata" + count);
            hashMap.put("id", count);
            result.add(hashMap);
        }

        return Result.ofSuccess(result);
    }
}
