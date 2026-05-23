<template>
  <view :class="['mp-fab', variantClass]" @click="$emit('tap')">
    <text class="mp-fab-icon">{{ icon }}</text>
    <text v-if="text" class="mp-fab-text">{{ text }}</text>
  </view>
</template>

<script>
export default {
  props: {
    icon: { type: String, default: '' },
    text: { type: String, default: '' },
    variant: { type: String, default: 'primary' }
  },
  emits: ['tap'],
  computed: {
    variantClass() {
      return this.variant === 'success' ? 'mp-fab--success' : 'mp-fab--primary'
    }
  }
}
</script>
