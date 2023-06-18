<template>
  <div />
</template>
<script>
export default {
  mounted () {
    this.$router.push(`/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/info`)
  }
}
</script>
