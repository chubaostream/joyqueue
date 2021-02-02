<template>
  <layout-container>
    <layout-sider class="layout-sider" width="180px">
      <d-menu :active-name="activeName" :theme="theme" width="auto">
        <div class="layout-logo-left"></div>
        <d-menu-item name="dataCenter" icon="radio" :size="iconSize" :to="`/${ curLang }/setting/dataCenter`"><!--color="#FFD306"-->
          <span class="layout-text">{{ langConfig.dataCenter }}</span>
        </d-menu-item>
        <d-menu-item name="dataCenter" icon="radio" :size="iconSize" :to="`/${ curLang }/setting/user`"><!--color="#FFD306"-->
          <span class="layout-text">{{ langConfig.user }}</span>
        </d-menu-item>
        <d-menu-item name="config" icon="edit" :size="iconSize" :to="`/${ curLang }/setting/config`"><!--color="#7FB3D5"-->
          <span class="layout-text">{{ langConfig.config }}</span>
        </d-menu-item>
        <d-menu-item name="brokerGroup" icon="layers" :size="iconSize" :to="`/${ curLang }/setting/brokerGroup`"><!--color="#58D68D"-->
          <span class="layout-text">{{ langConfig.brokerGroup }}</span>
        </d-menu-item>
        <d-menu-item name="broker" icon="cpu" :size="iconSize" :to="`/${ curLang }/setting/broker`"><!--color="#F5B7B1"-->
          <span class="layout-text">{{ langConfig.broker }}</span>
        </d-menu-item>
        <d-menu-item name="namespace" icon="feather" :size="iconSize" :to="`/${ curLang }/setting/namespace`"><!--color="#73C6B6"-->
          <span class="layout-text">{{ langConfig.namespace }}</span>
        </d-menu-item>
        <d-menu-item name="retryMonitor" icon="compass" :size="iconSize" :to="`/${ curLang }/setting/retryMonitor`"><!--color="#73C6B6"-->
          <span class="layout-text">{{ langConfig.retryMonitor }}</span>
        </d-menu-item>
      </d-menu>
    </layout-sider>
    <layout-content>
      <router-view></router-view>
    </layout-content>
  </layout-container>

</template>

<script>
// import compoLang from '../../i18n/components.json';
import submenu from '../../mixins/submenu.js'

export default {
  components: {},
  mixins: [ submenu ],
  data () {
    return {
      menu: 'setting'
    }
  },
  mounted () {
    // this.submenu = compoLang[this.curLang][this.menu]['user'];
    // this.activeName = 'user';
  },
  computed: {
    curLang () {
      return this.$i18n.locale
    }
  }
}

</script>

<style scoped>
  .layout{
    border: 1px solid #d7dde4;
    background: #f5f7f9;
    position: relative;
  }
  .layout-logo-left{
    width: 90%;
    height: 0px;
    line-height: 20px;
    color: #ffffff;
    background: #ffffff;
    border-radius: 3px;
    /*margin: 15px auto;*/
    text-align: center;
  }
  .toggle-btn{
    margin-left: 15px;
  }
  .layout-sider {
    min-height: calc(100vh - 70px);
    background: #fff;
    /*padding-bottom: 999em;*/
    /*margin-bottom: -999em;*/
  }
  .layout-header {
    padding-top: 10px;
    padding-left: 20px;
    box-sizing: border-box;
    flex-shrink: 0;
    /*background: #ffffff;*/
  }
  .layout-breadcrumb{
    padding: 10px 15px 0;
    display: inline-block;
  }
  .layout-content{
    padding-top: 10px;
    padding-bottom: 15px;
    overflow: hidden;
    /*background: #ffffff;*/
    border-radius: 4px;
    min-height: calc(100vh - 70px);
  }
  .layout-hide-text .layout-text {
    display: none;
  }
  .dui-menu-dark.dui-menu-vertical .dui-menu-item-active:not(.dui-menu-submenu) {
    background: rgb(84, 92, 100);
  }
</style>
