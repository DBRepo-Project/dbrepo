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
    <ContainerCard
      v-for="(container, idx) in containers"
      :container="container"
      :key="idx"/>
  </div>
</template>

<script>
import ContainerCard from '@/components/container/ContainerCard.vue'

export default {
  components: {
    ContainerCard
  },
  props: {
    containers: {
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

