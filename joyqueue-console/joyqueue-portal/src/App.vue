<template>
  <div class="wrapper">
    <main-header></main-header>
    <div class="transition-main main-content">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import mainHeader from './components/common/header.vue'

export default {
  components: {
    mainHeader
  },
  computed: {
    ...mapGetters([
      'loginUsername'
    ])
  }
}
</script>

<style>
  @import "assets/css/reset.css";
  @import "assets/css/common.css";
  @import "assets/css/main.css";
  .wrapper{
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }
</style>
