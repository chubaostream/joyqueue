<template>
    <div>
      <my-dialog :dialog="loginDialog">
        <grid-row>
          <grid-col offset="1">
            <d-form ref="form" :model="formData" :rules="rules" label-width="100px">
              <d-form-item label="用户名" placeholser="请输入用户名" prop="username" style="width: 85%">
                <d-input v-model="formData.username" oninput="value = value.trim()"></d-input>
              </d-form-item>
              <d-form-item label="密码:" placeholser="请输入密码" prop="password" style="width: 85%">
                <d-input type="password" v-model="formData.password" oninput="value = value.trim()"></d-input>
              </d-form-item>
            </d-form>
          </grid-col>
        </grid-row>
        <grid-row>
          <grid-col offset="18">
            <d-button type="primary" style="margin-top: 20px;" @click="login">登录</d-button>
          </grid-col>
        </grid-row>
      </my-dialog>
    </div>
</template>

<script>
import apiRequest from '../../utils/apiRequest'
import MyDialog from '../../components/common/myDialog'
import GridRow from '../../components/grid/row'
import GridCol from '../../components/grid/col'

export default {
  name: 'login',
  components: {GridCol, GridRow, MyDialog},
  data () {
    return {
      urls: {
        loginUrl: '/user/login'
      },
      formData: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          {
            required: true, message: '用户名不可以为空,长度范围为[3,20]', min: 3, max: 20, trigger: 'blur'
          }
        ],
        password: [
          {
            required: true, message: '密码不可以为空,长度范围为[4,20]', min: 4, max: 20, trigger: 'blur'
          }
        ]
      },
      loginDialog: {
        visible: true,
        width: 600,
        title: '登录',
        showFooter: false
      }
    }
  },
  methods: {
    login () {
      this.$refs.form.validate((valid) => {
        if (valid) {
          let data = this.formData
          apiRequest.post(this.urls.loginUrl, {}, data).then(result => {
            console.log(result)
            if (result.code === 200) {
              // 生成cookie并保存
              window.document.cookie = 'login.session=' + data.username + ':' + data.password
              this.$store.dispatch('getUserInfo').then(result => {
                sessionStorage.setItem('username', result.loginUserName)
                sessionStorage.setItem('role', result.loginUserRole)
                this.$router.push({
                  path: `/`
                })
              })
            }
          })
        }
      })
    }
  },
  mounted () {
  }
}
</script>

<style scoped>

</style>
