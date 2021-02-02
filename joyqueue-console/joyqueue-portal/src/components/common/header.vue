<template>
  <div>
    <d-menu mode="horizontal" :theme="theme1" :active-name="activeName" class="left">
    <span class="logo">
      <img src="../../assets/images/joyqueue-logo.png"/>
    </span>
    <div class="left">
      <d-menu-item name="application" icon="home" color="#D98880" :to="`/${ curLang }/application`">
        {{ langConfig.application }}
      </d-menu-item>
      <d-menu-item name="topic" icon="grid" color="#58D68D" :to="`/${ curLang }/topic`">
        {{ langConfig.topic }}
      </d-menu-item>
      <d-menu-item name="tool" icon="search" color="#F8C471" :to="`/${ curLang }/tool`">
        {{ langConfig.tool }}
      </d-menu-item>
      <d-menu-item name="setting" icon="settings" color="#BB8FCE" :to="`/${ curLang }/setting`" v-if="$store.getters.isAdmin">
        {{ langConfig.setting }}
      </d-menu-item>
    </div>
    <div class="right mr40">
      <!-- 语言选择 -->
      <d-dropdown class="lang-select" @on-command="switchLang">
        <a class="d-dropdown-link">
          {{displayedLang}}
          <icon name="chevron-down" slot="suffix"></icon>
        </a>
        <d-dropdown-menu slot="list">
          <d-dropdown-item v-for="(value, key) in langs"
                           :key="key"
                           :command="key">{{ value }}</d-dropdown-item>
        </d-dropdown-menu>
      </d-dropdown>
      <d-submenu name="6" class="right">
        <template slot="title">
          <icon name="user"/>
          {{loginUserName}}
        </template>
        <d-menu-item v-if="!$store.getters.loginUserName" name="6-1" @click.native="login()">
          <icon name="log-in" />
          {{ langConfig.login }}
        </d-menu-item>
        <d-menu-item v-else name="6-1" @click.native="logout()">
          <icon name="log-out" />
          {{ langConfig.logout }}
        </d-menu-item>
      </d-submenu>
    </div>
  </d-menu>
  </div>
</template>
<script>
import { mapGetters } from 'vuex'
import langs from '../../i18n/langs.json'
import compoLang from '../../i18n/components.json'
import cookie from '../../utils/cookie'
import apiRequest from '../../utils/apiRequest'
import apiUrl from '../../utils/apiUrl'

export default {
  data () {
    return {
      theme1: 'dark',
      activeName: '',
      isLogin: false,
      langs: langs
    }
  },
  computed: {
    ...mapGetters(['loginUserName', 'loginUserRole']),
    curLang () {
      return this.$i18n.locale
    },
    displayedLang () {
      return this.langs[this.curLang] || '中文'
    },
    langConfig () {
      return compoLang[this.curLang]['header']
    }
  },
  watch: {
    $route (to, from) {
      const pathArray = to.path.split('/')
      const pathLength = pathArray.length
      this.activeName = pathLength > 2 ? pathArray[2] : pathArray[pathLength - 1]
    }
  },
  methods: {
    switchLang (targetLang) {
      if (this.curLang === targetLang) return
      const oldLang = this.curLang
      this.$i18n.locale = targetLang
      window.localStorage.setItem('joyqueue-language', targetLang)
      this.$router.push(this.$route.path.replace(oldLang, targetLang))
    },
    logout () {
      cookie.del('sso.jd.com', '/', '.jd.com')
      apiRequest.get(apiUrl.logout).then(data => {
        this.$store.dispatch('clearUserInfo').then(data => {
          this.login()
          sessionStorage.removeItem('username')
          sessionStorage.removeItem('role')
        })
      })
    },
    login () {
      let url = `/${this.$i18n.locale}/login`
      if (this.$route.path.endsWith(url)) {
        this.$router.go(0)
      } else {
        this.$router.push({
          path: `/${this.$i18n.locale}/login`
        })
      }
    }
  },
  created () {
  }
}
</script>
<style scoped>
  .logo{
    display: inline-block;
    line-height:normal;
    float:left;
  }
  .logo img{
    width:130px;
    padding-top:16px;
    padding-left: 15px;
  }
  .dui-menu.dui-menu-dark.dui-menu-horizontal .left > .dui-menu-item.dui-menu-item-active {
    color: #67A2F9;
  }
  .lang-select {
    float: left;
    font-size: 14px;
    color: #dfdfdf;
  }
  .lang-select .dui-dropdown-menu {
    background: #2c405a;
  }
  .lang-select .dui-dropdown-menu-item:hover {
    color: #67A2F9;
    background: none;
  }
</style>
