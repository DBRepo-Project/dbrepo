<template>
  <div>
    <pre>{{ description }}</pre>
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
