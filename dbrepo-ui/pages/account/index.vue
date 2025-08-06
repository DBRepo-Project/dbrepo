<template>
  <div />
</template>

<script>
export default {
  mounted () {
    const { loggedIn } = useOidcAuth()
    if (!loggedIn) {
      return
    }
    this.$router.push('/account/info')
  }
}
</script>
