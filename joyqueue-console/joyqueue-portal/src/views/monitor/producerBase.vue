<template>
  <div>
    <div class="headLine">
      <d-input v-model="keyword" :placeholder="keywordTip" oninput="value = value.trim()" class="input" @on-enter="getList">
        <span slot="prepend">{{keywordName}}</span>
        <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
      </d-input>
      <d-button-group>
        <d-button v-if="$store.getters.isAdmin" @click="openAndQueryDialog('subscribeDialog', 'subscribe')" class="button">
          订阅<icon name="plus-circle" style="margin-left: 3px;"/>
        </d-button>
        <d-button type="primary" @click="getList" class="button">刷新
          <icon name="refresh-cw" style="margin-left: 3px;"></icon>
        </d-button>
      </d-button-group>
    </div>
    <my-table :data="tableData" style="z-index: 1" :showPin="showTablePin" :showPagination="this.showPagination" :page="page" @on-size-change="handleSizeChange"
              @on-detail-chart="goDetailChart" @on-current-change="handleCurrentChange" @on-detail="openDetailTab"
              @on-config="openConfigDialog" @on-weight="openWeightDialog" @on-send-message="openSendMessageDialog"
              @on-cancel-subscribe="cancelSubscribe" @on-rateLimit="openRateLimitDialog" @on-compare-chart="goCompareChart"
              @on-summary-chart="goSummaryChart" @on-performance-chart="goPerformanceChart" @on-producer-policy="producerPolicyDetail" />
    <d-button class="right load-btn" style="z-index: 2" v-if="this.curIndex < this.cacheList.length-1 && this.cacheList.length!==0" type="primary" @click="getRestList">加载更多
      <icon name="refresh-cw" style="margin-left: 3px;"></icon>
    </d-button>

    <!--生产订阅弹出框-->
    <my-dialog :dialog="subscribeDialog" class="maxDialogHeight" @on-dialog-cancel="dialogCancel('subscribeDialog')">
      <subscribe ref="subscribe" :search="search" :type="type" :colData="subscribeDialog.colData"
                 :keywordName="keywordName" :searchUrl="subscribeDialog.urls.search" :addUrl="subscribeDialog.urls.add"
                 @on-refresh="getList"/>
    </my-dialog>

    <!--Config dialog-->
    <my-dialog :dialog="configDialog" @on-dialog-confirm="configConfirm" @on-dialog-cancel="dialogCancel('configDialog')">
      <grid-row :v-if="configDialogTip">{{configDialogTip}}</grid-row>
      <producer-config-form ref="configForm" :data="configData"
                            :archive-config-enabled="configData.archive || !forbidEnableArchive"
                            :nearby-config-enabled="configData.nearBy || !forbidEnableNearby"/>
    </my-dialog>
    <my-dialog :dialog="weightDialog" @on-dialog-confirm="weightConfigConfirm" @on-dialog-cancel="dialogCancel('weightDialog')">
      <producer-weight-form ref="weightForm" :producerId="producerId"/>
    </my-dialog>

    <my-dialog :dialog="sendMessageDialog" @on-dialog-confirm="sendMessageConfirm" @on-dialog-cancel="dialogCancel('sendMessageDialog')">
      <producer-sendMessage-form ref="sendMessageForm" :data="sendMessageDialog.data" />
    </my-dialog>
    <my-dialog :dialog="rateLimitDialog" @on-dialog-confirm="rateLimitConfirm" @on-dialog-cancel="dialogCancel('rateLimitDialog')">
      <rate-limit ref="rateLimit" :limitTraffic="rateLimitDialog.limitTraffic" :limitTps="rateLimitDialog.limitTps"/>
    </my-dialog>
    <my-dialog :dialog="policyDialog" @on-dialog-confirm="policyConfirm" @on-dialog-cancel="dialogCancel('policyDialog')">
      <d-form ref="policyMetadata" label-width="200px">
        <d-form-item v-for="item in producerPolicies" :key="item.key" :label="item.key + ':'">
          <d-input v-model="item.value" style="width: 250px;"/>
        </d-form-item>
      </d-form>
    </my-dialog>
  </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest.js'
import myTable from '../../components/common/myTable.vue'
import myDialog from '../../components/common/myDialog.vue'
import subscribe from './subscribe.vue'
import ProducerConfigForm from './producerConfigForm.vue'
import ProducerWeightForm from './producerWeightForm.vue'
import {getTopicCode, replaceChartUrl, sortByProducer} from '../../utils/common.js'
import RateLimit from './rateLimit'
import ProducerSendMessageForm from './producerSendMessageForm'
import ButtonGroup from '../../components/button/button-group'
import apiUrl from '../../utils/apiUrl.js'

