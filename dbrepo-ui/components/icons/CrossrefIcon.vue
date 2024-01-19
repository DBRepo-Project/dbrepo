<template>
  <a :href="link" target="_blank">
    <svg
      id="Layer_1"
      version="1.1"
      xmlns="http://www.w3.org/2000/svg"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      x="0px"
      y="0px"
      width="200"
      viewBox="0 0 200 130.2"
      xml:space="preserve">
      <style type="text/css">
        .st0{fill:#3EB1C8;}
        .st1{fill:#D8D2C4;}
        .st2{fill:#4F5858;}
        .st3{fill:#FFC72C;}
        .st4{fill:#EF3340;}
      </style>
      <g>
        <polygon class="st0" points="111.8,95.5 111.8,66.8 135.4,59 177.2,73.3" />
        <polygon class="st1" points="153.6,36.8 111.8,51.2 135.4,59 177.2,44.6" />
        <polygon class="st2" points="135.4,59 177.2,44.6 177.2,73.3" />
        <polygon class="st3" points="177.2,0.3 177.2,29 153.6,36.8 111.8,22.5" />
        <polygon class="st4" points="153.6,36.8 111.8,51.2 111.8,22.5" />
      </g>
    </svg>
  </a>
</template>

<script>
export default {
  props: {
    crossrefFunderId: {
      type: String,
      default () {
        return null
      }
    }
  },
  computed: {
    link () {
      if (!this.crossrefFunderId) {
        return null
      }
      return this.crossrefFunderId
    }
  }
}
</script>
