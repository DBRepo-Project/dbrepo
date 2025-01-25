<template>
  <div />
</template>

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
