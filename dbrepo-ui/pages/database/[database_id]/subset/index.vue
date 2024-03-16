<template>
  <div>
    <DatabaseToolbar />
    <SubsetList />
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import SubsetList from '@/components/subset/SubsetList'

export default {
  components: {
    SubsetList
  },
  data () {
    return {
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
         title: this.$t('navigation.subsets'),
          to: `/database/${this.$route.params.database_id}/query`,
          disabled: true
        }
      ]
    }
  }
}
</script>
