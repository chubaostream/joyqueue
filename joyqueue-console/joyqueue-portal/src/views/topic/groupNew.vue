<template>
  <div>
    <div style="margin-left: 20px;">
      <d-form :model="addData" inline label-width="850">
        <d-form-item label="分区数量：" required style="margin-right: 20px;">
          <d-input v-model="addData.partitions" oninput="value = value.trim()" placeholder="请输入数字"  style="width: 150px"></d-input>
        </d-form-item>
        <d-form-item label="选举类型：" required style="margin-right: 20px;">
          <d-select v-model="addData.electType" value="0" placeholder="请选择" style="width: 100px">
            <d-option :value="0">raft</d-option>
            <d-option :value="1">fix</d-option>
          </d-select>
        </d-form-item>
        <d-form-item label="推荐leader：" required style="margin-right: 20px;">
          <d-select v-model="addData.recLeader" placeholder="请先勾选表格中broker" style="width: 170px">
            <d-option v-for="item in addData.replicaGroups" :value="item.brokerId" :key="item.id">{{item.brokerId}}</d-option>
          </d-select>
        </d-form-item>
        <d-form-item label="IP/ID搜索:" required style="margin-right: 20px;">
          <d-input v-model="searchData.keyword" oninput="value = value.trim()" @on-enter="getList" placeholder="请输入IP/ID"  style="width: 150px">
            <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
          </d-input>
        </d-form-item>
        <d-form-item label="Broker分组：" required style="margin-right: 20px;">
          <d-input v-model="searchData.group" oninput="value = value.trim()" @on-enter="getList" placeholder="请输入Broker分组"  style="width: 150px">
            <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
          </d-input>
        </d-form-item>
        <d-form-item>
          <d-button type="primary" :disabled="btnDisabled" @click="addNewPartitionGroup">
            添加
            <icon name="plus-circle" style="margin-left: 5px;"></icon>
          </d-button>
        </d-form-item>
      </d-form>
    </div>
    <my-table :optional="true" :data="tableData" :showPin="showTablePin" :page="page"
              @on-size-change="handleSizeChange" style="height: 400px;overflow-y:auto"
              @on-current-change="handleCurrentChange" @on-selection-change="handleSelectionChange"/>
  </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest.js'
import myTable from '../../components/common/myTable.vue'
import crud from '../../mixins/crud.js'

