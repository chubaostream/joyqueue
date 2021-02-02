<template>
  <div>
    <grid-row class="table-query">
      <grid-col span="3" style="padding-right: 10px;">
        <d-select v-model="searchData.type" placeholder="请选择类型"
                   @on-change="getList">
          <span slot="prepend">主题类型</span>
          <d-option value="-1" >全部</d-option>
          <d-option value="0" >普通主题</d-option>
          <d-option value="1" >广播主题</d-option>
          <d-option value="2" >顺序主题</d-option>
        </d-select>
      </grid-col>
      <grid-col span="4">
        <d-input v-model="searchData.keyword" placeholder="请输入英文名"
                 oninput="value = value.trim()"
                @on-enter="getList">
          <d-button type="borderless" slot="suffix" @click="getList"><icon name="search" size="14" color="#CACACA" ></icon></d-button>
        </d-input>
      </grid-col>
      <grid-col span="4" offset="13">
        <d-button v-if="$store.getters.isAdmin" type="primary" class="right" @click="openDialog('addDialog')">添加主题<icon name="plus-circle" style="margin-left: 5px;"></icon></d-button>
      </grid-col>
    </grid-row>

    <my-table :data="tableData" :showPin="showTablePin" :page="page" @on-size-change="handleSizeChange"
              @on-current-change="handleCurrentChange" @on-selection-change="handleSelectionChange" @on-view-detail="goDetail"
              @on-add-brokerGroup="addBrokerGroup" @on-del="del" @on-policy="handlePolicy" :operation-column-width="operationColumnWidth">
    </my-table>
    <!--添加-->
    <my-dialog :dialog="addDialog" class="add-dialog maxDialogHeight" @on-dialog-cancel="dialogCancel('addDialog')" :styles="{top: '40px'}">
      <topic-form @on-dialog-cancel="dialogCancel('addDialog')" :type="$store.getters.addFormType"
                  :addBrokerUrls="addBrokerUrls" :addBrokerColData="addBrokerColData"/>
    </my-dialog>
    <!--添加分组-->
    <my-dialog :dialog="addBrokerGroupDialog" @on-dialog-confirm="addBrokerGroupConfirm()" @on-dialog-cancel="addBrokerGroupCancel()">
      <add-broker-group :data="addBrokerGroupData" @on-choosed-brokerGroup="choosedBrokerGroup"></add-broker-group>
    </my-dialog>

    <my-dialog :dialog="policyDialog" @on-dialog-confirm="policyConfirm" @on-dialog-cancel="dialogCancel('policyDialog')">
      <d-form ref="policyMetadata" label-width="200px">
        <d-form-item v-for="item in policies" :key="item.key" :label="item.key + ':'">
          <d-input v-model="item.value" style="width: 250px;"/>
        </d-form-item>
      </d-form>
    </my-dialog>
  </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest.js'
import myTable from '../../components/common/myTable.vue'
import myDialog from '../../components/common/myDialog.vue'
import addBrokerGroup from './addBrokerGroup.vue'
import topicForm from './topicForm.vue'
import crud from '../../mixins/crud.js'
import {basePrimaryBtnRender, sortByTopic} from '../../utils/common.js'

