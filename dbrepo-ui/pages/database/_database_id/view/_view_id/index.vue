<template>
  <div />
</template>

<script>
export default {
  mounted () {
    this.$router.push(`/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/info`)
  }
}
</script>
<style>
</style>
