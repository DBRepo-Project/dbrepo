<template>
  <div>
    <v-toolbar
      flat
      :title="$t('pages.container.title')">
    </v-toolbar>
    <ContainerList
      v-cloak
      :loading="loading"
      :containers="containers" />
  </div>
</template>

<script>
import ContainerList from '@/components/container/ContainerList.vue'

export default {
  components: {
    ContainerList
  },
  data () {
    return {
      loading: true,
      dialog: null,
      containers: []
    }
  },
  mounted () {
    this.fetchContainers()
  },
  methods: {
    fetchContainers () {
      this.loading = true
      const containerService = useContainerService();
      containerService.findAll()
        .then((containers) => {
          this.containers = containers
          this.loading = false
        })
    }
  }
}
</script>
