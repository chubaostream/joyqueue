import apiRequest from './apiRequest.js'
import apiUrl from './apiUrl.js'
import Vue from 'vue'

let VInstance = new Vue()

export function getTopicCode (topic, namespace) {
  let topicCode = (topic || {}).code
  if (topicCode.indexOf('.') >= 0) {
    topicCode = topicCode.split('.')[1]
  }

  if (!namespace || !namespace.code || namespace.code.trim().length === 0) {
    return topicCode
  }

  return (namespace || {}).code + '.' + topicCode
}

export function getTopicCodeByCode (topicCode, namespaceCode) {
  if (topicCode.indexOf('.') >= 0) {
    topicCode = topicCode.split('.')[1]
  }

  if (!namespaceCode || namespaceCode.trim().length === 0) {
    return topicCode
  }

  return namespaceCode + '.' + topicCode
}

export function generateProducerDetailTabName (appCode, topicCode, namespaceCode) {
  return '生产详情-' + appCode + '@' + getTopicCodeByCode(topicCode, namespaceCode)
}

export function generateConsumerDetailTabName (appCode, subscribeGroup, topicCode, namespaceCode) {
  return '消费详情-' + getAppCodeByCode(appCode, subscribeGroup) + '@' + getTopicCodeByCode(topicCode, namespaceCode)
}

export function resolveTopicCode (topicCode) {
  if (topicCode === undefined) {
    return undefined
  } else if (topicCode.indexOf('.') < 0) {
    return topicCode
  } else {
    return topicCode.split('.')[1]
  }
}

export function resolveNamespaceCode (topicCode) {
  if (topicCode === undefined) {
    return undefined
  } else if (topicCode.indexOf('.') < 0) {
    return ''
  } else {
    return topicCode.split('.')[0]
  }
}

export function getAppCode (app, subscribeGroup) {
  if (!subscribeGroup || subscribeGroup.trim().length === 0) {
    return (app || {}).code
  }
  return (app || {}).code + '.' + subscribeGroup
}

export function getAppCodeByCode (appCode, subscribeGroup) {
  if (!subscribeGroup || subscribeGroup.trim().length === 0) {
    return appCode
  }
  return appCode + '.' + subscribeGroup
}

export function resolveAppCode (appCode) {
  if (appCode === undefined) {
    return undefined
  } else if (appCode.indexOf('.') < 0) {
    return appCode
  } else {
    return appCode.split('.')[0]
  }
}

export function getCodeRule () {
  return [
    {required: true, message: '请输入英文名，不超过120个字符', min: 1, max: 120, trigger: 'change'},
    {pattern: /^[a-zA-Z]+[a-zA-Z0-9_-]{1,120}[a-zA-Z0-9]+$/, message: '支持字母、数字、下划线(_)和横线(-)，以英文字母开头', trigger: 'change'}
  ]
}

export function ipValidator () {
  let ipPattern = '(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])'
  return [
    {pattern: '^' + ipPattern + '(,' + ipPattern + ')*$', message: '请输入正确格式的ip', required: false, trigger: 'blur'}
  ]
}

export function getCodeRule2 () {
  return [
    {required: true, message: '请输入英文名，不超过20个字符', min: 1, max: 20, trigger: 'change'},
    {pattern: /^[a-zA-Z][a-zA-Z0-9]{2,20}$/, message: '支持英文字母和数字，以英文字母开头', trigger: 'change'}
  ]
}

export function getCodeRule3 () {
  return [
    {required: true, message: '请输入英文名，不能超过100个字符', min: 1, max: 100, trigger: 'change'},
    {pattern: /^[a-zA-Z0-9][a-zA-Z0-9/._-]{0,99}$/, message: '支持字母、数字、英文句号(.)、下划线(_)和横线(-)', trigger: 'change'}
  ]
}

export function getErpsRule () {
  return [
    {required: true, message: '支持字母、数字、减号(-)、下划线(_)和英文句号(.)，多个ERP用英文逗号(,)分隔，不能有空格', trigger: 'change'},
    {pattern: /^[\w-._]+(,[\w-._]+)*$/, message: '支持字母、数字、减号(-)、下划线(_)和英文句号(.)，多个ERP用英文逗号(,)分隔，不能有空格', trigger: 'change'}
  ]
}

