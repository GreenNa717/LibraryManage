<template>
  <view :class="['mp-tag', tagClass]"><slot /></view>
</template>

<script>
export default {
  props: {
    status: { type: Number, required: true }
  },
  computed: {
    tagClass() {
      const map = { 0: 'mp-tag--borrowing', 1: 'mp-tag--returned', 2: 'mp-tag--overdue' }
      return map[this.status] || ''
    }
  }
}
</script>
