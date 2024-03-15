<template>
  <v-card variant="flat">
    <v-divider class="mx-4" />
    <v-card-subtitle class="db-subtitle">
      <v-skeleton-loader type="text" :style="randomWidth(100,300)" />
      <v-skeleton-loader type="text" class="pt-2" :style="randomWidth(100,200)" />
    </v-card-subtitle>
    <v-card-text class="db-description">
      <v-skeleton-loader type="chip" />
      <v-skeleton-loader type="text" class="pt-4" :style="randomWidth(800,1000)" />
      <v-skeleton-loader type="text" :style="randomWidth(800,1000)" />
      <v-skeleton-loader type="text" :style="randomWidth(600,1000)" />
    </v-card-text>
  </v-card>
</template>

<script>
export default {
  methods: {
    randomWidth (min, max) {
      const width = Math.random() * (max - min) + min
      return `width: ${width}px !important;`
    }
  }
}
</script>
