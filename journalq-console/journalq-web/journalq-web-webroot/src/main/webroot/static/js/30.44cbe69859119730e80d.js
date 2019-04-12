/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
webpackJsonp([30],{"0Zbj":function(t,e){},"VKC/":function(t,e,a){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var r={name:"task-form",mixins:[a("lcoF").a],props:{type:0,data:{type:Object,default:function(){return{type:"",mutex:"",referId:0,priority:0,daemons:!1,url:"",cron:"",dispatchType:0,retry:!0,status:1}}}},data:function(){return{formData:this.data,rules:{type:[{required:!0,message:"请填写类型",trigger:"change"}],referId:[{required:!0,message:"请填写关联id",trigger:"change"}],priority:[{required:!0,message:"请填优先级",trigger:"change"}],daemons:[{required:!0,message:"请填写守护线程",trigger:"change"}],dispatchType:[{required:!0,message:"请填写派发类型",trigger:"change"}],retry:[{required:!0,message:"请填写重试类型",trigger:"change"}]}}}},o={render:function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("d-form",{ref:"form",staticStyle:{height:"350px","overflow-y":"auto",width:"100%","padding-right":"20px"},attrs:{model:t.formData,rules:t.rules,"label-width":"100px"}},[a("d-form-item",{staticStyle:{width:"60%"},attrs:{label:"类型:",prop:"type"}},[a("d-input",{staticStyle:{width:"249px"},attrs:{placeholder:"由字母和点组成，如Topic.Update"},model:{value:t.formData.type,callback:function(e){t.$set(t.formData,"type",e)},expression:"formData.type"}})],1),t._v(" "),a("d-form-item",{staticStyle:{width:"60%"},attrs:{label:"互斥类型:"}},[a("d-input",{staticStyle:{width:"249px"},attrs:{placeholder:"互斥任务类型,支持后缀*，如Topic.*"},model:{value:t.formData.mutex,callback:function(e){t.$set(t.formData,"mutex",e)},expression:"formData.mutex"}})],1),t._v(" "),a("d-form-item",{staticStyle:{width:"60%"},attrs:{label:"关联主键:",prop:"referId"}},[a("d-input",{staticStyle:{width:"249px"},model:{value:t.formData.referId,callback:function(e){t.$set(t.formData,"referId",e)},expression:"formData.referId"}})],1),t._v(" "),a("d-form-item",{staticStyle:{width:"60%"},attrs:{label:"优先级:",prop:"priority"}},[a("d-input",{staticStyle:{width:"249px"},model:{value:t.formData.priority,callback:function(e){t.$set(t.formData,"priority",e)},expression:"formData.priority"}})],1),t._v(" "),a("d-form-item",{attrs:{label:"守护任务:",prop:"daemons"}},[a("d-radio-group",{model:{value:t.formData.daemons,callback:function(e){t.$set(t.formData,"daemons",e)},expression:"formData.daemons"}},[a("d-radio",{attrs:{label:!0}},[t._v("是")]),t._v(" "),a("d-radio",{attrs:{label:!1}},[t._v("否")]),t._v(" "),t.formData.daemons?a("span",{staticStyle:{color:"red"}},[t._v("守护任务默认重试")]):t._e()],1)],1),t._v(" "),a("d-form-item",{staticStyle:{width:"60%"},attrs:{label:"参数:",prop:"url"}},[a("d-input",{staticStyle:{width:"249px"},attrs:{placeholder:"任务的参数，如?a=1&b=2"},model:{value:t.formData.url,callback:function(e){t.$set(t.formData,"url",e)},expression:"formData.url"}})],1),t._v(" "),a("d-form-item",{staticStyle:{width:"60%"},attrs:{label:"表达式:",prop:"cron"}},[a("d-input",{staticStyle:{width:"249px"},attrs:{placeholder:"cron表达式，如 0 5 0/1 * * ?"},model:{value:t.formData.cron,callback:function(e){t.$set(t.formData,"cron",e)},expression:"formData.cron"}})],1),t._v(" "),a("d-form-item",{attrs:{label:"派发类型:",prop:"dispatchType"}},[a("d-radio-group",{model:{value:t.formData.dispatchType,callback:function(e){t.$set(t.formData,"dispatchType",e)},expression:"formData.dispatchType"}},[a("d-radio",{attrs:{label:0}},[t._v("任意执行器")]),t._v(" "),a("d-radio",{attrs:{label:1}},[t._v("原有执行器优先 ")]),t._v(" "),a("d-radio",{attrs:{label:2}},[t._v("必须原有执行器")])],1)],1),t._v(" "),a("d-form-item",{attrs:{label:"重试:",prop:"retry"}},[a("d-radio-group",{model:{value:t.formData.retry,callback:function(e){t.$set(t.formData,"retry",e)},expression:"formData.retry"}},[a("d-radio",{attrs:{label:!0}},[t._v("是")]),t._v(" "),a("d-radio",{attrs:{label:!1}},[t._v("否")])],1)],1)],1)},staticRenderFns:[]};var i=a("VU/8")(r,o,!1,function(t){a("0Zbj")},"data-v-7bb79e8c",null);e.default=i.exports}});
//# sourceMappingURL=30.44cbe69859119730e80d.js.map