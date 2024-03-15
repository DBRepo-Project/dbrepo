<template>
  <div>
    <pre v-text="description" />
  </div>
</template>

<script>
export default {
  props: {
    blob: {
      type: String,
      default: null
    }
  },
  computed: {
    description () {
      if (!this.blob) {
        return null
      }
      return 'blob'
    }
  }
}
</script>
