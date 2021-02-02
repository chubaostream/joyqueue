<template>
  <div>
    <div class="ml20 mt30">
      <d-input v-model="searchData.keyword" oninput="value = value.trim()" placeholder="请输入Erp或中文名" class="input" @on-enter="getList">
        <span slot="prepend">关键词</span>
        <icon name="search" size="14" color="#CACACA" slot="suffix" @click="getList"></icon>
      </d-input>
      <d-button-group>
        <d-button @click="openDialog('addDialog')" class="button">添加
          <icon name="plus-circle" style="margin-left: 3px;"></icon>
        </d-button>
        <d-button type="primary" @click="getList" class="button">刷新
          <icon name="refresh-cw" style="margin-left: 3px;"></icon>
        </d-button>
      </d-button-group>
    </div>
    <my-table :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange"
                  @on-current-change="handleCurrentChange" @on-selection-change="handleSelectionChange"
                  @on-del="del" @on-owner="setOwner">
        </my-table>

    <!--添加用户-->
    <my-dialog :dialog="addDialog" @on-dialog-confirm="addConfirm()" @on-dialog-cancel="addCancel()">
        <grid-row class="mb10">
          <grid-col :span="3" class="label">英文名:</grid-col>
          <grid-col :span="1"/>
          <grid-col :span="14" class="val">
            <d-input v-model="addData.user.code" oninput="value = value.trim()"></d-input>
          </grid-col>
        </grid-row>
    </my-dialog>
  </div>
</template>

<script>
import myTable from '../../components/common/myTable.vue'
import myDialog from '../../components/common/myDialog.vue'
import crud from '../../mixins/crud.js'
import apiRequest from '../../utils/apiRequest.js'

export default {
  name: 'application',
  components: {
    myTable,
    myDialog
  },
  mixins: [ crud ],
  data () {
    return {
      urls: {
        search: `/application/${this.$route.query.id}/user/search`,
        add: `/application/${this.$route.query.id}/user/add`,
        findApp: `/application/get/${this.$route.query.id}`,
        del: `/application/${this.$route.query.id}/user/delete`,
        setOwner: `/application/${this.$route.query.id}/user/setOwner`
      },
      searchData: {
        keyword: ''
      },
      appOwner: '',
      tableData: {
        rowData: [],
        colData: [
          {
            title: 'erp',
            key: 'code',
            width: '12%'
          },
          {
            title: '中文名',
            key: 'code',
            width: '12%'
          },
          {
            title: '所属部门',
            key: 'orgName',
            width: '25%'
          },
          {
            title: '邮箱',
            key: 'email',
            width: '13%'
          },
          {
            title: '手机号',
            key: 'mobile',
            width: '10%'
          },
          {
            title: '是否是负责人',
            key: 'isOwner',
            width: '10%'
          }
        ],
        btns: [
          {
            txt: '移除',
            method: 'on-del',
            bindKey: 'canDel',
            bindVal: 1
          },
          {
            txt: '设置为负责人',
            method: 'on-owner',
            bindKey: 'canOwner',
            bindVal: 1
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
        user: {
          code: ''
        }
      }
    }
  },
  computed: {
  },
  methods: {
    setOwner (item) {
      let _this = this
      this.$Dialog.confirm({
        title: '提示',
        content: '确定设置为应用负责人吗？'
      }).then(() => {
        this.update(_this.urls.setOwner + '/' + item.id, {}, '设置成功', '设置失败')
      })
    },
    getList () {
      this.showTablePin = true
      let data = this.getSearchVal()
      let _this = this
      apiRequest.post(this.urls.search, {}, data).then((data) => {
        if (data === '') {
          return
        }

        let users = data.data || []

        // 查app
        apiRequest.get(_this.urls.findApp, {}).then((app) => {
          if (app && app.data && app.data.owner) {
            _this.appOwner = app.data.owner.code
          } else {
            _this.appOwner = ''
          }

          console.log(_this.$store.getters.loginUserName)
          users.forEach(user => {
            user['canOwner'] = 0
            user['canDel'] = 0
            user['isOwner'] = ''

            if (_this.$store.getters.isAdmin) { // 管理员
              user['canDel'] = 1
              user['canOwner'] = 1
              if (_this.appOwner && user.code === _this.appOwner) { // owner不能不能重复设置为负责人
                user['canOwner'] = 0
                user['canDel'] = 0
              }
            } else if (_this.appOwner && _this.$store.getters.loginUserName === _this.appOwner) { // 负责人
              if (user.code !== app.data.owner.code) { // owner不能删除自己，不能重复设置为负责人，但是可以删除、设置其他人为联系人
                user['canOwner'] = 1
                user['canDel'] = 1
              }
            } else { // 普通用户
              if (user.code === _this.$store.getters.loginUserName) { // 普通应用用户只能删除自己
                user['canDel'] = 1
              }
            }

            if (_this.appOwner && user.code === _this.appOwner) { // 加是否是负责人列
              user['isOwner'] = '是'
            }
          })

          data.pagination = data.pagination || {
            totalRecord: data.data.length
          }
          _this.page.total = data.pagination.totalRecord
          _this.page.page = data.pagination.page
          _this.page.size = data.pagination.size
          _this.tableData.rowData = users
          _this.showTablePin = false
        })
      })
    }
  },
  // watch: {
  //   '$route' (to, from) {
  //     if (to.query.tab === 'myAppUsers') {
  //       this.getList()
  //     }
  //   }
  // },
  mounted () {
    // this.getList();
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.label{text-align: right; line-height: 32px;}
.val{}
</style>
