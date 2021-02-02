import axios from 'axios'
// import qs from 'qs' // 在发送请求之前对请求数据做处理（这个模块在安装axios的时候就已经安装了，不需要另外安装）
import Vue from 'vue'
import loading from '../components/loading'

let VInstance = new Vue()

// download url
const downloadUrl = url => {
  let iframe = document.createElement('iframe')
  iframe.style.display = 'none'
  iframe.src = url
  iframe.onload = function () {
    document.body.removeChild(iframe)
  }
  document.body.appendChild(iframe)
}

// Add a response interceptor
axios.interceptors.response.use(res => {
  // 处理excel文件
  if (res.headers && (res.headers['content-type'] === 'text/plain')) {
    downloadUrl(res.request.responseURL)
  }
  return res
}, err => {
  loading.finish()
  if (err.response && err.response.status === 401 && err.response.headers && err.response.headers.location) {
    const nowUrl = window.location.href
    const returnUrl = err.response.headers.location + '?ReturnUrl=' + nowUrl
    // 跳转登陆
    window.location.href = returnUrl
  } else {
    return Promise.reject(err)
  }
})

axios.interceptors.request.use(config => {
  if (config['loading']) {
    loading.start()
  }
  return config
}, err => {
  loading.finish()
  VInstance.$Message.error(err.toString())
  return Promise.reject(err)
})

axios.interceptors.response.use(response => {
  loading.finish()
  if (response.data.code === 200 || response.data.status === 200) {
    return Promise.resolve(response)
  } else if (response.data.code === 100) {
    return Promise.reject(response.data.message || 'Operation failed. No tips.')
  } else if (response.data.code === 300) {
    return Promise.resolve(response)
  } else {
    let err = response.data.message || 'Operation failed.'
    VInstance.$Message.error(err)
    return Promise.reject(err)
  }
}, err => {
  loading.finish()
  VInstance.$Message.error(err.toString())
  return Promise.reject(err)
})

export default {
  get (url, data) {
    return this.getBase(url, data, false)
  },
  post (url, params, data) {
    return this.postBase(url, params, data, false)
  },
  put (url, params, data) {
    return axios({
      method: 'put',
      url: '/v1' + url,
      params: params,
      data: data,
      loading: true
    }).then(response => {
      return response && response.data
    }).catch(err => {
      return err
    })
  },
  delete (url, params, data) {
    return axios({
      method: 'delete',
      url: '/v1' + url,
      params: params,
      data: data,
      loading: true
    }).then(response => {
      return response && response.data
    }).catch(() => {
      return {}
    })
  },
  getBase (url, data, loading) {
    return axios({
      method: 'get',
      url: '/v1' + url,
      params: data,
      loading: loading
    }).then(response => {
      return response && response.data
    }).catch(() => {
      return {}
    })
  },
  postBase (url, params, data, loading) {
    return axios({
      method: 'post',
      url: '/v1' + url,
      params: params,
      data: data,
      loading: loading
    }).then(response => {
      return response && response.data
    }).catch(() => {
      return {}
    })
  },
  all (...array) {
    return Promise.all(array).then(resList => {
      return resList
    }).catch(() => {
      return {}
    })
  }
}
