<template>
  <div>
    <div class="ml20 mt30">
      <d-input v-model="searchData.keyword" oninput="value = value.trim()" placeholder="请输入英文名" class="left mr10"
               style="width:300px" @on-enter="getList">
        <span slot="prepend">英文名</span>
        <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
      </d-input>
      <d-button type="primary" @click="openDialog('addDialog')">添加用户<icon name="plus-circle" style="margin-left: 5px;"></icon></d-button>
    </div>
    <my-table :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange" @on-current-change="handleCurrentChange"
              @on-selection-change="handleSelectionChange" @on-edit="edit" @on-del="del">
    </my-table>

    <!--添加用户-->
    <my-dialog :dialog="addDialog" @on-dialog-confirm="submit()" @on-dialog-cancel="addCancel()">
      <grid-row class="mb10">
        <grid-col :span="3" class="label">英文名:</grid-col>
        <grid-col :span="1"/>
        <grid-col :span="14" class="val">
          <d-input v-model="addData.code" oninput="value = value.trim()"></d-input>
        </grid-col>
      </grid-row>
    </my-dialog>
    <!--角色修改-->
    <my-dialog :dialog="editDialog" @on-dialog-confirm="editConfirm()" @on-dialog-cancel="editCancel()">
      <grid-row class="mb10">
        <grid-col :span="5" class="label">英文名：</grid-col>
        <grid-col :span="1"/>
        <grid-col :span="14" class="val">{{editData.code}}</grid-col>
      </grid-row>
      <grid-row class="mb10">
        <grid-col :span="5" class="label">角色：</grid-col>
        <grid-col :span="1"/>
        <grid-col :span="14" class="val">
          <d-radio v-model="editData.role" name="radio" :label="1">管理员</d-radio>
          <d-radio v-model="editData.role" name="radio" :label="0">用户</d-radio>
        </grid-col>
      </grid-row>
      <grid-row class="mb10">
        <grid-col :span="5" class="label">密码：</grid-col>
        <grid-col :span="1"/>
        <grid-col :span="14">
          <d-input v-model="editData.password" placeholder="请输入4位密码"></d-input>
        </grid-col>
      </grid-row>
    </my-dialog>
  </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest.js'
import myTable from '../../components/common/myTable.vue'
import myDialog from '../../components/common/myDialog.vue'
import crud from '../../mixins/crud.js'
import GridCol from '../../components/grid/col'
import GridRow from '../../components/grid/row'
export default {
  name: 'userManage',
  components: {
    GridRow,
    GridCol,
    myTable,
    myDialog
  },
  mixins: [ crud ],
  data () {
    return {
      searchData: {
        keyword: ''
      },
      searchRules: {
      },
      tableData: {
        rowData: [],
        colData: [
          {
            title: '英文名',
            key: 'code'
          },
          {
            title: '中文名',
            key: 'code'
          },
          {
            title: '密码',
            key: 'password'
          },
          {
            title: '所属部门',
            key: 'orgName'
          },
          {
            title: '邮箱',
            key: 'email'
          },
          {
            title: '手机号',
            key: 'mobile'
          },
          {
            title: '是否管理员',
            key: 'role',
            render: (h, params) => {
              let txt = params.item.role === 0 ? '用户' : '管理员'
              return h('span', txt)
            }
          }
          // {
          //   title:'状态',
          //   key: 'status',
          //   render:(h, params) => {
          //     let txt = params.item.status == 0 ? '禁用' : '启用'
          //     let color = params.item.status == 0 ? 'warning' : 'success'
          //     return h('DButton', {
          //       props: {
          //         size: 'small',
          //         borderless: true,
          //         color: color
          //       }
          //     }, txt)
          //   }
          // }
        ],
        // 表格操作，如果需要根据特定值隐藏显示， 设置bindKey对应的属性名和bindVal对应的属性值
        btns: [
          {
            txt: '修改  ',
            method: 'on-edit'
          },
          // {
          //   txt: '禁用',
          //   method: 'on-disable',
          //   bindKey: 'status',
          //   bindVal: 1
          // },
          // {
          //   txt: '启用',
          //   method: 'on-enable',
          //   bindKey: 'status',
          //   bindVal: 0
          // }
          {
            txt: '删除',
            method: 'on-del',
            bindKey: 'role',
            bindVal: 0
          }
        ]
      },
      multipleSelection: [],
      addDialog: {
        visible: false,
        title: '添加用户',
        showFooter: true
      },
      addData: {
        code: ''
      },
      editDialog: {
        visible: false,
        title: '角色修改',
        showFooter: true
      },
      editData: {}
    }
  },
  computed: {
    syncUrl () {
      return this.urlOrigin.sync
    }
  },
  methods: {
    openDialog (dialog) {
      this[dialog].visible = true
      this.addData.code = ''
    },
    syncConfirm () {
      apiRequest.get(this.syncUrl, this.syncDialogData).then((data) => {
        this.syncDialog.visible = false
        this.$Dialog.success({
          content: '同步成功'
        })
        this.getList()
      })
    },
    syncCancel (index, row) {
      this.syncDialog.visible = false
    },
    handleEdit (item, index) {
      console.log(item)
    },
    // 启用/禁用
    state (item, val) {
      let editData = item
      editData.status = val// 启用1，禁用0
      apiRequest.put(this.urlOrigin.edit + '/' + editData.id, {}, editData).then((data) => {
        this.$Dialog.success({
          content: '启用成功'
        })
        this.getList()
      })
    },
    // 启用
    enable (item) {
      this.state(item, 1)
    },
    // 禁用
    disable (item) {
      this.state(item, 0)
    },
    submit () {
      if (!this.addData || !this.addData.code) {
        this.$Message.error('请输入英文名')
        return false
      }
      this.addConfirm()
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
