<template>
  <div />
</template>

<script>
export default {
  mounted () {
    this.$router.push(`/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}/info`)
  }
}
</script>
<style>
</style>