export default {
  name: 'topic',
  components: {
    myTable,
    myDialog,
    topicForm,
    addBrokerGroup
  },
  mixins: [ crud ],
  props: {
    btns: {
      type: Array,
      default () {
        return [
          {
            txt: '策略',
            method: 'on-policy',
            isAdmin: true
          },
          {
            txt: '详情',
            method: 'on-view-detail'
          },
          {
            txt: '删除',
            method: 'on-del',
            isAdmin: 1
          }
        ]
      }
    },
    addBrokerUrls: {
      type: Object,
      default () {
        return {
          search: '/broker/search'
        }
      }
    },
    addBrokerColData: {
      type: Array,
      default () {
        return [
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
        ]
      }
    },
    operationColumnWidth: {
      type: Number,
      default: 140
    }
  },
  data () {
    return {
      searchData: {
        keyword: '',
        command: 1
      },
      topic: {},
      policy: {},
      policies: [],
      tableData: {
        rowData: [],
        colData: [
          {
            title: '英文名',
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
                      name: `/${this.$i18n.locale}/topic/detail`,
                      query: { id: params.item.id, topic: params.item.code, namespace: params.item.namespace.code }})
                  }
                }
              }, params.item.code)
            }
          },
          {
            title: '命名空间',
            width: 200,
            key: 'namespace.code'
          },
          {
            title: '类型',
            key: 'type',
            width: 80,
            render: (h, params) => {
              return basePrimaryBtnRender(h, params.item.type, [
                {
                  value: 0,
                  txt: 'Normal',
                  color: 'success'
                },
                {
                  value: 1,
                  txt: 'Broadcast',
                  color: 'warning'
                },
                {
                  value: 2,
                  txt: 'Sequential',
                  color: 'danger'
                }
              ])
            }
          },
          {
            title: '分区数',
            key: 'partitions'
          }
        ],
        btns: this.btns
      },
      addDialog: {
        visible: false,
        title: '添加主题',
        width: 800,
        showFooter: false
      },
      policyDialog: {
        visible: false,
        title: '策略详情',
        width: '500',
        showFooter: true
      },
      addData: {},
      addBrokerGroupDialog: {
        visible: false,
        title: '添加Broker分组',
        width: 700,
        showFooter: true
      },
      addBrokerGroupData: {}
    }
  },
  computed: {
  },
  methods: {
    goDetail (item) {
      this.$router.push({name: `/${this.$i18n.locale}/topic/detail`,
        query: { id: item.id, topic: item.code, namespace: item.namespace.code }})
    },
    sortData (data) {
      return data.sort((a, b) => sortByTopic(a, b))
    },
    addBrokerGroup (item) {
      this.openDialog('addBrokerGroupDialog')
      this.addBrokerGroupData = item
      this.addBrokerGroupData.brokerGroupsAdd = []
    },
    addBrokerGroupConfirm () {
      let addBrokerGroupData = this.addBrokerGroupData
      apiRequest.post(this.urlOrigin.addBrokerGroup, {}, addBrokerGroupData).then((data) => {
        this.addBrokerGroupDialog.visible = false
        this.$Dialog.success({
          content: '添加成功'
        })
        this.getList()
      })
    },
    policyConfirm () {
      this.topic.policy = {}
      for (let policy in this.policies) {
        if (this.policies.hasOwnProperty(policy)) {
          this.topic.policy[this.policies[policy].key] = this.policies[policy].value
        }
      }
      apiRequest.put(this.urlOrigin.edit + '/' + encodeURIComponent(this.topic.id), {}, this.topic).then((data) => {
        this.policyDialog.visible = false
        if (data.code === 200) {
          this.$Dialog.success({
            content: '更新成功'
          })
        }
        this.getList()
        this.policies = undefined
      })
    },
    addBrokerGroupCancel () {
      this.addBrokerGroupDialog.visible = false
    },
    choosedBrokerGroup (val) {
      this.addBrokerGroupData.brokerGroupsAdd = val
    },
    handlePolicy (item) {
      this.topic = item
      this.policies = []
      if (!this.topic.policy) {
        this.topic.policy = {}
      }
      this.policies.push({
        key: '存储最长时间',
        value: item.policy.storeMaxTime
      })
      this.policies.push({
        key: '保留未消费数据',
        value: item.policy.storeCleanKeepUnconsumed
      })
      for (let policy in this.policy) {
        this.policies.push({
          key: policy,
          value: this.policy[policy]
        })
      }
      this.policyDialog.visible = true
    },
    isAdmin (item) {
      return this.$store.getters.isAdmin
    },
    dialogCancel (dialogName) {
      this[dialogName].visible = false
      this.policies = undefined
      this.getList()
    },
    // 删除
    del (item, index) {
      let _this = this
      this.$Dialog.confirm({
        title: '提示',
        content: '删除时会自动删除与该主题关联的分片、分区组信息，确定要删除吗？'
      }).then(() => {
        apiRequest.post(_this.urlOrigin.del, {}, item.id).then((data) => {
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
.add-dialog{
  overflow: hidden;
}
.show-enter-active,.show-leave-active{
  transition:all 0.5s;
}
.show-enter{
  margin-right: 100px;
}
.show-leave-to{
  margin-left: -100px;
}
.show-enter-to{
  margin-right: 100px;
}
.show-leave{
  margin-left: 100px;
}
.hint{color: #f00;}
.star{color: #f00;}
.maxDialogHeight /deep/ .dui-dialog__body {
  height: 650px;
}
</style>