export function getNameRule () {
  return [
    { required: true, message: '请输入中文名，不超过60个字符', min: 1, max: 60, trigger: 'change' },
    {pattern: /^[\u4E00-\u9FA5a-zA-Z]+[\u4E00-\u9FA5a-zA-Z0-9/._-]{1,60}[\u4E00-\u9FA5a-zA-Z0-9]+$/, message: '支持中文、字母、数字、减号(-)、下划线(_)和英文句号(.)，以中英文字符开头', trigger: 'change'}
  ]
}

export function getSubscribeGroupRule () {
  return [
    {required: true, message: '请输入订阅分组，不超过20个字符', min: 1, max: 20, trigger: 'change'},
    {pattern: /^[a-zA-Z]+[a-zA-Z0-9/_-]{1,20}[a-zA-Z0-9]+$/, message: '支持字母、数字、下划线(_)和横线(-)，以英文字母开头', trigger: 'change'}
  ]
}

export function baseBtnRender (h, value, valueTxtColorOptions) {
  if (value === undefined || value === '') {
    return h('label', '')
  }

  let txt = value
  let color = ''
  valueTxtColorOptions.forEach((option) => {
    if (value === option.value) {
      txt = option.txt
      color = option.color
    }
  })

  return h('d-tag', {
    props: {
      size: 'small',
      color: color
    }
  }, txt)
}

export function basePrimaryBtnRender (h, value, valueTxtColorOptions) {
  if (value === undefined || value === '') {
    return h('label', '')
  }

  let txt = value
  let color = ''
  valueTxtColorOptions.forEach((option) => {
    if (value === option.value) {
      txt = option.txt
      color = option.color
    }
  })

  return h('d-tag', {
    props: {
      size: 'small',
      color: color
    }
  }, txt)
}

export function openOrCloseOptions () {
  return [
    {
      value: true,
      txt: '开启',
      color: 'success'
    },
    {
      value: false,
      txt: '关闭',
      color: 'danger'
    }
  ]
}

export function openOrCloseBtnRender (h, value) {
  return baseBtnRender(h, value, openOrCloseOptions())
}

export function yesOrNoOptions () {
  return [
    {
      value: true,
      txt: '是',
      color: 'success'
    },
    {
      value: false,
      txt: '否',
      color: 'danger'
    }
  ]
}

export function yesOrNoBtnRender (h, value) {
  return baseBtnRender(h, value, yesOrNoOptions())
}

/**
 * 客户端类型下拉选项
 */
export function clientTypeOptions () {
  return [
    {
      value: 0,
      txt: 'joyqueue',
      color: 'success'
    },
    {
      value: 1,
      txt: 'kafka',
      color: 'warning'
    }
    // ,{
    //   value: 2,
    //   txt: 'mqtt',
    //   color: 'danger'
    // },
    // {
    //   value: 10,
    //   txt: 'others',
    //   color: 'info'
    // }
  ]
}
export function topicTypeOptions () {
  return [
    {
      value: 0,
      txt: '普通主题',
      color: 'success'
    },
    {
      value: 1,
      txt: '广播主题',
      color: 'warning'
    },
    {
      value: 2,
      txt: '顺序主题',
      color: 'danger'
    }
  ]
}

export function clientTypeSelectRender (h, params, subscribeRef) {
  return h('d-select', {
    props: {
    },
    on: {
      'on-change': (newValue) => {
        params.item['clientType'] = newValue
        subscribeRef.tableData.rowData[params.index] = params.item
      }
    }
  },
  clientTypeOptions().map((item) => {
    return h('d-option', {
      props: {
        value: item.value,
        label: item.txt
      }
    })
  }))
}

export function clientTypeSelectRender2 (h, params, type) {
  return h('d-select', {
    props: {
      value: params.item.clientType
    },
    on: {
      'on-change': (newValue) => {
        params.item['clientType'] = newValue
        let url = ''
        if (type === 'producer') {
          url = apiUrl.common.updateProducer
        } else {
          url = apiUrl.common.updateConsumer
        }
        let id = ''
        if (params.item.id.indexOf('/') !== -1) {
          id = encodeURIComponent(params.item.id)
        } else {
          id = params.item.id
        }
        apiRequest.put(url + '/' + id, {}, params.item).then((data) => {
          if (data.code === 200) {
            VInstance.$Message.info('update success')
          } else {
            VInstance.$Message.error('failed to update clientType')
          }
        })
      },
      input: (event) => {
        params.item.clientType = event.clientType
      }
    }
  }, clientTypeOptions().map((item) => {
    return h('d-option', {
      props: {
        value: item.value,
        label: item.txt
      }
    })
  }))
}

