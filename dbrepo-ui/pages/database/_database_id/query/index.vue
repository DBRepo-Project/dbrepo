<template>
  <div>
    <DBToolbar v-model="db" />
    <QueryList />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>

export default {
  data () {
    return {
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/query`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    db () {
      return this.$store.state.database
    }
  }
}
</script>
