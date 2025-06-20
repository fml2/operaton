/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';
module.exports = [
  '$window',
  function($window) {
    var storage = $window.localStorage;
    var values = JSON.parse(storage.getItem('operaton') || '{}');
    return {
      get: function(key, defaultValue) {
        return typeof values[key] !== 'undefined' ? values[key] : defaultValue;
      },
      set: function(key, value) {
        values[key] = value;
        storage.setItem('operaton', JSON.stringify(values));
      },
      remove: function(key) {
        delete values[key];
        storage.setItem('operaton', JSON.stringify(values));
      }
    };
  }
];
