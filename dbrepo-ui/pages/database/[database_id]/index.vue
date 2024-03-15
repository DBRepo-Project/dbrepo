<template>
  <div />
</template>

<script>
export default {
  mounted () {
    this.$router.push(`/database/${this.$route.params.database_id}/info`)
  }
}
</script>
