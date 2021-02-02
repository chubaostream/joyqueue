<template>
  <div>
    <div class="ml20 mt10">
      <d-input v-model="searchData.keyword" placeholder="请输入要查询的IP/名称" class="left"
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
    </div>
    <my-table :optional="true" :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange"
              @on-current-change="handleCurrentChange" @on-selection-change="handleSelectionChange" @on-add="add">
    </my-table>

  </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest.js'
import myTable from '../../components/common/myTable.vue'
import crud from '../../mixins/crud.js'
export default {
  name: 'group-scale',
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
      firstOpen: true,
      urls: {
        search: '/partitionGroupReplica/searchBrokerToScale',
        addUrl: '/partitionGroupReplica/add'
      },
      searchData: {
        keyword: '',
        group: ''
      },
      tableData: {
        rowData: [{}],
        colData: [
          {
            title: 'Broker分组',
            key: 'group.code',
            width: '20%'
          },
          {
            title: 'ID',
            key: 'id',
            width: '20%'
          },
          {
            title: 'IP',
            key: 'ip',
            width: '20%'
          },
          {
            title: '宿主机',
            key: 'node',
            width: '20%'
          },
          {
            title: '端口',
            key: 'port',
            width: '20%'
          }
        ],
        btns: [
          {
            txt: '添加',
            method: 'on-add'
          }
        ]
      },
      multipleSelection: []
    }
  },
  methods: {
    // 扩容
    add (item) {
      let parmas = {
        topic: this.data.topic,
        namespace: this.data.namespace,
        groupNo: this.data.groupNo,
        brokerId: item.id
      }
      let _this = this
      this.$Dialog.confirm({
        title: '提示：请选择同分组broker',
        content: '确定要扩容吗？'
      }).then(() => {
        apiRequest.post(_this.urls.addUrl, null, parmas).then((data) => {
          _this.getList()
          _this.$emit('on-partition-group-change')
        })
      }).catch(() => {
      })
    },
    handleSelectionChange (val) {
      this.multipleSelection = val
      this.$emit('on-choosed-broker', val)
    },
    // 查询
    getList () {
      // 1. 查询数据库里的数据
      if (this.searchData.group && this.searchData.keyword) {
        this.$Message.error('查询关键字和broker分组不能同时输入')
        return
      }
      this.showTablePin = true
      let data = {
        pagination: {
          page: this.page.page,
          size: this.page.size
        },
        query: {
          topic: this.data.topic,
          namespace: this.data.namespace,
          groupNo: this.data.groupNo
        }
      }
      if (this.data.ip) {
        data.query.keyword = this.data.ip
        delete this.data.ip
        this.urlOrigin.search = '/partitionGroupReplica/searchBrokerToScaleDefault'
      } else {
        data.query.keyword = this.searchData.keyword
        this.urlOrigin.search = '/partitionGroupReplica/searchBrokerToScale'
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
