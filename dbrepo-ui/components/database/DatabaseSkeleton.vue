<template>
  <v-card
    variant="flat"
    rounded="0">
    <v-divider class="mx-4" />
    <v-skeleton-loader
      type="subtitle"
      :style="randomWidth(300,500)" />
    <v-skeleton-loader
      type="list-item-two-line"
      :style="randomWidth(500,800)" />
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
