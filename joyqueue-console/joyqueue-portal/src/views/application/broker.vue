<template>
  <div>
    <div class="ml20 mt30">
      <d-input v-model="searchData.name" oninput="value = value.trim()" placeholder="请输入Broker分组/IP" class="left mr10" style="width:300px" @on-enter="getList">
        <span slot="prepend">关键词</span>
        <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
      </d-input>
    </div>
    <my-table :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange"
              @on-current-change="handleCurrentChange">
    </my-table>
  </div>
</template>

<script>
import myTable from '../../components/common/myTable.vue'
import myDialog from '../../components/common/myDialog.vue'
import crud from '../../mixins/crud.js'
import apiRequest from '../../utils/apiRequest.js'
import {brokerRoleTypeRender, brokerPermissionTypeRender, brokerSyncModeTypeRender} from '../../utils/common.js'
import {timeStampToString} from '../../utils/dateTimeUtils'

export default {
  name: 'appBroker',
  components: {
    myTable,
    myDialog
  },
  mixins: [ crud ],
  data () {
    return {
      urls: {
        search: `/broker/search`
      },
      searchData: {
        appId: this.$route.query.id,
        appCode: this.$route.query.code,
        keyword: ''
      },
      searchRules: {
      },
      tableData: {
        rowData: [],
        colData: [
          {
            title: 'Broker分组',
            key: 'brokerGroup.code'
          },
          {
            title: 'IP:端口',
            key: 'broker.ip',
            width: '14%',
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
            title: '机房 (编码/名称)',
            key: 'dataCenter.code',
            width: '10%',
            formatter (item) {
              if (item.dataCenter) {
                return item.dataCenter.code + '/' + item.dataCenter.name
              }
            }
          },
          {
            title: '角色',
            key: 'role',
            width: '8%',
            render: (h, params) => {
              return brokerRoleTypeRender(h, params.item.role)
            }
          },
          {
            title: '复制方式',
            key: 'syncMode',
            render: (h, params) => {
              return brokerSyncModeTypeRender(h, params.item.syncMode)
            }
          },
          {
            title: '版本',
            key: 'startupInfo.version',
            width: '10%'
          },
          {
            title: '开机时间',
            key: 'startupInfo.startupTime',
            width: '10%',
            formatter (item) {
              return timeStampToString(item.startupInfo.startupTime)
            }
          },
          {
            title: '重试类型',
            key: 'retryType',
            render: (h, params) => {
              let label
              switch (params.item.retryType) {
                case 'DB':
                  label = '直连数据库'
                  break
                case 'REMOTE':
                  label = '访问远程服务'
                  break
              }
              return h('label', {}, label)
            }
          },
          {
            title: '权限',
            key: 'permission',
            render: (h, params) => {
              return brokerPermissionTypeRender(h, params.item.broker.permission)
            }
          },
          {
            title: '备注',
            key: 'description'
          }
        ]
      }
    }
  },
  computed: {
  },
  methods: {
    // 查询
    getList () {
      // 1. 查询数据库里的数据
      this.showTablePin = true
      let data = {
        pagination: {
          page: this.page.page,
          size: this.page.size
        },
        query: {
          topic: this.partitionGroup.topic,
          namespace: this.partitionGroup.namespace,
          groupNo: this.partitionGroup.groupNo
        }
      }
      apiRequest.post(this.urlOrigin.search, {}, data).then((data) => {
        data.data = data.data || []
        data.pagination = data.pagination || {
          totalRecord: data.data.length
        }
        this.page.total = data.pagination.totalRecord
        this.page.page = data.pagination.page
        this.page.size = data.pagination.size
        this.tableData.rowData = data.data
        this.showTablePin = false
        for (let i = 0; i < this.tableData.rowData.length; i++) {
          this.getDetail(this.tableData.rowData[i], i)
        }
      })
    }
  },
  mounted () {
    this.getList()
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.label{text-align: right; line-height: 32px;}
.val{}
</style>
