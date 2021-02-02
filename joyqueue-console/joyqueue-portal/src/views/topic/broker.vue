<template>
  <div>
    <div class="ml20 mt30">
      <d-button-group>
        <slot name="extendBtn"></slot>
        <d-button v-if="showBrokerChart" @click="goBrokerChart" class="button">Broker监控
          <icon name="cpu" style="margin-left: 3px;"/>
        </d-button>
        <d-button v-if="showHostChart" @click="goHostChart" class="button">主机监控
          <icon name="monitor" style="margin-left: 3px;"/>
        </d-button>
        <d-button type="primary" @click="getList" class="button">刷新
          <icon name="refresh-cw" style="margin-left: 3px;"></icon>
        </d-button>
      </d-button-group>
    </div>
    <my-table :data="tableData" :showPin="showTablePin" style="height: 400px;overflow-y:auto" :showPagination="false"/>
  </div>
</template>

<script>
import MyTable from '../../components/common/myTable'
import crud from '../../mixins/crud.js'
import apiRequest from '../../utils/apiRequest.js'
import {brokerPermissionTypeRender, brokerRetryTypeRender} from '../../utils/common.js'
import {timeStampToString} from '../../utils/dateTimeUtils'

export default {
  name: 'broker',
  components: {MyTable},
  mixins: [crud],
  props: {
    topicId: {
      type: String,
      default: ''
    },
    showBrokerChart: {
      type: Boolean,
      default: true
    },
    showHostChart: {
      type: Boolean,
      default: true
    }
  },
  data () {
    return {
      urls: {
        search: `/broker/findByTopic`,
        getUrl: `/grafana/getRedirectUrl`,
        findDetail: '/monitor/broker/findBrokerDetail/'
      },
      tableData: {
        rowData: [],
        colData: [
          {
            title: 'ID',
            key: 'id',
            width: '12%'
          },
          {
            title: 'IP:端口',
            key: 'ip',
            width: '15%',
            render: (h, params) => {
              console.log(params.item)
              const ip = params.item.ip
              const port = params.item.port
              return h('label', {
                style: {
                  color: '#3366FF'
                },
                on: {
                  click: () => {
                    this.$router.push({
                      path: '/' + this.$i18n.locale + '/setting/brokerMonitor',
                      query: {
                        brokerId: params.item.id,
                        brokerIp: ip,
                        brokerPort: port
                      }
                    })
                  },
                  mousemove: (event) => {
                    event.target.style.cursor = 'pointer'
                  }
                }
              }, `${ip}:${port}`)
            }
          },
          {
            title: '对外Ip:端口',
            key: 'externalIp',
            width: '15%',
            formatter (row) {
              if (row.externalIp) {
                return row.externalIp + ':' + row.externalPort
              }
              return ''
            }
          },
          {
            title: '机房 [编码/名称]',
            key: 'dataCenter.code',
            width: '10%',
            formatter (item) {
              if (item.dataCenter) {
                return item.dataCenter.code + '/' + item.dataCenter.name
              }
            }
          },
          {
            title: 'broker分组',
            key: 'group.code',
            width: '10%'
          },
          {
            title: '版本',
            key: 'startupInfo.version',
            width: '10%'
          },
          {
            title: '启动时间',
            key: 'startupInfo.startupTime',
            width: '10%',
            formatter (item) {
              if (item.startupInfo) {
                return timeStampToString(item.startupInfo.startupTime)
              }
            }
          },
          {
            title: '重试类型',
            key: 'retryType',
            width: '12%',
            render: (h, params) => {
              return brokerRetryTypeRender(h, params.item.retryType)
            }
          },
          {
            title: '权限',
            key: 'permission',
            width: '12%',
            render: (h, params) => {
              return brokerPermissionTypeRender(h, params.item.permission)
            }
          }
        ]
      },
      monitorUId: {
        broker: this.$store.getters.uIds.broker,
        host: this.$store.getters.uIds.host
      }
    }
  },
  methods: {
    getList () {
      this.showTablePin = true
      apiRequest.postBase(this.urls.search, {}, this.topicId, false).then((data) => {
        data.data = data.data || []
        this.tableData.rowData = data.data
        for (let i = 0; i < this.tableData.rowData.length; i++) {
          this.getDetail(this.tableData.rowData[i], i)
        }
        this.showTablePin = false
      })
    },
    getDetail (row, index) {
      apiRequest.getBase(this.urls.findDetail + row.id, {}, false)
        .then((data) => {
          this.tableData.rowData[index] = Object.assign(row, data.data || [])
          this.$set(this.tableData.rowData, index, this.tableData.rowData[index])
        })
    },
    goBrokerChart () {
      apiRequest.get(this.urls.getUrl + '/' + this.monitorUId.broker, {}, {}).then((data) => {
        let url = data.data || ''
        if (url.indexOf('?') < 0) {
          url += '?'
        } else if (!url.endsWith('?')) {
          url += '&'
        }
        url = url + 'var-topic=' + this.topicId
        window.open(url)
      })
    },
    goHostChart () {
      apiRequest.get(this.urls.getUrl + '/' + this.monitorUId.host, {}, {}).then((data) => {
        let url = data.data || ''
        if (url.indexOf('?') < 0) {
          url += '?'
        } else if (!url.endsWith('?')) {
          url += '&'
        }
        url = url + 'var-topic=' + this.topicId
        window.open(url)
      })
    }
  }
}
</script>

<style scoped>

</style>
