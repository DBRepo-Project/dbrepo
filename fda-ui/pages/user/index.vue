<template>
  <div />
</template>

<script>
export default {
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
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
<style>
</style>
