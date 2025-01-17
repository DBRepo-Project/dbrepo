<template>
  <div />
</template>

<script>
import { useUserStore } from '@/stores/user.js'

export default {
  data () {
    return {
      userStore: useUserStore()
    }
  },
  computed: {
    token () {
      return this.userStore.getToken
    },
    user () {
      return this.userStore.getUser
    }
  },
  mounted () {
    if (!this.user) {
      return
    }
    this.$router.push('/user/info')
  }
}
</script>
