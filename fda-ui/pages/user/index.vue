<template>
  <div />
</template>

<script>
export default {
  computed: {
    token () {
      return this.$store.state.token
    }
  },
  mounted () {
    if (!this.token) {
      return
    }
    this.$router.push('/user/info')
  }
}
</script>
<style>
</style>
