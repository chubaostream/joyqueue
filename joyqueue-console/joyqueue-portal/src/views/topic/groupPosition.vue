<template>
  <div>
    <d-button type="primary" @click="getList" class="button">刷新
      <icon name="refresh-cw" style="margin-left: 3px;"></icon>
    </d-button>
    <d-table :columns="tableData.colData" :data="tableData.rowData" style="height: 400px;overflow-y:auto">
    </d-table>
  </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest.js'
import myTable from '../../components/common/myTable.vue'
import crud from '../../mixins/crud.js'
import PositionExpand from './positionExpand'

export default {
  name: 'group-position',
  components: {
    PositionExpand,
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
      partitionGroup: {},
      urls: {
        search: '/monitor/broker/group/metric'
      },
      searchData: {
        keyword: ''
      },
      tableData: {
        rowData: [{}],
        colData: [
          {
            type: 'expand',
            width: 50,
            render: (h, params) => {
              return h(PositionExpand, {
                props: {
                  rowData: params.row.partitionPositionList
                }})
            }
          },
          {
            title: '分区组',
            key: 'partitionGroup'
          },
          {
            title: 'brokerId',
            key: 'brokerId'
          },
          {
            title: '位置',
            key: 'rightPosition'
          },
          {
            title: '是否leader',
            key: 'leader'
          },
          {
            title: '位移差距',
            key: 'rightPositionInterval'
          }
        ]
        // btns: [
        // ]
      },
      multipleSelection: []
    }
  },
  methods: {
    handleSelectionChange (val) {
      this.multipleSelection = val
      this.$emit('', val)
    },
    leader (item) {
      let data = item
      apiRequest.post(this.urls.leader, {}, data).then((data) => {
        if (data.code === 200) {
          this.$Message.success('更新成功')
          this.getList()
        } else {
          this.$Message.error('更新失败')
        }
      })
    },
    // 查询
    getList () {
      // 1. 查询数据库里的数据
      this.showTablePin = true
      let data = {
        partitionGroup: this.partitionGroup.groupNo,
        subscribe: {
          topic: this.partitionGroup.topic,
          namespace: this.partitionGroup.namespace
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
      })
    }
  },
  mounted () {
    this.partitionGroup = this.data
    this.getList()
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .label{text-align: right; line-height: 32px;}
  .val{}
</style>