export default {
  name: 'group-new',
  components: {
    myTable
  },
  props: {
    data: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  mixins: [ crud ],
  data () {
    return {
      btnDisabled: false,
      firstOpen: true,
      topic: {},
      namespace: {},
      addData: {
        topic: {},
        namespace: {},
        replicaGroups: [],
        partitions: 1,
        electType: 0,
        recLeader: ''
      },
      page: {
        page: 1,
        size: 10,
        total: 100
      },
      urls: {
        search: '/partitionGroupReplica/searchBrokerToAddNew',
        add: `/partitionGroup/add`,
        findDCByIps: '/dataCenter/findByIps',
        findTopicCount: '/monitor/findTopicCount'
      },
      searchData: {
        keyword: '',
        group: ''
      },
      tableData: {
        rowData: [{}],
        colData: [
          {
            title: 'brokerId',
            key: 'id',
            width: '15%'
          },
          {
            title: 'Broker分组',
            key: 'group.code',
            width: '15%'
          },
          {
            title: 'IP',
            key: 'ip',
            width: '15%'
          },
          {
            title: '端口',
            key: 'port',
            width: '10%'
          },
          {
            title: '宿主机',
            key: 'node',
            width: '20%'
          },
          {
            title: '主题数',
            key: 'topicCount',
            width: '10%'
          }
        ]
      },
      multipleSelection: []
    }
  },
  methods: {
    handleSelectionChange (val) {
      let brokerIds = []
      for (let i = 0; i < val.length; i++) {
        brokerIds.push({brokerId: val[i].id})
      }
      this.multipleSelection = val
      this.addData.replicaGroups = brokerIds
      if (this.addData.replicaGroups.length === 0) {
        this.addData.recLeader = ''
      } else {
        this.addData.recLeader = this.addData.replicaGroups[0].brokerId
      }
    },
    findTopicCount (i) {
      if (this.tableData.rowData[i].id) {
        let brokerId = this.tableData.rowData[i].id
        apiRequest.get(this.urls.findTopicCount + '?brokerId=' + brokerId, {}).then((data) => {
          this.tableData.rowData[i].topicCount = data.data
          this.$set(this.tableData.rowData, i, this.tableData.rowData[i])
        })
      }
    },
    findDCByIps (i, ip) {
      let data = []
      data.push(ip)
      apiRequest.post(this.urls.findDCByIps, {}, data).then((data) => {
        if (data && data.data) {
          let dcName = data.data.map(item => item.name).sort()
          let str = ''
          for (let i in dcName) {
            if (dcName.hasOwnProperty(i)) {
              str = str + dcName[i] + ' '
            }
          }
          this.tableData.rowData[i].dataCenters = str
          this.$set(this.tableData.rowData, i, this.tableData.rowData[i])
        }
      })
    },
    // 查询
    getList () {
      // 1. 查询数据库里的数据
      if (this.searchData.keyword && this.searchData.group) {
        this.$Message.error('IP/ID不能和broker分组同时进行搜索')
        return
      }
      this.showTablePin = true
      let data = {
        pagination: {
          page: this.page.page,
          size: this.page.size
        },
        query: {
          topic: this.topic,
          namespace: this.namespace,
          keyword: this.searchData.keyword
        }
      }
      if (this.data.ip) {
        data.query.keyword = this.data.ip
        delete this.data.ip
        this.urlOrigin.search = '/partitionGroupReplica/searchBrokerToAddNewDefault'
      } else {
        data.query.keyword = this.searchData.keyword
        this.urlOrigin.search = '/partitionGroupReplica/searchBrokerToAddNew'
      }
      if (this.searchData.group) {
        data.query.topic.brokerGroup = this.searchData.group
      } else {
        delete data.query.topic.brokerGroup
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
        for (let i in this.tableData.rowData) {
          if (this.tableData.rowData.hasOwnProperty(i)) {
            this.findTopicCount(i)
            this.findDCByIps(i, this.tableData.rowData[i].ip)
          }
        }
        if (this.firstOpen && this.tableData.rowData.length > 0) {
          let groupCode = ''
          if (this.tableData.rowData[0].group) {
            groupCode = this.tableData.rowData[0].group.code
          }
          let unique = true
          for (let i in this.tableData.rowData) {
            if (this.tableData.rowData.hasOwnProperty(i)) {
              if (this.tableData.rowData[i].group) {
                if (this.tableData.rowData[i].group.code.indexOf(groupCode) < 0) {
                  unique = false
                  break
                }
              }
            }
          }
          if (unique) {
            this.searchData.group = groupCode
          } else {
            this.searchData.group = ''
          }
        }
        this.firstOpen = false
        this.showTablePin = false
      })
    },
    addNewPartitionGroup () {
      this.btnDisabled = true
      let addData = this.addData
      if (!addData.partitions) {
        this.$Message.error('请输入分区数量')
        this.btnDisabled = false
        return
      }
      if (!addData.recLeader) {
        this.$Message.error('请选择推荐leader')
        this.btnDisabled = false
        return
      }
      let set = new Set()
      for (let broker of this.multipleSelection) {
        set.add(broker.node)
      }
      if (set.size < this.multipleSelection.length) {
        this.$Message.error('选择的Broker云主机不能重复')
        return
      }
      let _this = this
      apiRequest.post(this.urls.add, {}, addData).then((data) => {
        if (data && data.data) {
          _this.$emit('on-partition-group-change')
          _this.$emit('on-dialog-cancel')
        }
        this.btnDisabled = false
      })
    }
  },
  mounted () {
    this.topic = this.data.topic
    this.namespace = this.data.namespace
    this.addData.topic = this.data.topic
    this.addData.namespace = this.data.namespace
    this.getList()
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .label{text-align: right; line-height: 32px;}
  .val{}
</style>
