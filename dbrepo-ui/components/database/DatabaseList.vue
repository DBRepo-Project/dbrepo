<template>
  <div>
    <div
      v-if="loading"
      v-for="(idx) in [1,2,3,4,5]"
      :key="`d-${idx}`">
      <DatabaseSkeleton />
    </div>
    <div v-for="(database, idx) in databases" :key="idx">
      <DatabaseCard
        :database="database" />
    </div>
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

