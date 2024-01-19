<template>
  <div>
    <DatabaseToolbar v-model="db" />
    <SubsetList />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import SubsetList from '@/components/query/SubsetList.vue'
export default {
  components: {
    SubsetList
  },
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
