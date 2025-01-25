<template>
  <div>
    <v-row>
      <v-col>
        <v-card
          elevation="0"
          variant="flat"
          class="mx-auto"
          max-width="600"
          :title="title"
          :subtitle="subtitle"
          :text="text">
          <template v-slot:prepend>
            <v-icon
              :color="iconColor">
              {{ icon }}
            </v-icon>
          </template>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script>
export default {
  props: {
    title: {
      type: String,
      default: () => {
        return 'Title'
      }
    },
    subtitle: {
      type: String,
      default: () => {
        return 'Subtitle'
      }
    },
    text: {
      type: String,
      default: () => {
        return 'Text'
      }
    },
    icon: {
      type: String,
      default: () => {
        return 'mdi-alert-rhombus-outline'
      }
    },
    iconColor: {
      type: String,
      default: () => {
        return 'warning'
      }
    }
  }
}
</script>
