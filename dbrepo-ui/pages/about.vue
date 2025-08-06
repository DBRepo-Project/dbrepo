<template>
  <div
    v-if="source">
    <v-card
      rounded="0"
      variant="flat">
      <v-card-text
        class="markdown">
        <v-row>
          <v-col
            cols="8">
            <vue-markdown-it
              :source="source" />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import { VueMarkdownIt } from '@f3ve/vue-markdown-it'

export default {
  components: {
    VueMarkdownIt
  },
  data() {
    return {
      source: this.$config.public.about.content
    }
  }
}
</script>
