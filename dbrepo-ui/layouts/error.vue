<template>
  <div>
    <div class="text-center">
      <p class="text-h5" v-text="title" />
      <p v-if="error.statusCode === 404" class="text-body-1">
        <v-btn color="primary" to="/">
          Start Page
        </v-btn>
      </p>
    </div>
    <div>
      <pre>{{ error }}</pre>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'empty',
  props: {
    error: {
      type: Object,
      default: null
    }
  },
  data () {
    return {}
  },
  computed: {
    title () {
      return this.error.message
    }
  }
}
</script>

<style scoped>
h1 {
  font-size: 20px;
}
</style>
