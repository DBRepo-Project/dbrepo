<template>
  <div />
</template>

<script>
export default {
  mounted () {
    this.$router.push(`/database/${this.$route.params.database_id}/subset/${this.$route.params.subset_id}/info`)
  }
}
</script>
