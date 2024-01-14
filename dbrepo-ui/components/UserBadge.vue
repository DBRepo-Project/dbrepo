<template>
  <p class="mb-0">
    <OrcidIcon v-if="hasOrcid" :orcid="orcid" />
    <span v-if="isSelf">
      <v-badge inline content="you" color="code">{{ creatorName }}</v-badge>
    </span>
    <span v-else v-text="creatorName" />
  </p>
</template>
<script>
import OrcidIcon from '@/components/icons/OrcidIcon.vue'
import UserMapper from '@/api/user.mapper'

export default {
  components: {
    OrcidIcon
  },
  props: {
    user: {
      type: Object,
      default () {
        return {
          id: null,
          attributes: {
            orcid: null
          }
        }
      }
    },
    otherUser: {
      type: Object,
      default () {
        return {
          id: null
        }
      }
    }
  },
  computed: {
    hasOrcid () {
      if (!this.user || !this.user.attributes || !this.user.attributes.orcid) {
        return false
      }
      return true
    },
    orcid () {
      if (!this.hasOrcid) {
        return false
      }
      return this.user.attributes.orcid
    },
    creatorName () {
      return UserMapper.userToFullName(this.user)
    },
    isSelf () {
      if (!this.otherUser) {
        return false
      }
      return this.user.id === this.otherUser.id
    }
  }
}
</script>