export function clientTypeBtnRender (h, value) {
  return basePrimaryBtnRender(h, value, clientTypeOptions())
}
export function topicTypeBtnRender (h, value) {
  return basePrimaryBtnRender(h, value, topicTypeOptions())
}

export function subscribeGroupAutoCompleteRender (h, params, subscribeRef) {
  function querySearch (queryString, callback) {
    apiRequest.get(apiUrl.common.findSubscribeGroup).then((data) => {
      if (data.data === undefined || data.data === []) {
        let emptyResult = [{'value': ''}]
        callback(emptyResult)
      }
      let subscribeGroups = data.data.map(sg => {
        return {'value': sg}
      })
      let results = queryString ? subscribeGroups.filter(sg => {
        return sg.value.toLowerCase().indexOf(queryString.toLowerCase().trim()) === 0
      }) : subscribeGroups
      // 调用 callback 返回建议列表的数据
      callback(results)
    })
  }

  return h('d-autocomplete', {
    props: {
      value: params.item.subscribeGroup,
      placeholder: '请输入订阅分组',
      fetchSuggestions: querySearch
    },
    on: {
      select: (item) => {
        params.item.subscribeGroup = item.value || ''
        subscribeRef.tableData.rowData[params.index] = params.item
      },
      input: (item) => {
        params.item.subscribeGroup = (item.value || item) || ''
        subscribeRef.tableData.rowData[params.index] = params.item
      }
    }
  })
}

export function subscribeGroupInputRender (h, params, subscribeRef) {
  return h('d-input', {
    props: {
      value: params.item.subscribeGroup,
      placeholder: '请输入订阅分组'
    },
    on: {
      input: (item) => {
        params.item.subscribeGroup = (item.value || item) || ''
        subscribeRef.tableData.rowData[params.index] = params.item
      }
    }
  })
}

export function brokerPermissionTypeRender (h, value) {
  return baseBtnRender(h, value, [
    {
      value: 'FULL',
      txt: '读写',
      color: 'success'
    },
    {
      value: 'READ',
      txt: '只读',
      color: 'warning'
    },
    {
      value: 'NONE',
      txt: '无权限',
      color: 'danger'
    },
    {
      value: 'WRITE',
      txt: '只写',
      color: 'info'
    }
  ])
}

export function brokerRoleTypeRender (h, value) {
  return baseBtnRender(h, value, [
    {
      value: 0,
      txt: 'Dynamics',
      color: 'success'
    },
    {
      value: 1,
      txt: 'Master',
      color: 'warning'
    },
    {
      value: 2,
      txt: 'Slave',
      color: 'warning'
    },
    {
      value: 3,
      txt: 'Leaner',
      color: 'info'
    },
    {
      value: 4,
      txt: 'outsync',
      color: 'danger'
    }
  ])
}

export function brokerSyncModeTypeRender (h, value) {
  return baseBtnRender(h, value, [
    {
      value: 'SYNCHRONOUS',
      txt: '同步',
      color: 'warning'
    },
    {
      value: 'ASYNCHRONOUS',
      txt: '异步',
      color: 'info'
    }
  ])
}

export function brokerRetryTypeRender (h, value) {
  return baseBtnRender(h, value, [
    {
      value: 'RemoteRetry',
      txt: '远程数据库',
      color: 'warning'
    },
    {
      value: 'DB',
      txt: '直连数据库',
      color: 'info'
    }
  ])
}

export function replaceChartUrl (url, namespaceCode, topicCode, appFullName) {
  if (!url || url === '') {
    return undefined
  }
  if (!namespaceCode) {
    namespaceCode = 'default'
  }
  return url.replace(/\[namespace\]/g, namespaceCode).replace(/\[topic\]/g, topicCode).replace(/\[app\]/g, appFullName)
}

export function sortByProducer (p1, p2, desc, topicSortFirst) {
  return sortBase(p1, p2, false, desc, (p1, p2) => {
    let result
    if (topicSortFirst) {
      // compare topic code
      result = sort(p1.topic.code, p2.topic.code, true)
      if (result !== 0) {
        return result
      }
      // compare namespace
      result = sort(p1.namespace.code, p2.namespace.code, true)
      if (result !== 0) {
        return result
      }
      // compare app
      return sort(p1.app.code, p2.app.code, true)
    } else {
      // compare app
      result = sort(p1.app.code, p2.app.code, true)
      if (result !== 0) {
        return result
      }
      // compare topic code
      result = sort(p1.topic.code, p2.topic.code, true)
      if (result !== 0) {
        return result
      }
      // compare namespace
      return sort(p1.namespace.code, p2.namespace.code, true)
    }
  })
}