export default {
  name: 'producer-base',
  components: {
    ButtonGroup,
    RateLimit,
    myTable,
    myDialog,
    subscribe,
    ProducerConfigForm,
    ProducerWeightForm,
    ProducerSendMessageForm
  },
  props: {
    keywordTip: {
      type: String
    },
    keywordName: {
      type: String
    },
    btns: {
      type: Array
    },
    operates: {
      type: Array
    },
    btnGroups: {
      type: Object
    },
    colData: { // 生产者 列表表头
      type: Array
    },
    showPagination: {
      type: Boolean
    },
    search: {// 查询条件，我的应用：app:{id:0,code:'',namespace:{id:0,code:''}}  ， 主题中心：topic:{id:0,code:'',namespace:{id:0,code:''}}
      type: Object
    },
    subscribeDialogColData: { // 订阅 列表表头
      type: Array
    },
    subscribeUrls: {
      type: Object
    },
    monitorUrls: {// url variable format: [app], [topic], [namespace]
      type: Object
    },
    configDialogTip: {
      type: String,
      default: undefined
    },
    forbidEnableArchive: { // 禁止开启归档
      type: Boolean,
      default: false
    },
    forbidEnableNearby: { // 禁止开启归档
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      curIndex: 0,
      cacheList: [],
      producerPolicy: {
        qosLevel: undefined,
        region: undefined
      },
      producerPolicies: [],
      urls: {
        search: `/producer/search`,
        getMonitor: `/monitor/find`,
        del: `/producer/delete`,
        sendMessage: '/monitor/producer/sendMessage'
      },
      showTablePin: false,
      tableData: {
        rowData: [],
        colData: this.colData,
        btns: this.btns,
        operates: this.operates,
        btnGroups: this.btnGroups
      },
      keyword: '',
      page: {
        page: 1,
        size: 10,
        total: 100
      },
      sendMessageDialog: {
        visible: false,
        title: '发送消息',
        width: '850'
      },
      rateLimitDialog: {
        visible: false,
        title: '限流',
        width: '500',
        showFooter: true,
        limitTps: 0,
        limitTraffic: 0
      },
      policyDialog: {
        visible: false,
        title: '策略详情',
        width: '500',
        showFooter: true
      },
      type: this.$store.getters.producerType, // 1:生产， 2：消费
      subscribeDialog: {
        visible: false,
        title: '添加生产者',
        width: '850',
        showFooter: false,
        colData: this.subscribeDialogColData.map((element) => Object.assign({}, element)), // 订阅框 列表表头,
        urls: {
          add: this.subscribeUrls.add,
          search: this.subscribeUrls.search
        }
      },
      weightDialog: {
        visible: false,
        title: '设置生产权重',
        width: '700',
        showFooter: true
      },
      producerId: '',
      configDialog: {
        visible: false,
        title: '生产者配置详情',
        width: '600',
        showFooter: true,
        urls: {
          addOrUpdae: `/producer/config/addOrUpdate`
        }
      },
      configData: {},
      monitorUIds: {
        detail: this.$store.getters.uIds.producer.detail,
        summary: this.$store.getters.uIds.producer.summary,
        performance: this.$store.getters.uIds.producer.performance,
        compare: this.$store.getters.uIds.producer.compare
      }
    }
  },
  methods: {
    openDialog (dialog) {
      this[dialog].visible = true
    },
    openAndQueryDialog (dialog, ref) {
      this[dialog].visible = true
      this.$nextTick(() => {
        this.$refs[ref].getList()
      })
    },
    openDetailTab (item) {
      this.$emit('on-detail', item)
    },
    openConfigDialog (item) {
      this.configData = item.config || {}
      this.configData['producerId'] = item.id
      this.configDialog.visible = true
    },
    openWeightDialog (item) {
      // this.$refs['weightForm'].getListById(item.id)
      this.producerId = item.id
      this.openDialog('weightDialog')
    },
    openRateLimitDialog (item) {
      this.configData = item.config || {}
      this.rateLimitDialog.limitTps = item.config.limitTps
      this.configData['producerId'] = item.id
      this.rateLimitDialog.limitTraffic = item.config.limitTraffic
      this.rateLimitDialog.visible = true
    },
    openSendMessageDialog (item) {
      this.configData = item.config || {}
      this.configData['producerId'] = item.id
      this.sendMessageDialog.data = {
        topic: item.topic.code,
        namespace: item.namespace.code,
        app: item.app.code,
        message: ''
      }
      this.sendMessageDialog.visible = true
    },
    cancelSubscribe (item) {
      let _this = this
      this.$Dialog.confirm({
        title: '提示',
        content: '确定要取消订阅吗？'
      }).then(() => {
        apiRequest.postBase(_this.urls.del, {}, item.id, true).then((data) => {
          if (data.code !== this.$store.getters.successCode) {
            this.$Dialog.error({
              content: '取消订阅失败'
            })
          } else {
            this.$Message.success('取消订阅成功')
            _this.getList()
          }
        })
      })
    },
    handleSizeChange (val) {
      this.page.size = val
      this.getList()
    },
    handleCurrentChange (val) {
      this.page.page = val
      this.getList()
    },
    dialogConfirm (dialog) {
      this[dialog].visible = false
      this.getList()
    },
    dialogCancel (dialog) {
      this[dialog].visible = false
      this.producerPolicies = undefined
      this.getList()
    },
    policyConfirm () {
      if (this.producerPolicies) {
        for (let policy in this.producerPolicies) {
          if (this.producerPolicies.hasOwnProperty(policy)) {
            this.configData[this.producerPolicies[policy].key] = this.producerPolicies[policy].value
          }
        }
      }
      this.config(this.configData, 'policyDialog')
    },
    configConfirm () {
      this.$refs.configForm.$refs.form.validate((valid) => {
        if (valid) {
          this.configData = this.$refs.configForm.getFormData()
          this.config(this.configData, 'configDialog')
        }
      })
    },
    weightConfigConfirm () {
      let configData = {
        producerId: this.producerId,
        weight: this.$refs.weightForm.getWeights()
      }
      this.config(configData, 'weightDialog')
    },
    rateLimitConfirm () {
      let configData = this.configData
      configData.limitTps = this.$refs.rateLimit.tps
      configData.limitTraffic = this.$refs.rateLimit.traffic
      this.config(configData, 'rateLimitDialog')
    },
    sendMessageConfirm () {
      let formData = this.$refs.sendMessageForm.formData
      if (!formData.message) {
        this.$Message.error('消息体不能为空')
        return
      }
      let data = {
        topic: formData.topic,
        namespace: formData.namespace,
        app: formData.app,
        message: formData.message
      }
      apiRequest.post(this.urls.sendMessage, null, data, true).then((data) => {
        data.data = data.data || []
        if (data.code !== this.$store.getters.successCode) {
          this.$Dialog.error({
            content: '发送失败'
          })
        } else {
          this.$Message.success('发送成功')
          this.sendMessageDialog.visible = false
        }
      })
    },
    goSummaryChart (item) {
      if (this.monitorUrls && this.monitorUrls.summary) {
        window.open(replaceChartUrl(this.monitorUrls.summary, item.topic.namespace.code,
          item.topic.code, item.app.code))
      } else {
        apiRequest.get(apiUrl['monitor']['redirectUrl'] + '/' + this.monitorUIds.summary, {}, {}).then((data) => {
          let url = data.data || ''
          if (url.indexOf('?') < 0) {
            url += '?'
          } else if (!url.endsWith('?')) {
            url += '&'
          }
          url = url + 'var-topic=' + getTopicCode(item.topic, item.topic.namespace) + '&var-app=' + item.app.code
          window.open(url)
        })
      }
    },
    goDetailChart (item) {
      if (this.monitorUrls && this.monitorUrls.detail) {
        window.open(replaceChartUrl(this.monitorUrls.detail, item.topic.namespace.code,
          item.topic.code, item.app.code))
      } else {
        apiRequest.get(apiUrl['monitor']['redirectUrl'] + '/' + this.monitorUIds.detail, {}, {}).then((data) => {
          let url = data.data || ''
          if (url.indexOf('?') < 0) {
            url += '?'
          } else if (!url.endsWith('?')) {
            url += '&'
          }
          url = url + 'var-topic=' + getTopicCode(item.topic, item.topic.namespace) + '&var-app=' + item.app.code
          window.open(url)
        })
      }
    },
    goPerformanceChart (item) {
      if (this.monitorUrls && this.monitorUrls.performance) {
        window.open(replaceChartUrl(this.monitorUrls.performance, item.topic.namespace.code,
          item.topic.code, item.app.code))
      } else {
        apiRequest.get(apiUrl['monitor']['redirectUrl'] + this.monitorUIds.performance, {}, {}).then((data) => {
          if (data.data) {
            let url = data.data
            if (url.indexOf('?') < 0) {
              url += '?'
            } else if (!url.endsWith('?')) {
              url += '&'
            }
            url = url + 'var-topic=' + getTopicCode(item.topic, item.topic.namespace) + '&var-app=' + item.app.code
            window.open(url)
          }
        })
      }
    },
    goCompareChart (item) {
      if (this.monitorUrls && this.monitorUrls.compare) {
        window.open(replaceChartUrl(this.monitorUrls.compare, item.topic.namespace.code,
          item.topic.code, item.app.code))
      } else {
        apiRequest.get(apiUrl['monitor']['redirectUrl'] + '/' + this.monitorUIds.compare, {}, {}).then((data) => {
          let url = data.data || ''
          if (url.indexOf('?') < 0) {
            url += '?'
          } else if (!url.endsWith('?')) {
            url += '&'
          }
          url = url + 'var-topic=' + getTopicCode(item.topic, item.topic.namespace) + '&var-app=' + item.app.code
          window.open(url)
        })
      }
    },
    getMonitor (row, index) {
      let data = {
        topic: {
          id: row.topic.id,
          code: row.topic.code
        },
        namespace: {
          id: row.namespace.id,
          code: row.namespace.code
        },
        app: {
          id: row.app.id,
          code: row.app.code
        },
        type: this.type
      }
      apiRequest.postBase(this.urls.getMonitor, {}, data, false).then((data) => {
        this.tableData.rowData[index] = Object.assign(row, data.data || [])
        if (this.tableData.rowData[index].connections === undefined) {
          this.tableData.rowData[index].connections = 'unknown'
        }
        if (this.tableData.rowData[index].enQuence === undefined) {
          this.tableData.rowData[index].enQuence = 'unknown'
        }
        this.$set(this.tableData.rowData, index, this.tableData.rowData[index])
      })
    },
    // 实现懒加载的getList方法
    getList () {
      this.tableData.rowData = []
      // 查询数据库里的数据
      this.showTablePin = true
      let query = {}
      query.keyword = this.keyword
      let data = {
        pagination: {
          page: this.page.page,
          size: this.page.size
        },
        query: query
      }
      for (let i in this.search) {
        if (this.search.hasOwnProperty(i)) {
          data.query[i] = this.search[i]
        }
      }
      apiRequest.post(this.urls.search, {}, data).then((data) => {
        data.data = (data.data || []).sort((a, b) => sortByProducer(a, b))
        data.pagination = data.pagination || {
          totalRecord: data.data.length
        }
        this.page.total = data.pagination.totalRecord
        this.page.page = data.pagination.page
        this.page.size = data.pagination.size
        if (data.data.length > this.page.size) {
          this.tableData.rowData = data.data.slice(0, this.page.size)
          this.curIndex = this.page.size - 1
        } else {
          this.tableData.rowData = data.data
          this.curIndex = data.data.length - 1
        }
        this.cacheList = data.data
        this.showTablePin = false
        for (let i = 0; i < this.tableData.rowData.length; i++) {
          this.getMonitor(this.tableData.rowData[i], i)
        }
      })
    },
    config (configData, dialog) {
      apiRequest.post(this.configDialog.urls.addOrUpdae, {}, configData).then((data) => {
        if (data.code !== 200) {
          this.$Dialog.error({
            content: '配置失败'
          })
        } else {
          this[dialog].visible = false
          this.getList()
        }
      }).catch(() => {
      })
    },
    // 滚动事件触发下拉加载
    getRestList () {
      if (this.curIndex < this.cacheList.length - 1) {
        for (let i = 0; i < this.page.size; i++) {
          if (this.curIndex < this.cacheList.length - 1) {
            this.curIndex += 1
            if (!this.tableData.rowData.includes(this.cacheList[this.curIndex])) {
              this.tableData.rowData.push(this.cacheList[this.curIndex])
              this.getMonitor(this.tableData.rowData[this.curIndex], this.curIndex)
            }
          } else {
            break
          }
        }
      }
    },
    producerPolicyDetail (item) {
      this.configData = item.config || {}
      this.configData['producerId'] = item.id
      this.producerPolicies = []
      if (item.config.qosLevel !== undefined) {
        this.producerPolicy.qosLevel = item.config.qosLevel
      } else {
        this.producerPolicy.qosLevel = undefined
      }
      if (item.config.region !== undefined) {
        this.producerPolicy.region = item.config.region
      } else {
        this.producerPolicy.region = undefined
      }
      for (let policy in this.producerPolicy) {
        this.producerPolicies.push({
          key: policy,
          value: this.producerPolicy[policy]
        })
      }
      this.policyDialog.visible = true
    }
  },
  mounted () {
    // this.getList();
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .label{text-align: right; line-height: 32px;}
  .val{}
  .load-btn { margin-right: 50px;margin-top: -100px;position: relative}
  .maxDialogHeight /deep/ .dui-dialog__body {
    height: 650px;
  }
</style>
