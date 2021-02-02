<template>
  <div>
    <d-input v-model="searchData.keyword" placeholder="请输入要查询的IP/ID/标签" class="left"
             oninput="value = value.trim()"
              style="width: 300px" @on-enter="getList">
      <span slot="prepend">关键词</span>
      <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
    </d-input>
    <d-input v-model="searchData.group" placeholder="请输入要查询的Broker分组" class="left"
             oninput="value = value.trim()"
             style="width: 300px" @on-enter="getList">
      <span slot="prepend">Broker分组</span>
      <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
    </d-input>
    <my-table :optional="true" :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange"
              @on-current-change="handleCurrentChange" @on-selection-change="handleSelectionChange">
    </my-table>
  </div>
</template>

<script>
import myTable from '../../components/common/myTable.vue'
import crud from '../../mixins/crud.js'
import apiRequest from '../../utils/apiRequest.js'
import {timeStampToString} from '../../utils/dateTimeUtils'

export default {
  name: 'add-broker',
  components: {
    myTable
  },
  props: {
    // data: {
    //   type: Object,
    //   default () {
    //     return {}
    //   }
    // },
    urls: {
      type: Object
    },
    colData: {
      type: Array
    },
    btns: {
      type: Array
    }
  },
  mixins: [ crud ],
  data () {
    return {
      startInfo: '/monitor/start',
      searchData: {
        keyword: '',
        group: '',
        brokerGroupId: -1
      },
      tableData: {
        rowData: [{}],
        colData: this.colData,
        btns: this.btns
      },
      multipleSelection: [],
      selectedBrokers: []
    }
  },
  methods: {
    handleSelectionChange (val) {
      this.multipleSelection = val
      this.$emit('on-choosed-broker', val)
    },
    getListByGroup (brokerGroupId) {
      this.getListByGroupAndbrokers(brokerGroupId, undefined)
    },
    onBrokerLoadComplete (val) {
      this.$emit('on-broker-load-complete', val, tag => {
        for (let index = 0; index < tag.length; index++) {
          let row = {}
          Object.assign(row, tag[index])
          this.$set(this.tableData.rowData, index, row)
        }
      })
    },
    getListByGroupAndbrokers (brokerGroupId, brokers) {
      let query = {}
      if (!brokerGroupId) {
        query = {
          keyword: this.searchData.keyword
        }
      } else {
        this.searchData.brokerGroupId = brokerGroupId
        this.selectedBrokers = brokers
        query = {
          keyword: this.searchData.keyword,
          brokerGroupId: this.searchData.brokerGroupId
        }
      }

      let data = {
        pagination: {
          page: this.page.page,
          size: this.page.size
        },
        query: query
      }
      apiRequest.post(this.urls.search, {}, data).then((data) => {
        if (data === '') {
          return
        }
        data.data = data.data || []
        data.pagination = data.pagination || {
          totalRecord: data.data.length
        }
        this.page.total = data.pagination.totalRecord
        this.page.page = data.pagination.page
        this.page.size = data.pagination.size
        this.tableData.rowData = data.data
        this.tableData.rowData.forEach(element => {
          element['isChecked'] = false
          if (brokers && brokers.length > 0) {
            for (let broker of brokers) {
              if (broker.id === element.id) {
                element['isChecked'] = true
                break
              }
            }
          }
        })
        this.onBrokerLoadComplete(this.tableData.rowData)
      })
    },
    getBrokerByQueryGroup (brokerGroupId, brokerGroup) {
      let query = {}
      if (!brokerGroupId) {
        query = {
          keyword: this.searchData.keyword
        }
      } else {
        this.searchData.brokerGroupId = brokerGroupId
        query = {
          keyword: this.searchData.keyword,
          brokerGroupId: this.searchData.brokerGroupId
        }
        if (brokerGroup.length > 0) {
          query.group = {
            id: brokerGroup[0].id,
            code: brokerGroup[0].code,
            name: brokerGroup[0].name
          }
        }
      }

      let data = {
        pagination: {
          page: this.page.page,
          size: this.page.size
        },
        query: query
      }
      apiRequest.post(this.urls.search, {}, data).then((data) => {
        if (data === '') {
          return
        }
        data.data = data.data || []
        data.pagination = data.pagination || {
          totalRecord: data.data.length
        }
        this.page.total = data.pagination.totalRecord
        this.page.page = data.pagination.page
        this.page.size = data.pagination.size
        this.tableData.rowData = data.data
        this.tableData.rowData.forEach(element => {
          element['isChecked'] = false
          if (brokers && brokers.length > 0) {
            for (let broker of brokers) {
              if (broker.id === element.id) {
                element['isChecked'] = true
                break
              }
            }
          }
        })
        this.onBrokerLoadComplete(this.tableData.rowData)
      })
    },
    getBrokerStatus (rowData, i) {
      apiRequest.get(this.startInfo + '/' + rowData[i].id).then((data) => {
        if (data.code === 200) {
          this.tableData.rowData[i].startupTime = timeStampToString(data.data.startupTime)
          this.tableData.rowData[i].revision = data.data.revision
        } else {
          this.tableData.rowData[i].startupTime = '不存活'
        }
        this.$set(this.tableData.rowData, i, this.tableData.rowData[i])
      })
    },
    getList () {
      if (this.searchData.keyword && this.searchData.group) {
        this.$Message.error('验证不通过，ID/IP搜索和Broker分组编号不能同时搜索')
        return
      }
      if (this.searchData.keyword) {
        this.getListByGroupAndbrokers(this.searchData.brokerGroupId, this.selectedBrokers)
      } else {
        this.showTablePin = true
        let data = this.getSearchVal()
        apiRequest.post(this.urls.search, {}, data).then((data) => {
          data.data = data.data || []
          data.pagination = data.pagination || {
            totalRecord: data.data.length
          }
          this.page.total = data.pagination.totalRecord
          this.page.page = data.pagination.page
          this.page.size = data.pagination.size
          this.tableData.rowData = data.data
          for (let i = 0; i < this.tableData.rowData.length; i++) {
            this.getBrokerStatus(this.tableData.rowData, i)
          }
          this.showTablePin = false
        })
      }
    }
  },
  mounted () {
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .label{text-align: right; line-height: 32px;}
</style>
