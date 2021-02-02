<template>
  <div>
    <grid-row class="table-query">
      <grid-col span="4">
      <d-input v-model="searchData.keyword" oninput="value = value.trim()" :placeholder="langConfig.placeholder1" class="left mr10" style="width: 100%"
        @on-enter="getList">
        <d-button type="borderless" slot="suffix" @click="getList"><icon name="search" size="14" color="#CACACA"></icon></d-button>
      </d-input>
      </grid-col>
      <grid-col span="4" offset="16">
        <d-button v-if="$store.getters.isAdmin" type="primary" class="right" @click="openDialog('addDialog')">新建应用<icon name="plus-circle" style="margin-left: 5px;"></icon></d-button>
      </grid-col>
    </grid-row>
    <my-table :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange" @on-current-change="handleCurrentChange"
              @on-selection-change="handleSelectionChange" @on-del="del" @on-view-detail="goDetail" operation-column-width="140">
    </my-table>

    <!--新建弹出框-->
    <my-dialog :dialog="addDialog" @on-dialog-confirm="addConfirm()" @on-dialog-cancel="addCancel('addDialog')"
               :styles="{top: '200px'}">
      <app-add-form ref="addForm" :type="$store.getters.addFormType" />
    </my-dialog>
  </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest.js'
import myTable from '../../components/common/myTable.vue'
import myDialog from '../../components/common/myDialog.vue'
import appAddForm from './appAddForm.vue'
import crud from '../../mixins/crud.js'
import viewLang from '../../i18n/views.json'
import {sortByCode} from '../../utils/common.js'

export default {
  name: 'application',
  components: {
    myTable,
    myDialog,
    appAddForm
  },
  mixins: [ crud ],
  data () {
    const curLangConfig = viewLang[this.$i18n.locale]['application']
    return {
      searchData: {
        keyword: ''
      },
      tableData: {
        rowData: [],
        colData: [
          {
            title: curLangConfig['colData']['code'],
            key: 'code',
            width: 200,
            render: (h, params) => {
              return h('label', {
                style: {
                  cursor: 'pointer',
                  color: '#3366FF'
                },
                on: {
                  click: () => {
                    this.$router.push({
                      name: `/${this.curLang}/application/detail`,
                      query: {id: params.item.id, app: params.item.code}
                    })
                  }
                }
              }, params.item.code)
            }
          },
          {
            title: curLangConfig['colData']['description'],
            key: 'description'
          }
        ],
        btns: [
          {
            txt: curLangConfig['colData']['btnDetail'],
            method: 'on-view-detail'
          },
          {
            txt: curLangConfig['colData']['btnDelete'],
            method: 'on-del'
          }
        ]
      },
      addDialog: {
        visible: false,
        title: '新建应用',
        showFooter: true
      }
    }
  },
  computed: {
    addUrl () {
      return this.urlOrigin.add
    },
    curLang () {
      return this.$i18n.locale
    },
    langConfig () {
      return viewLang[this.curLang]['application']
    }
  },
  watch: {
    curLang () {
      this.tableData = {
        rowData: [],
        colData: [
          {
            title: this.langConfig['colData']['code'],
            key: 'code',
            render: (h, params) => {
              return h('label', {
                style: {
                  cursor: 'pointer',
                  color: '#3366FF'
                },
                on: {
                  click: () => {
                    this.$router.push({
                      name: `/${this.curLang}/application/detail`,
                      query: { id: params.item.id, app: params.item.code }
                    })
                  }
                }
              }, params.item.code)
            }
          },
          {
            title: this.curLangConfig['colData']['description'],
            key: 'description'
          }
        ],
        btns: [
          {
            txt: this.langConfig['colData']['btnDetail'],
            method: 'on-view-detail'
          },
          {
            txt: this.langConfig['colData']['btnDelete'],
            method: 'on-del',
            isAdmin: 1
          }
        ]
      }
    }
  },
  methods: {
    // 删除
    del (item, index) {
      let _this = this
      this.$Dialog.confirm({
        title: '提示',
        content: '删除时会自动将关联的用户和令牌删除，确定要删除吗？'
      }).then(() => {
        apiRequest.delete(_this.urlOrigin.del + '/' + item.id).then((data) => {
          if (data.code !== this.$store.getters.successCode) {
            this.$Dialog.error({
              content: '删除失败'
            })
          } else {
            this.$Message.success('删除成功')
            if (typeof (_this.afterDel) === 'function') {
              _this.afterDel(item)
            }
            _this.getList()
          }
        })
      }).catch(() => {
      })
    },
    goDetail (item) {
      this.$router.push({
        name: `/${this.curLang}/application/detail`,
        query: {id: item.id, app: item.code}})
    },
    sortData (data) {
      return data.sort((a, b) => sortByCode(a, b))
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
.table-query {
  width: 99%;
  margin-top: 20px;
  padding-right: 20px;
  }
</style>
