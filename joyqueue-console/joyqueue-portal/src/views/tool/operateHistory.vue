<template>
  <div>
    <div class="ml20 mt30">
      <d-date-picker v-model="times" type="daterange" range-separator="至" start-placeholder="开始日期" class="left mr5"
                     end-placeholder="结束日期" value-format="timestamp" :default-time="['00:00:00', '23:59:59']"
                     style="width: 370px" @on-change="getList">
        <span slot="prepend">日期范围</span>
      </d-date-picker>
      <d-select v-model="searchData.type" class="left mr5"  style="width:213px">
        <span slot="prepend">操作类型</span>
        <d-option v-for="item in typeList" :value="item.key" :key="item.key">{{ item.value }}</d-option>
      </d-select>
      <d-input v-model="searchData.identity" oninput="value = value.trim()" class="left mr5" placeholder="关联Id" style="width:213px">
        <span slot="prepend">关联Id</span>
      </d-input>
      <d-input v-model="searchData.erp" oninput="value = value.trim()" placeholder="用户名" class="left mr5" style="width:213px">
        <span slot="prepend">用户名</span>
      </d-input>
      <d-button type="primary" color="success" @click="getList">查询</d-button>
    </div>
    <my-table :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange"
              @on-current-change="handleCurrentChange" @on-selection-change="handleSelectionChange"
              @on-del="del">
    </my-table>
  </div>
</template>

<script>
import myTable from '../../components/common/myTable.vue'
import crud from '../../mixins/crud.js'
import {timeStampToString} from '../../utils/dateTimeUtils'
import apiRequest from '../../utils/apiRequest.js'

export default {
  name: 'operate-history',
  components: {
    myTable
  },
  mixins: [ crud ],
  data () {
    return {
      searchData: {
        type: -1,
        beginTime: '',
        endTime: '',
        erp: this.$store.getters.loginUserName
      },
      typeList: [
        {key: -1, value: '请选择类型'},
        {key: 1, value: 'topic'},
        {key: 2, value: 'consumer'},
        {key: 3, value: 'producer'},
        {key: 4, value: 'config'},
        {key: 5, value: 'appToken'},
        {key: 6, value: 'group'},
        {key: 7, value: 'broker'},
        {key: 8, value: 'datacenter'},
        {key: 9, value: 'namespace'},
        {key: 10, value: 'partitiongroup'},
        {key: 11, value: 'replica'}
      ],
      searchRules: {
      },
      tableData: {
        rowData: [],
        colData: [
          {
            title: '操作时间',
            key: 'createTime',
            width: '12%',
            formatter (row) {
              return timeStampToString(row.createTime)
            }
          },
          {
            title: '类型',
            key: 'type',
            width: '10%',
            formatter (row) {
              switch (row.type) {
                case 1:
                  return 'topic'
                case 2:
                  return 'consumer'
                case 3:
                  return 'producer'
                case 4:
                  return 'config'
                case 5:
                  return 'appToken'
                case 6:
                  return 'group'
                case 7:
                  return 'broker'
                case 8:
                  return 'datacenter'
                case 9:
                  return 'namespace'
                case 10:
                  return 'partitiongroup'
                case 11:
                  return 'replica'
              }
            }
          },
          {
            title: '关联id',
            key: 'identity',
            width: '12%'
          },
          {
            title: '操作人',
            key: 'createBy.code',
            width: '10%',
            render: (h, params) => {
              return h('label', {}, params.item.createBy.code)
            }
          },
          {
            title: '操作描述',
            key: 'target',
            width: '55%'
          }
        ],
        // 表格操作，如果需要根据特定值隐藏显示， 设置bindKey对应的属性名和bindVal对应的属性值
        btns: [
          // {
          //   txt: '编辑',
          //   method: 'on-edit'
          // },
          // {
          //   txt: '删除',
          //   method: 'on-del'
          // }
        ]
      },
      multipleSelection: [],
      addDialog: {
        visible: false,
        title: '新建任务',
        showFooter: true
      },
      addData: {
        type: '',
        referId: 0,
        priority: 0,
        daemons: 1,
        url: '',
        cron: '',
        dispatchType: 0,
        retry: 1
      },
      editDialog: {
        visible: false,
        title: '编辑任务',
        showFooter: true
      },
      editData: {},
      times: []
    }
  },
  methods: {
    validate () {
      if (!this.$store.getters.isAdmin && !this.searchData.erp) {
        this.$Message.error('用户名不能为空')
        return false
      }

      if (!this.$store.getters.isAdmin && this.searchData.erp !== this.$store.getters.loginUserName) {
        this.$Message.error('用户名必须为登录用户本人')
        return false
      }

      return true
    },
    getList () {
      this.searchData.beginTime = this.times[0]
      this.searchData.endTime = this.times[1]
      if (this.validate()) {
        this.showTablePin = true
        let data = this.getSearchVal()
        apiRequest.post(this.urlOrigin.search, {}, data).then((data) => {
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
          this.showTablePin = false
        })
      }
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