export function sortByConsumer (c1, c2, desc, topicSortFirst) {
  return sortBase(c1, c2, false, desc, (c1, c2) => {
    let result
    if (topicSortFirst) {
      // compare topic code
      result = sort(c1.topic.code, c2.topic.code, true)
      if (result !== 0) {
        return result
      }
      // compare namespace
      result = sort(c1.namespace.code, c2.namespace.code, true)
      if (result !== 0) {
        return result
      }
      // compare app
      result = sort(c1.app.code, c2.app.code, true)
      if (result !== 0) {
        return result
      }
      // compare subscribeGroup
      return sort(c1.subscribeGroup, c2.subscribeGroup, true)
    } else {
      // compare app
      result = sort(c1.app.code, c2.app.code, true)
      if (result !== 0) {
        return result
      }
      // compare subscribeGroup
      result = sort(c1.subscribeGroup, c2.subscribeGroup, true)
      if (result !== 0) {
        return result
      }
      // compare topic code
      result = sort(c1.topic.code, c2.topic.code, true)
      if (result !== 0) {
        return result
      }
      // compare namespace
      return sort(c1.namespace.code, c2.namespace.code, true)
    }
  })
}

export function sortByTopic (t1, t2, desc) {
  return sortBase(t1, t2, false, desc, (t1, t2) => {
    let result
    // compare topic code
    result = sort(t1.code, t2.code, true)
    if (result !== 0) {
      return result
    }
    // compare namespace
    result = sort(t1.namespace.code, t2.namespace.code, true)
    if (result !== 0) {
      return result
    }
  })
}

export function sortByCode (t1, t2, desc) {
  return sortBase(t1, t2, false, desc, (t1, t2) => {
    // compare code
    return sort(t1.code, t2.code, true)
  })
}

export function sort (a, b, ignoreCase, desc) {
  return sortBase(a, b, ignoreCase, desc, (a, b) => {
    return desc ? ((b > a) ? 1 : (b < a) ? -1 : 0) : ((b > a) ? -1 : (b < a) ? 1 : 0)
  })
}

export function sortBase (a, b, ignoreCase, desc, sameSortFunc) {
  if (a && !b) {
    return desc ? -1 : 1
  }
  if (!a && b) {
    return desc ? 1 : -1
  }
  if (!a && !b) {
    return 0
  }

  if (ignoreCase) {
    a = a.toLowerCase()
    b = b.toLowerCase()
  }

  return sameSortFunc(a, b, desc)
}

export function bytesToSize (bytes, decimals = 2, isTotal = false) {
  if (bytes === 0) {
    if (isTotal) {
      return '0 Bytes'
    } else {
      return '0 Bytes/s'
    }
  }
  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  let flowRate = parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i]
  if (!isTotal) {
    return flowRate + '/s'
  }
  return flowRate
}

export function mergePartitionGroup (group) {
  group.sort((v1, v2) => v1 - v2)
  let allList = []
  let tmpList = []
  if (group.length >= 0) {
    tmpList.push(group[0])
  }
  if (group.length === 1) {
    allList.push(tmpList)
  }
  for (let i = 1; i < group.length; i++) {
    if (group[i] - group[i - 1] !== 1) {
      if (tmpList.indexOf(group[i - 1]) === -1) {
        tmpList.push(group[i - 1])
      }
      let list = []
      Object.assign(list, tmpList)
      allList.push(list)
      tmpList = []
      tmpList.push(group[i])
    }
    if (i === group.length - 1) {
      if (tmpList.indexOf(group[i]) === -1) {
        tmpList.push(group[i])
      }
      allList.push(tmpList)
    }
  }
  let groupStr = ''
  for (let i in allList) {
    if (groupStr !== '') {
      groupStr += ','
    }
    if (allList.hasOwnProperty(i)) {
      if (allList[i].length > 1) {
        groupStr += '[' + allList[i][0] + '-' + allList[i][allList[i].length - 1] + ']'
      } else if (allList[i].length === 1) {
        groupStr += '[' + allList[i][0] + ']'
      }
    }
  }
  return groupStr
}
