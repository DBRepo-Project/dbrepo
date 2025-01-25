<template>
  <div />
</template>

<script setup>
const { loggedIn, user, login, logout } = useOidcAuth()
</script>
<script>
export default {
  mounted () {
    if (!this.loggedIn) {
      return
    }
    this.$router.push('/user/info')
  }
}
</script>
