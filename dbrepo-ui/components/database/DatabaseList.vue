<template>
  <div>
    <v-card
      variant="flat"
      rounded="0">
      <v-list-item
        v-if="loading"
        lines="two">
        <Loading />
      </v-list-item>
    </v-card>
    <DatabaseCard
      v-for="(database, idx) in databases"
      :database="database"
      :key="idx"/>
  </div>
</template>

<script>
import DatabaseSkeleton from '@/components/database/DatabaseSkeleton.vue'

export default {
  components: {
    DatabaseSkeleton
  },
  props: {
    databases: {
      type: Array,
      default: () => {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: () => {
        return true
      }
    }
  }
}
</script>

