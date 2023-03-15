<template>
  <div />
</template>

<script>
export default {
  mounted () {
    this.$router.push(`/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`)
  }
}
</script>
<style>
</style>
