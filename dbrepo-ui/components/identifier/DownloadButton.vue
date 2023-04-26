<template>
  <v-btn
    :loading="loading"
    v-bind="$attrs"
    @click.stop="download">
    <slot />
  </v-btn>
</template>

<script>

export default {
  props: {
    pid: {
      type: Number,
      default () {
        return null
      }
    },
    contentType: {
      type: String,
      default () {
        return 'text/xml'
      }
    },
    filename: {
      type: String,
      default () {
        return 'identifier.xml'
      }
    }
  },
  data () {
    return {
      loading: false
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {
          headers: { Accept: 'application/json' }
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}`, Accept: 'application/json' }
      }
    }
  },
  methods: {
    async download () {
      this.loading = true
      try {
        const config = this.config
        config.headers.Accept = this.contentType
        const res = await this.$axios.get(`/api/pid/${this.pid}`, config)
        console.debug('export identifier', res)
        const url = window.URL.createObjectURL(new Blob([res.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', this.filename)
        document.body.appendChild(link)
        link.click()
      } catch (err) {
        console.error('Could not export identifier', err)
        this.$toast.error('Could not export identifier')
        this.error = true
      }
      this.loading = false
    }
  }
}
</script>
<style scoped>
</style>
